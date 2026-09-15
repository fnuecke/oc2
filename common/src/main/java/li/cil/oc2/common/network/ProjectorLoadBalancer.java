/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.TickEvent;
import li.cil.oc2.common.Config;
import li.cil.oc2.common.blockentity.ProjectorBlockEntity;
import li.cil.oc2.common.network.message.ProjectorFramebufferMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ProjectorLoadBalancer {
    private static final int SENDS_PER_TICK = 1; // we encode full frames, so a new one replaces an older one.

    private static final Balancer BALANCER = new Balancer();

    // --------------------------------------------------------------------- //

    public static void initialize() {
        TickEvent.SERVER_PRE.register(server -> BALANCER.tick());
        LifecycleEvent.SERVER_STOPPED.register(server -> BALANCER.clear());
    }

    public static void update(final ProjectorBlockEntity projector, final ServerPlayer player) {
        BALANCER.update(projector, player);
    }

    public static void offerFrame(final ProjectorBlockEntity projector, final ProjectorBlockEntity.FrameSupplier frameSupplier) {
        final ProjectorEntry entry = BALANCER.getEntry(projector);
        if (entry != null) {
            entry.nextFrameSupplier = frameSupplier;
        }
    }

    @Nullable
    public static CompletableFuture<?> stop(final ProjectorBlockEntity projector) {
        final ProjectorEntry entry = BALANCER.getEntry(projector);
        if (entry == null) {
            return null;
        }

        entry.nextFrameSupplier = null;
        BALANCER.remove(projector);

        return entry.runningEncode;
    }

    // --------------------------------------------------------------------- //

    private static final class Balancer extends StreamingLoadBalancer<ProjectorBlockEntity, ProjectorEntry> {
        Balancer() {
            super(() -> Config.projectorAverageMaxBytesPerSecond, SENDS_PER_TICK, System::currentTimeMillis);
        }

        @Override
        protected ProjectorEntry createEntry(final ProjectorBlockEntity projector) {
            return new ProjectorEntry(projector);
        }
    }

    private static final class ProjectorEntry extends StreamingLoadBalancer.Entry {
        private static final ExecutorService ENCODER_WORKERS = Executors.newFixedThreadPool(
            Math.clamp(Runtime.getRuntime().availableProcessors() / 2, 2, 4), r -> {
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
        private CompletableFuture<?> runningEncode;

        ProjectorEntry(final ProjectorBlockEntity projector) {
            this.projector = projector;
            this.projectorPos = projector.getBlockPos();
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
            runningEncode = CompletableFuture.runAsync(() -> {
                final byte[] frame = frameSupplier.encode(forceKeyframe);
                if (frame == null) {
                    return;
                }

                BALANCER.consumeBudget(frame.length * recipients.size());

                final ProjectorFramebufferMessage message = new ProjectorFramebufferMessage(projectorPos, frame);
                for (final ServerPlayer player : recipients) {
                    Network.sendToClient(message, player);
                }
            }, ENCODER_WORKERS);
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
