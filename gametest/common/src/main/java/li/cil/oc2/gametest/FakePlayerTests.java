/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest;

import dev.architectury.hooks.level.entity.PlayerHooks;
import li.cil.oc2.common.util.FakePlayerUtils;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;

public final class FakePlayerTests {
    public static void fakePlayerIsRecognisedAsFake(final GameTestHelper helper) {
        final ServerPlayer fakePlayer = FakePlayerUtils.getFakePlayer(helper.getLevel());
        if (!PlayerHooks.isFake(fakePlayer)) {
            throw new GameTestAssertException(Component.literal("oc2's fake player is not recognized as fake"), 0);
        }
        if (fakePlayer.gameMode.getGameModeForPlayer() != GameType.SURVIVAL) {
            throw new GameTestAssertException(Component.literal("oc2's fake player is in " + fakePlayer.gameMode.getGameModeForPlayer() + ", expected survival"), 0);
        }

        final Player ordinaryPlayer = helper.makeMockPlayer(GameType.SURVIVAL);
        if (PlayerHooks.isFake(ordinaryPlayer)) {
            throw new GameTestAssertException(Component.literal("an ordinary player is being reported as fake"), 0);
        }

        helper.succeed();
    }

    // --------------------------------------------------------------------- //

    private FakePlayerTests() {
    }
}
