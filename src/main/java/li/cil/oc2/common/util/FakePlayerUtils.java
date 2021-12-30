package li.cil.oc2.common.util;

import com.mojang.authlib.GameProfile;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import li.cil.oc2.api.API;
import li.cil.oc2.common.Config;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;

import javax.annotation.Nullable;

public final class FakePlayerUtils {
    private static final String FAKE_PLAYER_NAME = "[" + API.MOD_ID + "]";

    ///////////////////////////////////////////////////////////////////

    public static ServerPlayer getFakePlayer(final ServerLevel world, final Entity entity) {
        final ServerPlayer player = getFakePlayer(world);
        player.copyPosition(entity);
        player.xRotO = player.getXRot();
        player.yRotO  = player.getYRot();
        player.yHeadRot = player.getYRot();
        player.yHeadRotO = player.getYRot();
        return player;
    }

    public static ServerPlayer getFakePlayer(final ServerLevel world) {
        final FakePlayer player = FakePlayerFactory.get(world, new GameProfile(Config.fakePlayerUUID, FAKE_PLAYER_NAME));

        // We need to give our fake player a fake network handler because some events we want
        // to use the fake player with will unconditionally access this field.
        if (player.connection == null) {
            player.connection = new FakeServerPlayNetHandler(player);
        }

        return player;
    }

    ///////////////////////////////////////////////////////////////////

    private static class FakeServerPlayNetHandler extends ServerGamePacketListenerImpl {
        public FakeServerPlayNetHandler(final FakePlayer fakePlayer) {
            super(fakePlayer.server, new Connection(PacketFlow.CLIENTBOUND), fakePlayer);
        }

        @Override
        public void send(final Packet<?> packetIn, @Nullable final GenericFutureListener<? extends Future<? super Void>> futureListeners) {
        }
    }
}
