package li.cil.oc2.common.util;

import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

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
}
