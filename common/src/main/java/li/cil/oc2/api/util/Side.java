/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.util;

import net.minecraft.core.Direction;

import javax.annotation.Nullable;

/**
 * This enum indicates a side of a block device.
 * <p>
 * It is intended to be used by {@link li.cil.oc2.api.bus.device.rpc.RPCDevice} APIs,
 * providing both convenience for the caller by providing a range of aliases, and also
 * stability, in case Mojang decide to rename the enum fields of the {@link Direction}
 * enum at some time in the future.
 * <p>
 * Sides come in two flavors. Absolute sides ({@code north}, {@code south}, {@code west}
 * and {@code east}) always refer to the same direction in the world. Relative sides
 * ({@code front}, {@code back}, {@code left} and {@code right}) are resolved against the
 * orientation of the device's container, and hence refer to different directions in the
 * world as that container is rotated. {@code up} and {@code down} are the same either way.
 * <p>
 * {@link #getDirection()} returns the underlying direction as-is. For relative sides that
 * is a direction in the container's local frame, which must be rotated into world space
 * before use; see {@link #isRelative()}.
 */
public enum Side {
    DOWN(Direction.DOWN),
    down(DOWN),
    d(DOWN),

    UP(Direction.UP),
    up(UP),
    u(UP),

    NORTH(Direction.NORTH),
    north(NORTH),
    n(NORTH),

    SOUTH(Direction.SOUTH),
    south(SOUTH),
    s(SOUTH),

    WEST(Direction.WEST),
    west(WEST),
    w(WEST),

    EAST(Direction.EAST),
    east(EAST),
    e(EAST),

    BACK(Direction.NORTH, true),
    back(BACK),
    b(BACK),

    FRONT(Direction.SOUTH, true),
    front(FRONT),
    f(FRONT),

    LEFT(Direction.WEST, true),
    left(LEFT),
    l(LEFT),

    RIGHT(Direction.EAST, true),
    right(RIGHT),
    r(RIGHT),
    ;

    private static final Side[] BY_INDEX = {DOWN, UP, BACK, FRONT, LEFT, RIGHT};

    @Nullable
    private final Side base;
    private final Direction direction;
    private final boolean relative;

    Side(final Direction direction) {
        this(direction, false);
    }

    Side(final Direction direction, final boolean relative) {
        this.base = null;
        this.direction = direction;
        this.relative = relative;
    }

    Side(final Side side) {
        this.base = side;
        this.direction = side.direction;
        this.relative = side.relative;
    }

    public static Side byIndex(final int index) {
        if (index < 0 || index >= BY_INDEX.length) {
            throw new IllegalArgumentException("Side index [" + index + "] is outside [0, " + (BY_INDEX.length - 1) + "].");
        }
        return BY_INDEX[index];
    }

    public Direction getDirection() {
        return direction;
    }

    public boolean isRelative() {
        return relative;
    }

    @Override
    public String toString() {
        return base != null ? base.toString() : super.toString();
    }
}
