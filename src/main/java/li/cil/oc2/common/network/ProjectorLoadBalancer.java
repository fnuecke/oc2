/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalNotification;
import li.cil.oc2.api.API;
import li.cil.oc2.common.Config;
import li.cil.oc2.common.blockentity.ProjectorBlockEntity;
import li.cil.oc2.common.network.message.ProjectorFramebufferMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * Mostly round-robin load balancer for allowing projectors to send data to clients.
 * <p>
 * We try to satisfy two limits here:
 * <ul>
 * <li>
 * Server side, limiting overall bandwidth consumed by projectors.
 * </li>
 * <li>
 * Client side, limiting per-client bandwidth consumed by projectors.
 * </li>
 * </ul>
 * <p>
 * To achieve this, there's a global budget and a per-projector skip count. The global budget
 * controls overall data sent from the server. The skip counts modulate the round-robin behaviour
 * of the load balancer. For example, projectors further away from their closest player will get
 * a penalty, as will projectors with a large number of players watching them.
 */
@Mod.EventBusSubscriber(modid = API.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ProjectorLoadBalancer {
    private static final long CACHE_EXPIRES_AFTER = 2000; /* In milliseconds */

    /**
     * Maps projectors to their specific info, i.e. players watching them and sending state.
     * <p>
     * This is used to let projectors register new frames to be sent, and to register new players
     * with already being tracked projectors.
     * <p>
     * Player watching state is expired manually, by checking the last update time for players in
     * the info instance every tick. This is required in case two players start watching a projector
     * but then only one keeps watching it. In that case, we want to still remove the other player
     * from the projector info, but keep the info for the still watching player.
     */
    private static final Cache<ProjectorBlockEntity, ProjectorInfo> PROJECTOR_INFO = CacheBuilder.newBuilder()
        .weakKeys()
        .expireAfterWrite(Duration.ofMillis(CACHE_EXPIRES_AFTER))
        .removalListener(ProjectorLoadBalancer::handleProjectorInfoRemoved)
        .build();

    /**
     * Global byte budget for sending stuff to clients. This is filled up every tick and consumed
     * when sending packets to clients. We only send stuff when budget is non-negative.
     */
    private static final AtomicInteger BUDGET = new AtomicInteger(getMaxBudget());

    /**
     * Pointer into our circular projector linked list pointing to the projector we last sent a packet for.
     * <p>
     * At the same time represents the head of our doubly-linked, circular list of projectors. This list is
     * used to do the round-robin load balancing, by advancing it to the next projector until we find one
     * that can send something every tick.
     */
    @Nullable private static ProjectorInfo lastSender;

    /**
     * Updates timestamp of a player currently watching a projector.
     */
    public static void updateWatcher(final ProjectorBlockEntity projector, final ServerPlayer player) {
        try {
            PROJECTOR_INFO.get(projector, () -> addProjectorInfo(projector))
                .players.put(player, System.currentTimeMillis());
        } catch (final ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Notifies the load balancer that a projector has data to send.
     * <p>
     * Ignored if there are no players watching the projector.
     */
    public static void offerFrame(final ProjectorBlockEntity projector, final Supplier<ByteBuffer> messageSupplier) {
        final ProjectorInfo info = PROJECTOR_INFO.getIfPresent(projector);
        if (info != null) {
            info.nextFrameSupplier = messageSupplier;
        }
    }

    /**
     * Expires cached values. Checks if we can send something, and if so starts async
     * generation of the package to send.
     */
    @SubscribeEvent
    public static void handleServerTick(final TickEvent.ServerTickEvent event) {
        PROJECTOR_INFO.cleanUp();
        removeExpiredPlayers();

        if (BUDGET.updateAndGet(ProjectorLoadBalancer::replenishBudget) > 0) {
            sendNextReadyPacket();
        }
    }

    /**
     * Cleanup when server stops.
     */
    @SubscribeEvent
    public static void handleServerStopped(final ServerStoppedEvent event) {
        PROJECTOR_INFO.invalidateAll();
    }

    private static int getMaxBudget() {
        // We allow over-budgeting projectors to some degree, to allow short bursts of larger frame changes.
        // Otherwise, this would be divided by twenty, since we attempt to send every tick.
        return Config.projectorAverageMaxBytesPerSecond / 2;
    }

    private static int replenishBudget(final int budget) {
        return Math.min(getMaxBudget(), budget + Math.max(1, Config.projectorAverageMaxBytesPerSecond / 20));
    }

    private static ProjectorInfo addProjectorInfo(final ProjectorBlockEntity projector) {
        projector.setRequiresKeyframe(); // When first watcher starts, immediately request keyframe.
        final ProjectorInfo info = new ProjectorInfo(projector.getBlockPos());
        if (lastSender == null) {
            // No sender yet, start the circle.
            lastSender = info;
        } else {
            // Just add after last sender.
            lastSender.add(info);
        }
        return info;
    }

    private static void handleProjectorInfoRemoved(final RemovalNotification<ProjectorBlockEntity, ProjectorInfo> notification) {
        final ProjectorInfo info = requireNonNull(notification.getValue());

        if (lastSender == info) {
            if (lastSender.next == lastSender) {
                lastSender = null; // Last element in list, clear list.
            } else {
                lastSender = info.next; // Shift current entry to next.
            }
        }

        info.remove();
    }

    private static void removeExpiredPlayers() {
        if (lastSender == null) {
            return;
        }

        final ProjectorInfo start = lastSender;
        do {
            lastSender.removeExpiredPlayers();
            lastSender = lastSender.next;
        } while (lastSender != start);
    }

    private static void sendNextReadyPacket() {
        if (lastSender == null) {
            return;
        }

        final ProjectorInfo start = lastSender;
        do {
            lastSender = lastSender.next;
            if (lastSender.sendIfReady()) {
                return;
            }
        } while (lastSender != start);
    }

    /**
     * Tracks info for a single projector. This class is an entry in a circular double linked list,
     * i.e. the last entry will always point back to the first entry, which makes looping over it
     * for the round-robin load-balancing more comfortable.
     */
    private static class ProjectorInfo {
        private static final ExecutorService ENCODER_WORKERS = Executors.newCachedThreadPool(r -> {
            final Thread thread = new Thread(r);
            thread.setDaemon(true);
            thread.setName("Projector Frame Encoder");
            return thread;
        });

        /**
         * Pointers to next and previous entries in linked list. May point to this if it's the only entry.
         */
        private ProjectorInfo next, previous;

        /**
         * Position of the projector this info is for in the world.
         */
        private final BlockPos projectorPos;

        /**
         * The players watching this projector, to know where to send the data and how strongly to penalize.
         */
        private final WeakHashMap<ServerPlayer, Long> players = new WeakHashMap<>();

        /**
         * The current penalty, in the form of rounds in the round-robin to skip.
         */
        private int skipCount;
        @Nullable private Supplier<ByteBuffer> nextFrameSupplier;
        @Nullable private Future<?> runningEncode;

        public ProjectorInfo(final BlockPos projectorPos) {
            next = previous = this;
            this.projectorPos = projectorPos;
        }

        public void add(final ProjectorInfo info) {
            info.next = next;
            next.previous = info;
            next = info;
            info.previous = this;
        }

        public void remove() {
            if (previous == null) {
                return;
            }

            previous.next = next;
            next.previous = previous;

            previous = null;
            next = null;
        }

        public void removeExpiredPlayers() {
            players.entrySet().removeIf(entry -> System.currentTimeMillis() - entry.getValue() > CACHE_EXPIRES_AFTER);
        }

        public boolean sendIfReady() {
            if (skipCount > 0) {
                skipCount--;
                return false;
            }

            final boolean isReady = !players.isEmpty() && nextFrameSupplier != null && (runningEncode == null || runningEncode.isDone());
            if (isReady) {
                sendAsync();
                updateSkipCount();
            }

            return isReady;
        }

        private void sendAsync() {
            assert nextFrameSupplier != null;
            final Supplier<ByteBuffer> frameSupplier = nextFrameSupplier;
            nextFrameSupplier = null;

            assert runningEncode == null || runningEncode.isDone();
            runningEncode = ENCODER_WORKERS.submit(() -> {
                final ByteBuffer frame = frameSupplier.get();
                final int budgetCost = frame.limit() * players.size();
                BUDGET.accumulateAndGet(budgetCost, (budget, cost) -> budget - cost);

                final ProjectorFramebufferMessage message = new ProjectorFramebufferMessage(projectorPos, frame);
                for (final ServerPlayer player : players.keySet()) {
                    Network.sendToClient(message, player);
                }
            });
        }

        private void updateSkipCount() {
            skipCount = 0;

            double closestPlayerDistanceSqr = Double.MAX_VALUE;
            final Vec3 blockCenter = Vec3.atCenterOf(projectorPos);
            for (final ServerPlayer player : players.keySet()) {
                skipCount++;
                final double distance = player.distanceToSqr(blockCenter);
                closestPlayerDistanceSqr = Math.min(closestPlayerDistanceSqr, distance);
            }

            final double closestPlayerDistance = Math.sqrt(closestPlayerDistanceSqr);
            if (closestPlayerDistance > 16) {
                skipCount++;
            }
        }
    }
}
