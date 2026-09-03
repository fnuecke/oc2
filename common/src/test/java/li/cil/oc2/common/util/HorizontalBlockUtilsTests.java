/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.util;

import li.cil.oc2.api.util.Side;
import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public final class HorizontalBlockUtilsTests {
    @BeforeAll
    public static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    public void absoluteSidesIgnoreFacing() {
        for (final Direction facing : Direction.Plane.HORIZONTAL) {
            final BlockState blockState = facing(facing);
            assertEquals(Direction.NORTH, HorizontalBlockUtils.toGlobal(blockState, Side.NORTH));
            assertEquals(Direction.SOUTH, HorizontalBlockUtils.toGlobal(blockState, Side.SOUTH));
            assertEquals(Direction.WEST, HorizontalBlockUtils.toGlobal(blockState, Side.WEST));
            assertEquals(Direction.EAST, HorizontalBlockUtils.toGlobal(blockState, Side.EAST));
        }
    }

    @Test
    public void relativeSidesFollowFacing() {
        for (final Direction facing : Direction.Plane.HORIZONTAL) {
            final BlockState blockState = facing(facing);
            assertEquals(facing, HorizontalBlockUtils.toGlobal(blockState, Side.FRONT));
            assertEquals(facing.getOpposite(), HorizontalBlockUtils.toGlobal(blockState, Side.BACK));
        }
    }

    @Test
    public void relativeAndAbsoluteSidesDivergeWhenRotated() {
        final BlockState blockState = facing(Direction.EAST);
        assertNotEquals(
            HorizontalBlockUtils.toGlobal(blockState, Side.EAST),
            HorizontalBlockUtils.toGlobal(blockState, Side.RIGHT));
    }

    @Test
    public void verticalSidesIgnoreFacing() {
        for (final Direction facing : Direction.Plane.HORIZONTAL) {
            final BlockState blockState = facing(facing);
            assertEquals(Direction.UP, HorizontalBlockUtils.toGlobal(blockState, Side.UP));
            assertEquals(Direction.DOWN, HorizontalBlockUtils.toGlobal(blockState, Side.DOWN));
        }
    }

    @Test
    public void toLocalInvertsToGlobal() {
        for (final Direction facing : Direction.Plane.HORIZONTAL) {
            final BlockState blockState = facing(facing);
            for (final Side side : Side.values()) {
                final Direction global = HorizontalBlockUtils.toGlobal(blockState, side);
                final Direction local = HorizontalBlockUtils.toLocal(blockState, side);
                assertEquals(local, HorizontalBlockUtils.toLocal(blockState, global));
            }
        }
    }

    @Test
    public void sideIndicesAreRelative() {
        for (int index = 0; index < 6; index++) {
            final Side side = Side.byIndex(index);
            assertEquals(Direction.from3DDataValue(index), side.getDirection());
            assertEquals(side.getDirection().getAxis().isHorizontal(), side.isRelative());
        }
    }

    private static BlockState facing(final Direction facing) {
        return Blocks.FURNACE.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, facing);
    }
}
