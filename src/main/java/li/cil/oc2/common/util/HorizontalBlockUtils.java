package li.cil.oc2.common.util;

import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

public final class HorizontalBlockUtils {
    public static final int HORIZONTAL_DIRECTION_COUNT = 4;

    ///////////////////////////////////////////////////////////////////

    @Nullable
    public static Direction toLocal(final BlockState blockState, @Nullable final Direction direction) {
        if (direction == null) {
            return null;
        }

        final int index = direction.getHorizontal();
        if (index < 0) {
            return direction;
        }

        final Direction facing = blockState.get(HorizontalFacingBlock.FACING);
        final int toLocal = HORIZONTAL_DIRECTION_COUNT - facing.getHorizontal();
        final int rotatedIndex = (index + toLocal) % HORIZONTAL_DIRECTION_COUNT;
        return Direction.fromHorizontal(rotatedIndex);
    }

    @Nullable
    public static Direction toGlobal(final BlockState blockState, @Nullable final Direction direction) {
        if (direction == null) {
            return null;
        }

        final int index = direction.getHorizontal();
        if (index < 0) {
            return direction;
        }

        final Direction facing = blockState.get(HorizontalFacingBlock.FACING);
        final int toGlobal = facing.getHorizontal();
        final int rotatedIndex = (index + toGlobal) % HORIZONTAL_DIRECTION_COUNT;
        return Direction.fromHorizontal(rotatedIndex);
    }
}
