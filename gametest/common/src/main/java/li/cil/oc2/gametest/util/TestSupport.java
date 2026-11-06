/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.util;

import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.transaction.Transaction;
import li.cil.oc2.common.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;

public final class TestSupport {
    public static final String MOD_ID = "oc2_gametest";
    public static final String TEMPLATE = "empty";
    public static final String TEMPLATE_ID = MOD_ID + ":" + TEMPLATE;
    public static final int WORK_Y = 2;
    public static final int MAX_X = 39;

    public static final BlockPos POWER_POS = new BlockPos(1, WORK_Y, 2);
    public static final BlockPos COMPUTER_POS = new BlockPos(2, WORK_Y, 2);
    public static final BlockPos CABLE_POS = new BlockPos(3, WORK_Y, 2);
    public static final BlockPos DEVICE_POS = new BlockPos(4, WORK_Y, 2);
    public static final BlockPos ROBOT_POS = new BlockPos(12, WORK_Y, 2);

    public static final int BOOT_TIMEOUT_TICKS = 6_000;
    public static final int CPM_BOOT_TIMEOUT_TICKS = 1_000;
    public static final int REAL_TIME_TIMEOUT_TICKS = 60_000;

    public static String script(final String... lines) {
        return String.join("\n", lines) + "\n";
    }

    public static GameTestAssertException failure(final GameTestHelper helper, final String message) {
        return new GameTestAssertException(Component.literal(message), 0);
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

    public static void assertValue(final GameTestHelper helper, final String what, @Nullable final Object expected, @Nullable final Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw failure(helper, what + ": expected " + expected + ", got " + actual);
        }
    }

    public static void assertArrayEquals(final GameTestHelper helper, final String what, final Object[] expected, final Object[] actual) {
        if (!Arrays.equals(expected, actual)) {
            throw failure(helper, what + ": expected " + Arrays.toString(expected) + ", got " + Arrays.toString(actual));
        }
    }

    public static <T> T assertNotNull(final GameTestHelper helper, @Nullable final T value, final String what) {
        if (value == null) {
            throw new GameTestAssertException(Component.literal(what + " is null"), 0);
        }
        return value;
    }

    public static void assertThrows(final GameTestHelper helper, final String what, final Runnable action) {
        try {
            action.run();
        } catch (final IllegalArgumentException e) {
            return;
        }
        throw failure(helper, what + " should have been rejected");
    }

    public static <T> T abort(final Function<TransactionContext, T> operation) {
        try (Transaction transaction = Transaction.openRoot()) {
            return operation.apply(transaction);
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
            throw new GameTestAssertException(Component.literal("nothing placed at " + pos + " from " + stack), 0);
        }
    }

    public static void useOn(final GameTestHelper helper, final Player player, final ItemStack stack, final BlockPos pos, final Direction face) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hitResult(helper, pos, face)));
    }

    public static BlockHitResult hitResult(final GameTestHelper helper, final BlockPos pos, final Direction face) {
        final BlockPos absolute = helper.absolutePos(pos);
        final Vec3 location = Vec3.atCenterOf(absolute)
            .add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
        return new BlockHitResult(location, face, absolute, false);
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

    public static void onTestEnd(final GameTestHelper helper, final Runnable action) {
        testInfo(helper).addListener(new GameTestListener() {
            @Override
            public void testStructureLoaded(final GameTestInfo info) {
            }

            @Override
            public void testPassed(final GameTestInfo info, final GameTestRunner runner) {
                action.run();
            }

            @Override
            public void testFailed(final GameTestInfo info, final GameTestRunner runner) {
                action.run();
            }

            @Override
            public void testAddedForRerun(final GameTestInfo original, final GameTestInfo copy, final GameTestRunner runner) {
            }
        });
    }

    // --------------------------------------------------------------------- //

    private TestSupport() {
    }

    private static GameTestInfo testInfo(final GameTestHelper helper) {
        try {
            final Field field = GameTestHelper.class.getDeclaredField("testInfo");
            field.setAccessible(true);
            return (GameTestInfo) field.get(helper);
        } catch (final ReflectiveOperationException e) {
            throw new GameTestAssertException(Component.literal("could not read the test info: " + e), 0);
        }
    }
}
