/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.util;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;

import javax.annotation.Nullable;
import java.util.*;

public final class ServerScheduler {
    private static final TickScheduler globalTickScheduler = new TickScheduler();
    private static final WeakHashMap<LevelAccessor, TickScheduler> levelTickSchedulers = new WeakHashMap<>();
    private static final WeakHashMap<LevelAccessor, SimpleScheduler> levelUnloadSchedulers = new WeakHashMap<>();
    private static final WeakHashMap<LevelAccessor, HashMap<ChunkPos, ListenerCollection>> chunkLoadSchedulers = new WeakHashMap<>();
    private static final WeakHashMap<LevelAccessor, HashMap<ChunkPos, ListenerCollection>> chunkUnloadSchedulers = new WeakHashMap<>();

    ///////////////////////////////////////////////////////////////////

    public static void schedule(final Runnable runnable) {
        schedule(runnable, 0);
    }

    public static void schedule(final Runnable runnable, final int afterTicks) {
        globalTickScheduler.schedule(runnable, afterTicks);
    }

    public static void schedule(final LevelAccessor level, final Runnable runnable) {
        schedule(level, runnable, 0);
    }

    public static void schedule(final LevelAccessor level, final Runnable runnable, final int afterTicks) {
        final TickScheduler scheduler = levelTickSchedulers.computeIfAbsent(level, w -> new TickScheduler());
        scheduler.schedule(runnable, afterTicks);
    }

    public static void scheduleOnUnload(final LevelAccessor level, final Runnable listener) {
        levelUnloadSchedulers
            .computeIfAbsent(level, unused -> new SimpleScheduler())
            .add(listener);
    }

    public static void cancelOnUnload(@Nullable final LevelAccessor level, final Runnable listener) {
        if (level == null) {
            return;
        }

        final SimpleScheduler scheduler = levelUnloadSchedulers.get(level);
        if (scheduler != null) {
            scheduler.remove(listener);
        }
    }

    public static void subscribeOnLoad(final LevelAccessor level, final ChunkPos chunkPos, final Runnable listener) {
        chunkLoadSchedulers
            .computeIfAbsent(level, unused -> new HashMap<>())
            .computeIfAbsent(chunkPos, unused -> new ListenerCollection())
            .add(listener);
    }

    public static void unsubscribeOnLoad(@Nullable final LevelAccessor level, final ChunkPos chunkPos, final Runnable listener) {
        if (level == null) {
            return;
        }

        final HashMap<ChunkPos, ListenerCollection> chunkMap = chunkLoadSchedulers.get(level);
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
    }

    public static void subscribeOnUnload(final LevelAccessor level, final ChunkPos chunkPos, final Runnable listener) {
        chunkUnloadSchedulers
            .computeIfAbsent(level, unused -> new HashMap<>())
            .computeIfAbsent(chunkPos, unused -> new ListenerCollection())
            .add(listener);
    }

    public static void unsubscribeOnUnload(@Nullable final LevelAccessor level, final ChunkPos chunkPos, final Runnable listener) {
        if (level == null) {
            return;
        }

        final HashMap<ChunkPos, ListenerCollection> chunkMap = chunkUnloadSchedulers.get(level);
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
    }

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
        LifecycleEvent.SERVER_STOPPED.register(server -> {
            globalTickScheduler.clear();
            levelTickSchedulers.clear();
            levelUnloadSchedulers.clear();
            chunkLoadSchedulers.clear();
            chunkUnloadSchedulers.clear();
        });

        LifecycleEvent.SERVER_LEVEL_UNLOAD.register(level -> {
            levelTickSchedulers.remove(level);
            chunkLoadSchedulers.remove(level);
            chunkUnloadSchedulers.remove(level);

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

    public static void onChunkLoad(final LevelAccessor level, final ChunkPos chunkPos) {
        runChunkListeners(chunkLoadSchedulers, level, chunkPos);
    }

    public static void onChunkUnload(final LevelAccessor level, final ChunkPos chunkPos) {
        runChunkListeners(chunkUnloadSchedulers, level, chunkPos);
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

    private static final class SimpleScheduler {
        private final Set<Runnable> listeners = Collections.newSetFromMap(new WeakHashMap<>());

        public void add(final Runnable listener) {
            listeners.add(listener);
        }

        public void remove(final Runnable listener) {
            listeners.remove(listener);
        }

        public void run() {
            for (final Runnable runnable : listeners) {
                runnable.run();
            }

            listeners.clear();
        }
    }

    private static final class ListenerCollection {
        private final Set<Runnable> listeners = Collections.newSetFromMap(new WeakHashMap<>());

        public void add(final Runnable listener) {
            listeners.add(listener);
        }

        public void remove(final Runnable listener) {
            listeners.remove(listener);
        }

        public boolean isEmpty() {
            return listeners.isEmpty();
        }

        public void run() {
            for (final Runnable runnable : listeners) {
                runnable.run();
            }
        }
    }
}
