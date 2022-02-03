/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.util;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.*;

public final class ServerScheduler {
    private static final TickScheduler globalTickScheduler = new TickScheduler();
    private static final WeakHashMap<LevelAccessor, TickScheduler> levelTickSchedulers = new WeakHashMap<>();
    private static final WeakHashMap<LevelAccessor, SimpleScheduler> levelUnloadSchedulers = new WeakHashMap<>();
    private static final WeakHashMap<LevelAccessor, HashMap<ChunkPos, SimpleScheduler>> chunkLoadSchedulers = new WeakHashMap<>();
    private static final WeakHashMap<LevelAccessor, HashMap<ChunkPos, SimpleScheduler>> chunkUnloadSchedulers = new WeakHashMap<>();

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
        MinecraftForge.EVENT_BUS.register(EventHandler.class);
    }

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
        levelUnloadSchedulers.computeIfAbsent(level, unused -> new SimpleScheduler()).add(listener);
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

    public static void scheduleOnLoad(final LevelAccessor level, final ChunkPos chunkPos, final Runnable listener) {
        chunkLoadSchedulers
            .computeIfAbsent(level, unused -> new HashMap<>())
            .computeIfAbsent(chunkPos, unused -> new SimpleScheduler())
            .add(listener);
    }

    public static void cancelOnLoad(@Nullable final LevelAccessor level, final ChunkPos chunkPos, final Runnable listener) {
        if (level == null) {
            return;
        }

        final HashMap<ChunkPos, SimpleScheduler> chunkMap = chunkLoadSchedulers.get(level);
        if (chunkMap == null) {
            return;
        }

        final SimpleScheduler scheduler = chunkMap.get(chunkPos);
        if (scheduler != null) {
            scheduler.remove(listener);
        }
    }

    public static void scheduleOnUnload(final LevelAccessor level, final ChunkPos chunkPos, final Runnable listener) {
        chunkUnloadSchedulers
            .computeIfAbsent(level, unused -> new HashMap<>())
            .computeIfAbsent(chunkPos, unused -> new SimpleScheduler())
            .add(listener);
    }

    public static void cancelOnUnload(@Nullable final LevelAccessor level, final ChunkPos chunkPos, final Runnable listener) {
        if (level == null) {
            return;
        }

        final HashMap<ChunkPos, SimpleScheduler> chunkMap = chunkUnloadSchedulers.get(level);
        if (chunkMap == null) {
            return;
        }

        final SimpleScheduler scheduler = chunkMap.get(chunkPos);
        if (scheduler != null) {
            scheduler.remove(listener);
        }
    }

    ///////////////////////////////////////////////////////////////////

    private static final class EventHandler {
        @SubscribeEvent
        public static void handleServerStoppedEvent(final ServerStoppedEvent event) {
            globalTickScheduler.clear();
            levelTickSchedulers.clear();
            levelUnloadSchedulers.clear();
            chunkLoadSchedulers.clear();
            chunkUnloadSchedulers.clear();
        }

        @SubscribeEvent
        public static void handleLevelUnload(final WorldEvent.Unload event) {
            final LevelAccessor level = event.getWorld();

            levelTickSchedulers.remove(level);
            chunkLoadSchedulers.remove(level);
            chunkUnloadSchedulers.remove(level);

            final SimpleScheduler scheduler = levelUnloadSchedulers.remove(level);
            if (scheduler != null) {
                scheduler.run();
            }
        }

        @SubscribeEvent
        public static void handleChunkLoad(final ChunkEvent.Load event) {
            final HashMap<ChunkPos, SimpleScheduler> chunkMap = chunkLoadSchedulers.get(event.getWorld());
            if (chunkMap == null) {
                return;
            }

            final SimpleScheduler scheduler = chunkMap.get(event.getChunk().getPos());
            if (scheduler != null) {
                scheduler.run();
            }
        }

        @SubscribeEvent
        public static void handleChunkUnload(final ChunkEvent.Unload event) {
            final HashMap<ChunkPos, SimpleScheduler> chunkMap = chunkUnloadSchedulers.get(event.getWorld());
            if (chunkMap == null) {
                return;
            }

            final SimpleScheduler scheduler = chunkMap.get(event.getChunk().getPos());
            if (scheduler != null) {
                scheduler.run();
            }
        }

        @SubscribeEvent
        public static void handleServerTick(final TickEvent.ServerTickEvent event) {
            if (event.phase == TickEvent.Phase.START) {
                globalTickScheduler.tick();

                for (final TickScheduler scheduler : levelTickSchedulers.values()) {
                    scheduler.tick();
                }
            }
        }

        @SubscribeEvent
        public static void handleLevelTick(final TickEvent.WorldTickEvent event) {
            if (event.phase != TickEvent.Phase.START) {
                return;
            }

            globalTickScheduler.processQueue();

            final TickScheduler scheduler = levelTickSchedulers.get(event.world);
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
}
