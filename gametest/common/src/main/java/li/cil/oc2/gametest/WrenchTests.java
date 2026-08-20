/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest;

import li.cil.oc2.common.block.Blocks;
import li.cil.oc2.common.blockentity.BusCableBlockEntity;
import li.cil.oc2.common.item.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import static li.cil.oc2.gametest.TestSupport.DEVICE_POS;

public final class WrenchTests {
    public static void rotatesOnTopFace(final GameTestHelper helper) {
        helper.setBlock(DEVICE_POS, Blocks.DISK_DRIVE.get());
        final Direction before = helper.getBlockState(DEVICE_POS).getValue(HorizontalDirectionalBlock.FACING);

        useWrench(helper, DEVICE_POS, Direction.UP, false);

        final Direction after = helper.getBlockState(DEVICE_POS).getValue(HorizontalDirectionalBlock.FACING);
        final Direction expected = Rotation.CLOCKWISE_90.rotate(before);
        if (after != expected) {
            throw new GameTestAssertException("wrenching the top face should rotate clockwise: expected "
                    + expected + ", got " + after);
        }
        helper.succeed();
    }

    public static void rotatesOnBottomFace(final GameTestHelper helper) {
        helper.setBlock(DEVICE_POS, Blocks.DISK_DRIVE.get());
        final Direction before = helper.getBlockState(DEVICE_POS).getValue(HorizontalDirectionalBlock.FACING);

        useWrench(helper, DEVICE_POS, Direction.DOWN, false);

        final Direction after = helper.getBlockState(DEVICE_POS).getValue(HorizontalDirectionalBlock.FACING);
        final Direction expected = Rotation.COUNTERCLOCKWISE_90.rotate(before);
        if (after != expected) {
            throw new GameTestAssertException("wrenching the bottom face should rotate counterclockwise: expected "
                    + expected + ", got " + after);
        }
        helper.succeed();
    }

    public static void doesNotRotateOnSideFace(final GameTestHelper helper) {
        helper.setBlock(DEVICE_POS, Blocks.DISK_DRIVE.get());
        final Direction before = helper.getBlockState(DEVICE_POS).getValue(HorizontalDirectionalBlock.FACING);

        useWrench(helper, DEVICE_POS, Direction.NORTH, false);

        final Direction after = helper.getBlockState(DEVICE_POS).getValue(HorizontalDirectionalBlock.FACING);
        if (after != before) {
            throw new GameTestAssertException("wrenching a side face must not rotate: was " + before
                    + ", now " + after);
        }
        helper.succeed();
    }

    public static void sneakingDoesNotBypassBlockInteraction(final GameTestHelper helper) {
        helper.setBlock(DEVICE_POS, Blocks.BUS_CABLE.get());
        final BusCableBlockEntity busCable = busCable(helper, DEVICE_POS);
        busCable.setFacade(new ItemStack(net.minecraft.world.item.Items.STONE));
        if (busCable(helper, DEVICE_POS).getFacade().isEmpty()) {
            throw new GameTestAssertException("test setup failed: facade was not applied");
        }

        useWrench(helper, DEVICE_POS, Direction.UP, true);

        if (helper.getBlockState(DEVICE_POS).isAir()) {
            throw new GameTestAssertException("sneak-wrenching a facaded cable broke the block, which means "
                    + "block interaction was bypassed and the wrench's own break path ran instead");
        }
        if (!busCable(helper, DEVICE_POS).getFacade().isEmpty()) {
            throw new GameTestAssertException("sneak-wrenching should have removed the facade, so the "
                    + "block's interaction handler evidently never ran");
        }
        helper.succeed();
    }

    public static void sneakingBreaksWrenchBreakableBlock(final GameTestHelper helper) {
        helper.setBlock(DEVICE_POS, Blocks.CHARGER.get());

        useWrench(helper, DEVICE_POS, Direction.NORTH, true);

        if (!helper.getBlockState(DEVICE_POS).isAir()) {
            throw new GameTestAssertException("sneak-wrenching a wrench-breakable block should break it, "
                    + "found " + helper.getBlockState(DEVICE_POS));
        }
        helper.succeed();
    }

    // ------------------------------------------------------------- //

    private static void useWrench(final GameTestHelper helper, final BlockPos pos, final Direction face, final boolean sneaking) {
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setShiftKeyDown(sneaking);

        final ItemStack wrench = new ItemStack(Items.WRENCH.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, wrench);

        final BlockPos absolute = helper.absolutePos(pos);
        final Vec3 location = Vec3.atCenterOf(absolute)
                .add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);

        player.gameMode.useItemOn(player, helper.getLevel(), wrench, InteractionHand.MAIN_HAND,
                new BlockHitResult(location, face, absolute, false));
    }

    private static BusCableBlockEntity busCable(final GameTestHelper helper, final BlockPos pos) {
        final BlockEntity blockEntity = helper.getBlockEntity(pos);
        if (!(blockEntity instanceof final BusCableBlockEntity busCable)) {
            throw new GameTestAssertException("expected a bus cable at " + pos + ", found " + blockEntity);
        }
        return busCable;
    }

    // ------------------------------------------------------------- //

    private WrenchTests() {
    }
}
