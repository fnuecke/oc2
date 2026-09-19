/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.util;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.Vec3;

public record BlockPlacement(Direction facing, Direction upward) {
    public static BlockPlacement free(final BlockPlaceContext context) {
        final Direction clicked = context.getClickedFace();
        final Vec3 position = context.getClickLocation();
        final Direction horizontalDirection = context.getHorizontalDirection();
        if (clicked.getAxis().isVertical()) {
            return new BlockPlacement(horizontalDirection.getOpposite(), horizontalDirection);
        }

        final Direction right = clicked.getCounterClockWise();
        final double towardsRight = fractionAlong(position, right) - 0.5;
        final double towardsTop = fractionAlong(position, Direction.UP) - 0.5;

        if (Math.abs(towardsRight) >= Math.abs(towardsTop)) {
            return new BlockPlacement(towardsRight < 0 ? right : right.getOpposite(), Direction.UP);
        } else {
            return new BlockPlacement(towardsTop > 0 ? Direction.DOWN : Direction.UP, clicked);
        }
    }

    public static BlockPlacement attached(final BlockPlaceContext context) {
        final Direction clicked = context.getClickedFace();
        final Direction horizontalDirection = context.getHorizontalDirection();
        return new BlockPlacement(clicked, clicked == Direction.DOWN ? horizontalDirection.getOpposite() : horizontalDirection);
    }

    // --------------------------------------------------------------------- //

    private static double fractionAlong(final Vec3 position, final Direction direction) {
        final double value = switch (direction.getAxis()) {
            case X -> position.x;
            case Y -> position.y;
            case Z -> position.z;
        };

        final double fraction = value - Math.floor(value);
        return direction.getAxisDirection() == Direction.AxisDirection.POSITIVE ? fraction : 1 - fraction;
    }
}
