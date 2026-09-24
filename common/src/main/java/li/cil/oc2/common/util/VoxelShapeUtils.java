/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.util;

import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Quaternionfc;
import org.joml.Vector3f;

public final class VoxelShapeUtils {
    private static final ThreadLocal<VoxelShape> TEMP_SHAPE = new ThreadLocal<>();

    public static VoxelShape rotateHorizontalClockwise(final VoxelShape shape) {
        TEMP_SHAPE.set(Shapes.empty());
        shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
            final VoxelShape rotatedBox = Shapes.box(minZ, minY, 1.0 - maxX, maxZ, maxY, 1.0 - minX);
            TEMP_SHAPE.set(Shapes.or(TEMP_SHAPE.get(), rotatedBox));
        });
        return TEMP_SHAPE.get();
    }

    public static VoxelShape rotateAroundCenter(final VoxelShape shape, final Quaternionfc rotation) {
        TEMP_SHAPE.set(Shapes.empty());
        shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
            final Vector3f min = rotateAroundCenter(minX, minY, minZ, rotation);
            final Vector3f max = rotateAroundCenter(maxX, maxY, maxZ, rotation);
            final VoxelShape rotatedBox = Shapes.box(
                Math.min(min.x(), max.x()), Math.min(min.y(), max.y()), Math.min(min.z(), max.z()),
                Math.max(min.x(), max.x()), Math.max(min.y(), max.y()), Math.max(min.z(), max.z()));
            TEMP_SHAPE.set(Shapes.or(TEMP_SHAPE.get(), rotatedBox));
        });
        return TEMP_SHAPE.get();
    }

    // --------------------------------------------------------------------- //

    private static Vector3f rotateAroundCenter(final double x, final double y, final double z, final Quaternionfc rotation) {
        final Vector3f rotated = new Vector3f((float) x - 0.5f, (float) y - 0.5f, (float) z - 0.5f).rotate(rotation).add(0.5f, 0.5f, 0.5f);
        return rotated.set(snapToVoxelGrid(rotated.x()), snapToVoxelGrid(rotated.y()), snapToVoxelGrid(rotated.z()));
    }

    private static float snapToVoxelGrid(final float value) {
        return Math.round(value * 16) / 16f;
    }
}
