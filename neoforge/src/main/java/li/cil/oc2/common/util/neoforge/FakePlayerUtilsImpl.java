/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.util.neoforge;

import li.cil.oc2.common.util.FakePlayerUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

public final class FakePlayerUtilsImpl {
    public static ServerPlayer getFakePlayer(final ServerLevel level) {
        final ServerPlayer player = FakePlayerFactory.get(level, FakePlayerUtils.getFakePlayerProfile());
        if (player.gameMode.getGameModeForPlayer() != GameType.SURVIVAL) {
            player.gameMode.setGameModeForPlayer(GameType.SURVIVAL, null);
        }
        return player;
    }

    private FakePlayerUtilsImpl() {
    }
}
