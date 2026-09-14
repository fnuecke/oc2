/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.TickEvent;
import li.cil.oc2.common.Config;
import li.cil.oc2.common.blockentity.ProjectorBlockEntity;
import li.cil.oc2.common.network.message.ProjectorFramebufferMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class ProjectorLoadBalancer {
    private static final int SENDS_PER_TICK = 1; // we encode full frames, so a new one replaces an older one.
    private static final double PENALTY_DISTANCE = 16;

    private static final Balancer BALANCER = new Balancer();

    // --------------------------------------------------------------------- //

    public static void initialize() {
        TickEvent.SERVER_PRE.register(server -> BALANCER.tick());
        LifecycleEvent.SERVER_STOPPED.register(server -> BALANCER.clear());
    }

    /**
     * Updates timestamp of a player currently watching a projector.
     */
    public static void updateWatcher(final ProjectorBlockEntity projector, final ServerPlayer player) {
        BALANCER.update(projector, player);
    }

    /**
     * Notifies the load balancer that a projector has data to send.
     * <p>
     * Ignored if there are no players watching the projector.
     */
    public static void offerFrame(final ProjectorBlockEntity projector, final ProjectorBlockEntity.FrameSupplier frameSupplier) {
        final ProjectorEntry entry = BALANCER.getEntry(projector);
        if (entry != null) {
            entry.nextFrameSupplier = frameSupplier;
        }
    }

    // --------------------------------------------------------------------- //

    private static final class Balancer extends StreamingLoadBalancer<ProjectorBlockEntity, ProjectorEntry> {
        Balancer() {
            super(() -> Config.projectorAverageMaxBytesPerSecond, SENDS_PER_TICK, PENALTY_DISTANCE, System::currentTimeMillis);
        }

        @Override
        protected ProjectorEntry createEntry(final ProjectorBlockEntity projector) {
            return new ProjectorEntry(projector);
        }
    }

    private static final class ProjectorEntry extends StreamingLoadBalancer.Entry {
        private static final ExecutorService ENCODER_WORKERS = Executors.newCachedThreadPool(r -> {
            final Thread thread = new Thread(r);
            thread.setDaemon(true);
            thread.setName("Projector Frame Encoder");
            return thread;
        });

        private final ProjectorBlockEntity projector;
        private final BlockPos projectorPos;

        @Nullable
        private ProjectorBlockEntity.FrameSupplier nextFrameSupplier;
        @Nullable
        private Future<?> runningEncode;

        ProjectorEntry(final ProjectorBlockEntity projector) {
            this.projector = projector;
            this.projectorPos = projector.getBlockPos();
        }

        @Override
        protected Vec3 getPosition() {
            return Vec3.atCenterOf(projectorPos);
        }

        @Override
        protected boolean isReady() {
            return nextFrameSupplier != null && (runningEncode == null || runningEncode.isDone());
        }

        @Override
        protected void send(final List<ServerPlayer> recipients) {
            assert nextFrameSupplier != null;
            final var frameSupplier = nextFrameSupplier;
            nextFrameSupplier = null;

            final boolean forceKeyframe = frameSupplier.consumeRequiresKeyframe();
            runningEncode = ENCODER_WORKERS.submit(() -> {
                final byte[] frame = frameSupplier.encode(forceKeyframe);
                if (frame == null) {
                    return;
                }

                BALANCER.consumeBudget(frame.length * recipients.size());

                final ProjectorFramebufferMessage message = new ProjectorFramebufferMessage(projectorPos, frame);
                for (final ServerPlayer player : recipients) {
                    Network.sendToClient(message, player);
                }
            });
        }

        @Override
        protected void onPlayerAdded() {
            projector.setRequiresKeyframe();
        }
    }

    // --------------------------------------------------------------------- //

    private ProjectorLoadBalancer() {
    }
}
