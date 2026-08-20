/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest;

import dev.architectury.hooks.level.entity.PlayerHooks;
import li.cil.oc2.common.util.FakePlayerUtils;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;

public final class FakePlayerTests {
    public static void fakePlayerIsRecognisedAsFake(final GameTestHelper helper) {
        final ServerPlayer fakePlayer = FakePlayerUtils.getFakePlayer(helper.getLevel());
        if (!PlayerHooks.isFake(fakePlayer)) {
            throw new GameTestAssertException("oc2's fake player is not recognised as fake; mods guarding "
                    + "on PlayerHooks.isFake would treat robot actions as a real player's");
        }

        final Player ordinaryPlayer = helper.makeMockPlayer(GameType.SURVIVAL);
        if (PlayerHooks.isFake(ordinaryPlayer)) {
            throw new GameTestAssertException("an ordinary player is being reported as fake");
        }

        helper.succeed();
    }

    // ------------------------------------------------------------- //

    private FakePlayerTests() {
    }
}
