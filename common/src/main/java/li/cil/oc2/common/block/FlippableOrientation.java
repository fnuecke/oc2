/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.block;

import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public enum FlippableOrientation implements StringRepresentable {
    NORTH("north", Direction.NORTH, Direction.UP),
    EAST("east", Direction.EAST, Direction.UP),
    SOUTH("south", Direction.SOUTH, Direction.UP),
    WEST("west", Direction.WEST, Direction.UP),
    NORTH_UPSIDE_DOWN("north_upside_down", Direction.NORTH, Direction.DOWN),
    EAST_UPSIDE_DOWN("east_upside_down", Direction.EAST, Direction.DOWN),
    SOUTH_UPSIDE_DOWN("south_upside_down", Direction.SOUTH, Direction.DOWN),
    WEST_UPSIDE_DOWN("west_upside_down", Direction.WEST, Direction.DOWN),
    UP_NORTH("up_north", Direction.UP, Direction.NORTH),
    UP_EAST("up_east", Direction.UP, Direction.EAST),
    UP_SOUTH("up_south", Direction.UP, Direction.SOUTH),
    UP_WEST("up_west", Direction.UP, Direction.WEST),
    DOWN_NORTH("down_north", Direction.DOWN, Direction.NORTH),
    DOWN_EAST("down_east", Direction.DOWN, Direction.EAST),
    DOWN_SOUTH("down_south", Direction.DOWN, Direction.SOUTH),
    DOWN_WEST("down_west", Direction.DOWN, Direction.WEST);

    // --------------------------------------------------------------------- //

    private final String serializedName;
    private final Direction facing;
    private final Direction up;
    private final Direction right;
    private final int rotationX;
    private final int rotationY;
    private final Quaternionf rotation;

    // --------------------------------------------------------------------- //

    FlippableOrientation(final String serializedName, final Direction facing, final Direction up) {
        this.serializedName = serializedName;
        this.facing = facing;
        this.up = up;
        this.rotationX = computeRotationX(facing, up);
        this.rotationY = computeRotationY(facing, up);
        this.rotation = new Quaternionf().rotationYXZ(
            (float) Math.toRadians(-rotationY),
            (float) Math.toRadians(-rotationX),
            0);
        this.right = Direction.rotate(new Matrix4f().rotation(rotation), Direction.EAST);
    }

    // --------------------------------------------------------------------- //

    public static FlippableOrientation of(final Direction facing, final Direction up) {
        for (final FlippableOrientation orientation : values()) {
            if (orientation.facing == facing && orientation.up == up) {
                return orientation;
            }
        }
        throw new IllegalArgumentException("No orientation faces [" + facing + "] with its top towards [" + up + "].");
    }

    public Direction getFacing() {
        return facing;
    }

    public Direction getUp() {
        return up;
    }

    public Direction getRight() {
        return right;
    }

    public int getRotationX() {
        return rotationX;
    }

    public int getRotationY() {
        return rotationY;
    }

    public Quaternionf getRotation() {
        return rotation;
    }

    public boolean isUpsideDown() {
        return up == Direction.DOWN;
    }

    public FlippableOrientation getUpright() {
        return isUpsideDown() ? of(facing, Direction.UP) : this;
    }

    public FlippableOrientation rotate(final Rotation rotation) {
        return of(rotation.rotate(facing), rotation.rotate(up));
    }

    public FlippableOrientation mirror(final Mirror mirror) {
        return of(mirror.mirror(facing), mirror.mirror(up));
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }

    // --------------------------------------------------------------------- //

    private static int computeRotationX(final Direction facing, final Direction up) {
        return switch (facing) {
            case UP -> 270;
            case DOWN -> 90;
            default -> up == Direction.DOWN ? 180 : 0;
        };
    }

    private static int computeRotationY(final Direction facing, final Direction up) {
        return (int) switch (facing) {
            case UP -> up.toYRot();
            case DOWN -> up.getOpposite().toYRot();
            default -> up == Direction.DOWN ? facing.toYRot() : facing.getOpposite().toYRot();
        };
    }
}
