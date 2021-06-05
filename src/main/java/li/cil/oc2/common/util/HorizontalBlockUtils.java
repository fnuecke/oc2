package li.cil.oc2.common.util;

import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.util.Direction;

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

        if (!blockState.hasProperty(HorizontalBlock.FACING)) {
            return direction;
        }

        final Direction facing = blockState.getValue(HorizontalBlock.FACING);
        final int toLocal = HORIZONTAL_DIRECTION_COUNT - facing.get2DDataValue();
        final int rotatedIndex = (index + toLocal) % HORIZONTAL_DIRECTION_COUNT;
        return Direction.from2DDataValue(rotatedIndex);
    }

    @Nullable
    public static Direction toGlobal(final BlockState blockState, @Nullable final Direction direction) {
        if (direction == null) {
            return null;
        }

        final int index = direction.get2DDataValue();
        if (index < 0) {
            return direction;
        }

        if (!blockState.hasProperty(HorizontalBlock.FACING)) {
            return direction;
        }

        final Direction facing = blockState.getValue(HorizontalBlock.FACING);
        final int toGlobal = facing.get2DDataValue();
        final int rotatedIndex = (index + toGlobal) % HORIZONTAL_DIRECTION_COUNT;
        return Direction.from2DDataValue(rotatedIndex);
    }
}
