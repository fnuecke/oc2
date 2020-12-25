package li.cil.oc2.common.util;

import net.minecraft.world.IWorld;
import net.minecraft.world.chunk.IChunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppedEvent;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public final class ServerScheduler {
    private static final TickScheduler globalTickScheduler = new TickScheduler();
    private static final WeakHashMap<IWorld, TickScheduler> worldTickSchedulers = new WeakHashMap<>();
    private static final WeakHashMap<IWorld, UnloadScheduler> worldUnloadSchedulers = new WeakHashMap<>();
    private static final WeakHashMap<IChunk, UnloadScheduler> chunkUnloadSchedulers = new WeakHashMap<>();

    ///////////////////////////////////////////////////////////////////

    public static void register() {
        MinecraftForge.EVENT_BUS.register(EventHandler.class);
    }

    public static void schedule(final Runnable runnable) {
        schedule(runnable, 0);
    }

    public static void schedule(final Runnable runnable, final int afterTicks) {
        globalTickScheduler.schedule(runnable, afterTicks);
    }

    public static void schedule(final IWorld world, final Runnable runnable) {
        schedule(world, runnable, 0);
    }

    public static void schedule(final IWorld world, final Runnable runnable, final int afterTicks) {
        final TickScheduler scheduler = worldTickSchedulers.computeIfAbsent(world, w -> new TickScheduler());
        scheduler.schedule(runnable, afterTicks);
    }

    public static void scheduleOnUnload(final IWorld world, final Runnable listener) {
        worldUnloadSchedulers.computeIfAbsent(world, unused -> new UnloadScheduler()).add(listener);
    }

    public static void cancelOnUnload(@Nullable final IWorld world, final Runnable listener) {
        if (world == null) {
            return;
        }

        final UnloadScheduler scheduler = worldUnloadSchedulers.get(world);
        if (scheduler != null) {
            scheduler.remove(listener);
        }
    }

    public static void scheduleOnUnload(final IChunk chunk, final Runnable listener) {
        chunkUnloadSchedulers.computeIfAbsent(chunk, unused -> new UnloadScheduler()).add(listener);
    }

    public static void cancelOnUnload(@Nullable final IChunk chunk, final Runnable listener) {
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
        @SubscribeEvent
        public static void handleServerStoppedEvent(final FMLServerStoppedEvent event) {
            globalTickScheduler.clear();
            worldTickSchedulers.clear();
            worldUnloadSchedulers.clear();
            chunkUnloadSchedulers.clear();
        }

        @SubscribeEvent
        public static void handleWorldUnload(final WorldEvent.Unload event) {
            worldTickSchedulers.remove(event.getWorld());

            final List<IChunk> unloadedChunks = chunkUnloadSchedulers.keySet().stream()
                    .filter(chunk -> chunk.getWorldForge() == event.getWorld())
                    .collect(Collectors.toList());
            for (final IChunk chunk : unloadedChunks) {
                chunkUnloadSchedulers.remove(chunk);
            }

            final UnloadScheduler scheduler = worldUnloadSchedulers.remove(event.getWorld());
            if (scheduler != null) {
                scheduler.run();
            }
        }

        @SubscribeEvent
        public static void handleChunkUnload(final ChunkEvent.Unload event) {
            final UnloadScheduler scheduler = chunkUnloadSchedulers.remove(event.getChunk());
            if (scheduler != null) {
                scheduler.run();
            }
        }

        @SubscribeEvent
        public static void handleServerTick(final TickEvent.ServerTickEvent event) {
            if (event.phase == TickEvent.Phase.START) {
                globalTickScheduler.tick();

                for (final TickScheduler scheduler : worldTickSchedulers.values()) {
                    scheduler.tick();
                }
            }
        }

        @SubscribeEvent
        public static void handleWorldTick(final TickEvent.WorldTickEvent event) {
            if (event.phase != TickEvent.Phase.START) {
                return;
            }

            globalTickScheduler.processQueue();

            final TickScheduler scheduler = worldTickSchedulers.get(event.world);
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
        public int compareTo(final ServerScheduler.ScheduledRunnable o) {
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
