package li.cil.oc2.common.util;

import com.mojang.authlib.GameProfile;
import li.cil.oc2.api.API;
import li.cil.oc2.common.Config;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.util.FakePlayerFactory;

public final class FakePlayerUtils {
    private static final String FAKE_PLAYER_NAME = "[" + API.MOD_ID + "]";

    ///////////////////////////////////////////////////////////////////

    public static ServerPlayer getFakePlayer(final ServerLevel level, final Entity entity) {
        final ServerPlayer player = getFakePlayer(level);
        player.copyPosition(entity);
        player.xRotO = player.getXRot();
        player.yRotO = player.getYRot();
        player.yHeadRot = player.getYRot();
        player.yHeadRotO = player.getYRot();
        return player;
    }

    public static ServerPlayer getFakePlayer(final ServerLevel level) {
        return FakePlayerFactory.get(level, getFakePlayerProfile());
    }

    public static GameProfile getFakePlayerProfile() {
        return new GameProfile(Config.fakePlayerUUID, FAKE_PLAYER_NAME);
    }
}
