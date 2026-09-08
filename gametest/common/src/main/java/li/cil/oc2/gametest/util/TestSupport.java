/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.util;

import li.cil.oc2.common.item.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public final class TestSupport {
    public static final String MOD_ID = "oc2_gametest";
    public static final String TEMPLATE = "empty";
    public static final int WORK_Y = 2;
    public static final int MAX_X = 39;

    public static final BlockPos POWER_POS = new BlockPos(1, WORK_Y, 2);
    public static final BlockPos COMPUTER_POS = new BlockPos(2, WORK_Y, 2);
    public static final BlockPos CABLE_POS = new BlockPos(3, WORK_Y, 2);
    public static final BlockPos DEVICE_POS = new BlockPos(4, WORK_Y, 2);

    public static final int BOOT_TIMEOUT_TICKS = 150_000;

    public static GameTestAssertException failure(final GameTestHelper helper, final String message) {
        return new GameTestAssertException(message);
    }

    public static void assertTrue(final GameTestHelper helper, final String what, final boolean condition) {
        if (!condition) {
            throw failure(helper, what);
        }
    }

    public static void assertEquals(final GameTestHelper helper, final String what, final long expected, final long actual) {
        if (expected != actual) {
            throw failure(helper, what + ": expected " + expected + ", got " + actual);
        }
    }

    public static Player fakePlayer(final GameTestHelper helper) {
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setYRot(0);
        return player;
    }

    public static void place(final GameTestHelper helper, final Player player, final ItemStack stack, final BlockPos pos) {
        useOn(helper, player, stack, pos, Direction.UP);
        if (helper.getBlockState(pos).isAir()) {
            throw new GameTestAssertException("nothing placed at " + pos + " from " + stack);
        }
    }

    public static void useOn(final GameTestHelper helper, final Player player, final ItemStack stack, final BlockPos pos, final Direction face) {
        final BlockPos absolute = helper.absolutePos(pos);
        final Vec3 location = Vec3.atCenterOf(absolute)
            .add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);

        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
            new BlockHitResult(location, face, absolute, false)));
    }

    public static void placePower(final GameTestHelper helper, final Player player) {
        place(helper, player, new ItemStack(Items.CREATIVE_ENERGY.get()), POWER_POS);
    }

    public static void breakBlock(final GameTestHelper helper, final BlockPos pos) {
        helper.getLevel().destroyBlock(helper.absolutePos(pos), false, null);
    }

    public static void breakBlockAndDrop(final GameTestHelper helper, final BlockPos pos) {
        helper.getLevel().destroyBlock(helper.absolutePos(pos), true, null);
    }

    public static void assertNotNull(final GameTestHelper helper, @Nullable final Object value, final String what) {
        if (value == null) {
            throw new GameTestAssertException(what + " is null");
        }
    }

    // --------------------------------------------------------------------- //

    private TestSupport() {
    }
}
