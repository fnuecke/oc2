package li.cil.oc2.common.util;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class HorizontalDirectionalBlockUtils {
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
    public static Direction toGlobal(final BlockState blockState, @Nullable final Direction direction) {
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
        final int toGlobal = facing.get2DDataValue();
        final int rotatedIndex = (index + toGlobal) % HORIZONTAL_DIRECTION_COUNT;
        return Direction.from2DDataValue(rotatedIndex);
    }
}
