package li.cil.oc2.common.util;

import com.mojang.authlib.GameProfile;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import li.cil.oc2.api.API;
import li.cil.oc2.common.Config;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketDirection;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;

import javax.annotation.Nullable;

public final class FakePlayerUtils {
    private static final String FAKE_PLAYER_NAME = "[" + API.MOD_ID + "]";

    ///////////////////////////////////////////////////////////////////

    public static ServerPlayerEntity getFakePlayer(final ServerWorld world, final Entity entity) {
        final ServerPlayerEntity player = getFakePlayer(world);
        player.copyLocationAndAnglesFrom(entity);
        player.prevRotationPitch = player.rotationPitch;
        player.prevRotationYaw = player.rotationYaw;
        player.rotationYawHead = player.rotationYaw;
        player.prevRotationYawHead = player.rotationYawHead;
        return player;
    }

    public static ServerPlayerEntity getFakePlayer(final ServerWorld world) {
        final FakePlayer player = FakePlayerFactory.get(world, new GameProfile(Config.fakePlayerUUID, FAKE_PLAYER_NAME));

        // We need to give our fake player a fake network handler because some events we want
        // to use the fake player with will unconditionally access this field.
        if (player.connection == null) {
            player.connection = new FakeServerPlayNetHandler(player);
        }

        return player;
    }

    ///////////////////////////////////////////////////////////////////

    private static class FakeServerPlayNetHandler extends ServerPlayNetHandler {
        public FakeServerPlayNetHandler(final FakePlayer fakePlayer) {
            super(fakePlayer.server, new NetworkManager(PacketDirection.CLIENTBOUND), fakePlayer);
        }

        @Override
        public void sendPacket(final IPacket<?> packetIn, @Nullable final GenericFutureListener<? extends Future<? super Void>> futureListeners) {
        }
    }
}
