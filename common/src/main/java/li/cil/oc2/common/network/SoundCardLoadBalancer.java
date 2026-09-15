/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.TickEvent;
import li.cil.oc2.common.Config;
import li.cil.oc2.common.bus.device.vm.item.SoundCardDevice;
import li.cil.oc2.common.network.message.SoundCardAudioMessage;
import li.cil.oc2.common.util.BlockLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class SoundCardLoadBalancer {
    private static final int SENDS_PER_TICK = 4; // audio is sequential, so we need to buffer.
    private static final int LISTENER_REFRESH_INTERVAL_TICKS = 10;

    private static final Balancer BALANCER = new Balancer();
    private static final Set<SoundCardDevice.Stream> STREAMS = new HashSet<>();

    private static int ticksUntilListenerRefresh;

    // --------------------------------------------------------------------- //

    public static void initialize() {
        TickEvent.SERVER_PRE.register(server -> handleServerTick());
        LifecycleEvent.SERVER_STOPPED.register(server -> {
            STREAMS.clear();
            BALANCER.clear();
        });
    }

    public static void add(final SoundCardDevice.Stream stream) {
        STREAMS.add(stream);
    }

    public static void remove(final SoundCardDevice.Stream stream) {
        STREAMS.remove(stream);
        BALANCER.remove(stream);
    }

    // --------------------------------------------------------------------- //

    private SoundCardLoadBalancer() {
    }

    private static void handleServerTick() {
        if (--ticksUntilListenerRefresh <= 0) {
            ticksUntilListenerRefresh = LISTENER_REFRESH_INTERVAL_TICKS;
            refreshListeners();
        }

        BALANCER.tick();
    }

    private static void refreshListeners() {
        for (final SoundCardDevice.Stream stream : STREAMS) {
            final Optional<BlockLocation> location = stream.getLocation();
            if (location.isEmpty() || !(location.get().tryGetLevel().orElse(null) instanceof final ServerLevel level)) {
                continue;
            }

            for (final ServerPlayer player : level.players()) {
                if (isInEarshot(player, location.get().blockPos())) {
                    BALANCER.update(stream, player);
                }
            }
        }
    }

    private static boolean isInEarshot(final ServerPlayer player, final BlockPos pos) {
        return player.distanceToSqr(Vec3.atCenterOf(pos)) <= SoundCardDevice.AUDIBLE_DISTANCE * SoundCardDevice.AUDIBLE_DISTANCE;
    }

    // --------------------------------------------------------------------- //

    private static final class Balancer extends StreamingLoadBalancer<SoundCardDevice.Stream, SoundEntry> {
        Balancer() {
            super(() -> Config.soundCardAverageMaxBytesPerSecond, SENDS_PER_TICK, System::currentTimeMillis);
        }

        @Override
        protected SoundEntry createEntry(final SoundCardDevice.Stream stream) {
            return new SoundEntry(stream);
        }
    }

    private static final class SoundEntry extends StreamingLoadBalancer.Entry {
        private final SoundCardDevice.Stream stream;

        SoundEntry(final SoundCardDevice.Stream stream) {
            this.stream = stream;
        }

        @Override
        protected boolean isReady() {
            return stream.hasAudio() && stream.getLocation().isPresent();
        }

        @Override
        protected void send(final List<ServerPlayer> recipients) {
            final SoundCardDevice.Chunk chunk = stream.poll();
            final Optional<BlockLocation> location = stream.getLocation();
            if (chunk == null || location.isEmpty()
                || !(location.get().tryGetLevel().orElse(null) instanceof final ServerLevel level)) {
                return;
            }

            final BlockPos pos = location.get().blockPos();
            final List<ServerPlayer> listeners = recipients.stream()
                .filter(player -> player.level() == level && isInEarshot(player, pos))
                .toList();
            if (listeners.isEmpty()) {
                return;
            }

            BALANCER.consumeBudget(chunk.samples().length * listeners.size());

            final SoundCardAudioMessage message = new SoundCardAudioMessage(stream.getId(), pos, chunk.sampleRate(), chunk.samples());
            for (final ServerPlayer player : listeners) {
                Network.sendToClient(message, player);
            }
        }
    }
}
