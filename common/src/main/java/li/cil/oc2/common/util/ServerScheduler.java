/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.util;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;

public final class ServerScheduler {
    private static final TickScheduler globalTickScheduler = new TickScheduler();
    private static final WeakHashMap<LevelAccessor, TickScheduler> levelTickSchedulers = new WeakHashMap<>();
    private static final WeakHashMap<LevelAccessor, SimpleScheduler> levelUnloadSchedulers = new WeakHashMap<>();
    private static final WeakHashMap<LevelAccessor, HashMap<ChunkPos, ListenerCollection>> chunkLoadSchedulers = new WeakHashMap<>();
    private static final WeakHashMap<LevelAccessor, HashMap<ChunkPos, ListenerCollection>> chunkUnloadSchedulers = new WeakHashMap<>();
    private static final WeakHashMap<LevelAccessor, ChunkListenerCollection> anyChunkUnloadObservers = new WeakHashMap<>();

    // --------------------------------------------------------------------- //

    public static void schedule(final Runnable runnable) {
        schedule(runnable, 0);
    }

    public static void schedule(final Runnable runnable, final int afterTicks) {
        validateServerThread();
        globalTickScheduler.schedule(runnable, afterTicks);
    }

    public static void schedule(final LevelAccessor level, final Runnable runnable) {
        schedule(level, runnable, 0);
    }

    public static void schedule(final LevelAccessor level, final Runnable runnable, final int afterTicks) {
        runOnServerThread(level, () -> {
            final TickScheduler scheduler = levelTickSchedulers.computeIfAbsent(level, w -> new TickScheduler());
            scheduler.schedule(runnable, afterTicks);
        });
    }

    public static void scheduleOnUnload(final LevelAccessor level, final Runnable listener) {
        runOnServerThread(level, () -> levelUnloadSchedulers
            .computeIfAbsent(level, unused -> new SimpleScheduler())
            .add(listener));
    }

    public static void cancelOnUnload(@Nullable final LevelAccessor level, final Runnable listener) {
        if (level == null) {
            return;
        }

        runOnServerThread(level, () -> {
            final SimpleScheduler scheduler = levelUnloadSchedulers.get(level);
            if (scheduler != null) {
                scheduler.remove(listener);
            }
        });
    }

    public static void subscribeOnLoad(final LevelAccessor level, final ChunkPos chunkPos, final Runnable listener) {
        runOnServerThread(level, () -> chunkLoadSchedulers
            .computeIfAbsent(level, unused -> new HashMap<>())
            .computeIfAbsent(chunkPos, unused -> new ListenerCollection())
            .add(listener));
    }

    public static void unsubscribeOnLoad(@Nullable final LevelAccessor level, final ChunkPos chunkPos, final Runnable listener) {
        unsubscribe(level, chunkPos, listener, chunkLoadSchedulers);
    }

    public static void subscribeOnUnload(final LevelAccessor level, final ChunkPos chunkPos, final Runnable listener) {
        runOnServerThread(level, () -> chunkUnloadSchedulers
            .computeIfAbsent(level, unused -> new HashMap<>())
            .computeIfAbsent(chunkPos, unused -> new ListenerCollection())
            .add(listener));
    }

    public static void unsubscribeOnUnload(@Nullable final LevelAccessor level, final ChunkPos chunkPos, final Runnable listener) {
        unsubscribe(level, chunkPos, listener, chunkUnloadSchedulers);
    }

    public static void subscribeOnAnyChunkUnload(final LevelAccessor level, final Consumer<ChunkPos> listener) {
        runOnServerThread(level, () -> anyChunkUnloadObservers
            .computeIfAbsent(level, unused -> new ChunkListenerCollection())
            .add(listener));
    }

    public static void unsubscribeOnAnyChunkUnload(@Nullable final LevelAccessor level, final Consumer<ChunkPos> listener) {
        if (level == null) {
            return;
        }

        runOnServerThread(level, () -> {
            final ChunkListenerCollection listeners = anyChunkUnloadObservers.get(level);
            if (listeners != null) {
                listeners.remove(listener);
            }
        });
    }

    // --------------------------------------------------------------------- //

    public static void initialize() {
        LifecycleEvent.SERVER_STOPPED.register(server -> {
            globalTickScheduler.clear();
            levelTickSchedulers.clear();
            levelUnloadSchedulers.clear();
            chunkLoadSchedulers.clear();
            chunkUnloadSchedulers.clear();
            anyChunkUnloadObservers.clear();
        });

        LifecycleEvent.SERVER_LEVEL_UNLOAD.register(level -> {
            levelTickSchedulers.remove(level);
            chunkLoadSchedulers.remove(level);
            chunkUnloadSchedulers.remove(level);
            anyChunkUnloadObservers.remove(level);

            final SimpleScheduler scheduler = levelUnloadSchedulers.remove(level);
            if (scheduler != null) {
                scheduler.run();
            }
        });

        TickEvent.SERVER_PRE.register(server -> {
            globalTickScheduler.tick();

            for (final TickScheduler scheduler : levelTickSchedulers.values()) {
                scheduler.tick();
            }
        });

        TickEvent.SERVER_LEVEL_PRE.register(level -> {
            globalTickScheduler.processQueue();

            final TickScheduler scheduler = levelTickSchedulers.get(level);
            if (scheduler != null) {
                scheduler.processQueue();
            }
        });
    }

    private static void unsubscribe(@Nullable final LevelAccessor level, final ChunkPos chunkPos, final Runnable listener, final WeakHashMap<LevelAccessor, HashMap<ChunkPos, ListenerCollection>> schedulers) {
        if (level == null) {
            return;
        }

        runOnServerThread(level, () -> {
            final HashMap<ChunkPos, ListenerCollection> chunkMap = schedulers.get(level);
            if (chunkMap == null) {
                return;
            }

            final ListenerCollection listeners = chunkMap.get(chunkPos);
            if (listeners != null) {
                listeners.remove(listener);
                if (listeners.isEmpty()) {
                    chunkMap.remove(chunkPos);
                }
            }
        });
    }

    public static void onChunkLoad(final LevelAccessor level, final ChunkPos chunkPos) {
        validateServerThread();
        runChunkListeners(chunkLoadSchedulers, level, chunkPos);
    }

    public static void onChunkUnload(final LevelAccessor level, final ChunkPos chunkPos) {
        validateServerThread();
        runChunkListeners(chunkUnloadSchedulers, level, chunkPos);

        final ChunkListenerCollection observers = anyChunkUnloadObservers.get(level);
        if (observers != null) {
            observers.run(chunkPos);
        }
    }

    private static void validateServerThread() {
        if (!ServerUtils.isOnServerThread()) {
            throw new IllegalStateException("ServerScheduler must only be used from the server thread.");
        }
    }

    private static void runOnServerThread(final LevelAccessor level, final Runnable runnable) {
        final MinecraftServer server = level.getServer();
        if (server == null) {
            // No server for this level: client level or test mock. Validate rather than dispatch.
            validateServerThread();
            runnable.run();
        } else if (server.isSameThread()) {
            runnable.run();
        } else {
            server.execute(() -> {
                // A stopped server runs submitted tasks inline on the calling thread; drop the
                // operation instead of touching the collections off-thread.
                if (server.isSameThread()) {
                    runnable.run();
                }
            });
        }
    }

    private static void runChunkListeners(final Map<LevelAccessor, HashMap<ChunkPos, ListenerCollection>> schedulers,
                                          final LevelAccessor level, final ChunkPos chunkPos) {
        final HashMap<ChunkPos, ListenerCollection> chunkMap = schedulers.get(level);
        if (chunkMap == null) {
            return;
        }

        final ListenerCollection listeners = chunkMap.get(chunkPos);
        if (listeners != null) {
            listeners.run();
        }
    }

    private static final class TickScheduler {
        private final PriorityQueue<ScheduledRunnable> queue = new PriorityQueue<>();
        private int currentTick;

        public void schedule(final Runnable runnable, final int afterTicks) {
            queue.add(new ScheduledRunnable(currentTick + afterTicks, runnable));
        }

        public void processQueue() {
            while (!queue.isEmpty() && queue.peek().tick <= currentTick) {
                queue.poll().runnable.run();
            }
        }

        public void tick() {
            currentTick++;
        }

        public void clear() {
            currentTick = 0;
            queue.clear();
        }
    }

    private record ScheduledRunnable(int tick, Runnable runnable) implements Comparable<ScheduledRunnable> {
        @Override
        public int compareTo(final ServerScheduler.ScheduledRunnable o) {
            return Integer.compare(tick, o.tick);
        }
    }

    private static abstract class Listeners<T> {
        private final Set<T> listeners = Collections.newSetFromMap(new WeakHashMap<>());

        public void add(final T listener) {
            listeners.add(listener);
        }

        public void remove(final T listener) {
            listeners.remove(listener);
        }

        public boolean isEmpty() {
            return listeners.isEmpty();
        }

        public void clear() {
            listeners.clear();
        }

        public void run(final Consumer<? super T> invoker) {
            // Snapshot: listeners may unregister from inside their callback.
            for (final T listener : List.copyOf(listeners)) {
                invoker.accept(listener);
            }
        }
    }

    private static final class SimpleScheduler extends Listeners<Runnable> {
        public void run() {
            super.run(Runnable::run);
            clear();
        }
    }

    private static final class ListenerCollection extends Listeners<Runnable> {
        public void run() {
            super.run(Runnable::run);
        }
    }

    private static final class ChunkListenerCollection extends Listeners<Consumer<ChunkPos>> {
        public void run(final ChunkPos chunkPos) {
            super.run(consumer -> consumer.accept(chunkPos));
        }
    }
}
