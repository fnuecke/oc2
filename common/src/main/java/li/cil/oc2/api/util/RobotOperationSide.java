/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.util;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;

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

    /**
     * Gets the world-space direction for the specified side relative to the specified entity.
     *
     * @param entity the entity to which the side is relative.
     * @param side   the side to convert to a world-space direction.
     * @return a world-space direction.
     */
    public static Direction toGlobal(final Entity entity, @Nullable final RobotOperationSide side) {
        Direction direction = side == null
            ? RobotOperationSide.FRONT.direction
            : side.direction;
        if (direction.getAxis().isHorizontal()) {
            final int horizontalIndex = entity.getDirection().get2DDataValue();
            for (int i = 0; i < horizontalIndex; i++) {
                direction = direction.getClockWise();
            }
        }
        return direction;
    }
}
