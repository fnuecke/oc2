package li.cil.oc2.common;

import net.minecraft.world.IWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppedEvent;
import org.jetbrains.annotations.NotNull;

import java.util.PriorityQueue;
import java.util.WeakHashMap;

public final class ServerScheduler {
    public static void register() {
        MinecraftForge.EVENT_BUS.register(EventHandler.class);
    }

    private static final Scheduler serverScheduler = new Scheduler();
    private static final WeakHashMap<IWorld, Scheduler> worldSchedulers = new WeakHashMap<>();

    public static void schedule(final Runnable runnable) {
        schedule(runnable, 0);
    }

    public static void schedule(final Runnable runnable, final int afterTicks) {
        serverScheduler.schedule(runnable, afterTicks);
    }

    public static void schedule(final IWorld world, final Runnable runnable) {
        schedule(world, runnable, 0);
    }

    public static void schedule(final IWorld world, final Runnable runnable, final int afterTicks) {
        final Scheduler scheduler = worldSchedulers.computeIfAbsent(world, w -> new Scheduler());
        scheduler.schedule(runnable, afterTicks);
    }

    private static final class EventHandler {
        @SubscribeEvent
        public static void handleServerStoppedEvent(final FMLServerStoppedEvent event) {
            serverScheduler.clear();
            worldSchedulers.clear();
        }

        @SubscribeEvent
        public static void handleWorldUnload(final WorldEvent.Unload event) {
            worldSchedulers.remove(event.getWorld());
        }

        @SubscribeEvent
        public static void handleServerTick(final TickEvent.ServerTickEvent event) {
            if (event.phase == TickEvent.Phase.START) {
                for (final Scheduler scheduler : worldSchedulers.values()) {
                    scheduler.tick();
                }
            }
        }

        @SubscribeEvent
        public static void handleWorldTick(final TickEvent.WorldTickEvent event) {
            if (event.phase != TickEvent.Phase.START) {
                return;
            }

            final Scheduler scheduler = worldSchedulers.get(event.world);
            if (scheduler != null) {
                scheduler.processQueue();
            }
        }
    }

    private static final class Scheduler {
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
}
