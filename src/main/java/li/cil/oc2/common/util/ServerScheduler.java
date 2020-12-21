package li.cil.oc2.common.util;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public final class ServerScheduler {
    private static final TickScheduler globalTickScheduler = new TickScheduler();
    private static final WeakHashMap<WorldAccess, TickScheduler> worldTickSchedulers = new WeakHashMap<>();
    private static final WeakHashMap<WorldAccess, UnloadScheduler> worldUnloadSchedulers = new WeakHashMap<>();
    private static final WeakHashMap<WorldChunk, UnloadScheduler> chunkUnloadSchedulers = new WeakHashMap<>();

    ///////////////////////////////////////////////////////////////////

    public static void register() {
        ServerLifecycleEvents.SERVER_STOPPED.register(EventHandler::handleServerStoppedEvent);
        ServerWorldEvents.UNLOAD.register(EventHandler::handleWorldUnload);
        ServerChunkEvents.CHUNK_LOAD.register(EventHandler::handleChunkUnload);
        ServerTickEvents.START_SERVER_TICK.register(EventHandler::handleServerTick);
        ServerTickEvents.START_WORLD_TICK.register(EventHandler::handleWorldTick);
    }

    public static void schedule(final Runnable runnable) {
        schedule(runnable, 0);
    }

    public static void schedule(final Runnable runnable, final int afterTicks) {
        globalTickScheduler.schedule(runnable, afterTicks);
    }

    public static void schedule(final WorldAccess world, final Runnable runnable) {
        schedule(world, runnable, 0);
    }

    public static void schedule(final WorldAccess world, final Runnable runnable, final int afterTicks) {
        final TickScheduler scheduler = worldTickSchedulers.computeIfAbsent(world, w -> new TickScheduler());
        scheduler.schedule(runnable, afterTicks);
    }

    public static void scheduleOnUnload(final WorldAccess world, final Runnable listener) {
        worldUnloadSchedulers.computeIfAbsent(world, unused -> new UnloadScheduler()).add(listener);
    }

    public static void removeOnUnload(@Nullable final WorldAccess world, final Runnable listener) {
        if (world == null) {
            return;
        }

        final UnloadScheduler scheduler = worldUnloadSchedulers.get(world);
        if (scheduler != null) {
            scheduler.remove(listener);
        }
    }

    public static void scheduleOnUnload(final WorldChunk chunk, final Runnable listener) {
        chunkUnloadSchedulers.computeIfAbsent(chunk, unused -> new UnloadScheduler()).add(listener);
    }

    public static void removeOnUnload(@Nullable final WorldChunk chunk, final Runnable listener) {
        if (chunk == null) {
            return;
        }

        final UnloadScheduler scheduler = chunkUnloadSchedulers.get(chunk);
        if (scheduler != null) {
            scheduler.remove(listener);
        }
    }

    ///////////////////////////////////////////////////////////////////

    private static final class EventHandler {
        public static void handleServerStoppedEvent(final MinecraftServer server) {
            globalTickScheduler.clear();
            worldTickSchedulers.clear();
            worldUnloadSchedulers.clear();
            chunkUnloadSchedulers.clear();
        }

        public static void handleWorldUnload(final MinecraftServer server, final ServerWorld world) {
            worldTickSchedulers.remove(world);

            final List<WorldChunk> unloadedChunks = chunkUnloadSchedulers.keySet().stream()
                    .filter(chunk -> chunk.getWorld() == world)
                    .collect(Collectors.toList());
            for (final WorldChunk chunk : unloadedChunks) {
                chunkUnloadSchedulers.remove(chunk);
            }

            final UnloadScheduler scheduler = worldUnloadSchedulers.remove(world);
            if (scheduler != null) {
                scheduler.run();
            }
        }

        public static void handleChunkUnload(final ServerWorld world, final WorldChunk chunk) {
            final UnloadScheduler scheduler = chunkUnloadSchedulers.remove(chunk);
            if (scheduler != null) {
                scheduler.run();
            }
        }

        public static void handleServerTick(final MinecraftServer server) {
            globalTickScheduler.tick();

            for (final TickScheduler scheduler : worldTickSchedulers.values()) {
                scheduler.tick();
            }
        }

        public static void handleWorldTick(final ServerWorld world) {
            globalTickScheduler.processQueue();

            final TickScheduler scheduler = worldTickSchedulers.get(world);
            if (scheduler != null) {
                scheduler.processQueue();
            }
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

    private static final class ScheduledRunnable implements Comparable<ScheduledRunnable> {
        public final int tick;
        public final Runnable runnable;

        private ScheduledRunnable(final int tick, final Runnable runnable) {
            this.tick = tick;
            this.runnable = runnable;
        }

        @Override
        public int compareTo(@NotNull final ServerScheduler.ScheduledRunnable o) {
            return Integer.compare(tick, o.tick);
        }
    }

    private static final class UnloadScheduler {
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
}
