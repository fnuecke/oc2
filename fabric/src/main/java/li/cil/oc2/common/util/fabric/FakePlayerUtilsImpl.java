/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.util.fabric;

import com.mojang.authlib.GameProfile;
import dev.architectury.event.EventResult;
import dev.architectury.hooks.level.entity.fabric.FakePlayers;
import li.cil.oc2.common.util.FakePlayerUtils;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.stats.Stat;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Matches NeoForge's {@code FakePlayerFactory}: no networking, no stats, no dying.
 */
public final class FakePlayerUtilsImpl {
    private static final Map<ServerLevel, ServerPlayer> FAKE_PLAYERS = new WeakHashMap<>();

    // ------------------------------------------------------------- //

    public static void initialize() {
        ServerWorldEvents.UNLOAD.register((server, level) -> FAKE_PLAYERS.remove(level));

        FakePlayers.EVENT.register(player -> player instanceof FabricFakePlayer
                ? EventResult.interruptTrue()
                : EventResult.pass());
    }

    public static ServerPlayer getFakePlayer(final ServerLevel level) {
        return FAKE_PLAYERS.computeIfAbsent(level, l -> new FabricFakePlayer(l, FakePlayerUtils.getFakePlayerProfile()));
    }

    // ------------------------------------------------------------- //

    private static final class FabricFakePlayer extends ServerPlayer {
        private FabricFakePlayer(final ServerLevel level, final GameProfile profile) {
            super(level.getServer(), level, profile, ClientInformation.createDefault());
            this.connection = new FakePlayerPacketListener(level.getServer(), this);
        }

        @Override
        public void displayClientMessage(final Component message, final boolean actionBar) {
        }

        @Override
        public void awardStat(final Stat<?> stat, final int amount) {
        }

        @Override
        public boolean isInvulnerableTo(final DamageSource source) {
            return true;
        }

        @Override
        public boolean canHarmPlayer(final Player player) {
            return false;
        }

        @Override
        public void die(final DamageSource source) {
        }

        @Override
        public void tick() {
        }
    }

    private static final class FakePlayerPacketListener extends ServerGamePacketListenerImpl {
        private static final Connection DUMMY_CONNECTION = new Connection(PacketFlow.SERVERBOUND);

        private FakePlayerPacketListener(final MinecraftServer server, final ServerPlayer player) {
            super(server, DUMMY_CONNECTION, player, CommonListenerCookie.createInitial(player.getGameProfile(), false));
        }

        @Override
        public void tick() {
        }

        @Override
        public void resetPosition() {
        }

        @Override
        public void send(final Packet<?> packet) {
        }
    }

    private FakePlayerUtilsImpl() {
    }
}
