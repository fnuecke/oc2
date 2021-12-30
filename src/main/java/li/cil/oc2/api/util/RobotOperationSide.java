package li.cil.oc2.api.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.core.Direction;

import javax.annotation.Nullable;

/**
 * A more restrictive version of {@link Side}, intended for robot operation APIs.
 */
public enum RobotOperationSide {
    FRONT(Direction.SOUTH),
    front(FRONT),
    f(FRONT),

    UP(Direction.UP),
    up(UP),
    u(UP),

    DOWN(Direction.DOWN),
    down(DOWN),
    d(DOWN),
    ;

    private final Direction direction;

    RobotOperationSide(final Direction direction) {
        this.direction = direction;
    }

    RobotOperationSide(final RobotOperationSide parent) {
        this(parent.direction);
    }

    public Direction getDirection() {
        return direction;
    }

    public static Direction getAdjustedDirection(@Nullable final RobotOperationSide side, final Entity entity) {
        Direction direction = side == null
                ? RobotOperationSide.FRONT.getDirection()
                : side.getDirection();
        if (direction.getAxis().isHorizontal()) {
            final int horizontalIndex = entity.getDirection().get2DDataValue();
            for (int i = 0; i < horizontalIndex; i++) {
                direction = direction.getClockWise();
            }
        }
        return direction;
    }
}
