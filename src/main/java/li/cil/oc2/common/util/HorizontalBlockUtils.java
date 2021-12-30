package li.cil.oc2.common.util;

import li.cil.oc2.api.util.Side;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.core.Direction;

import javax.annotation.Nullable;

public final class HorizontalBlockUtils {
    public static final int HORIZONTAL_DIRECTION_COUNT = 4;

    ///////////////////////////////////////////////////////////////////

    @Nullable
    public static Direction toLocal(final BlockState blockState, @Nullable final Direction direction) {
        if (direction == null) {
            return null;
        }

        final int index = direction.get2DDataValue();
        if (index < 0) {
            return direction;
        }

        if (!blockState.hasProperty(HorizontalDirectionalBlock.FACING)) {
            return direction;
        }

        final Direction facing = blockState.getValue(HorizontalDirectionalBlock.FACING);
        final int toLocal = HORIZONTAL_DIRECTION_COUNT - facing.get2DDataValue();
        final int rotatedIndex = (index + toLocal) % HORIZONTAL_DIRECTION_COUNT;
        return Direction.from2DDataValue(rotatedIndex);
    }

    @Nullable
    public static Direction toGlobal(final BlockState blockState, @Nullable final Side side) {
        if (side == null) {
            return null;
        }

        final int index = side.get2DDataValue();
        if (index < 0) {
            return side.getDirection();
        }

        if (!blockState.hasProperty(HorizontalDirectionalBlock.FACING)) {
            return side.getDirection();
        }

        final Direction facing = blockState.getValue(HorizontalDirectionalBlock.FACING);
        final int toGlobal = facing.get2DDataValue();
        final int rotatedIndex = (index + toGlobal) % HORIZONTAL_DIRECTION_COUNT;
        return Direction.from2DDataValue(rotatedIndex);
    }
}
