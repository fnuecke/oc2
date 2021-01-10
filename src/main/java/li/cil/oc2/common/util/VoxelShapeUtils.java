package li.cil.oc2.common.util;

import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;

public final class VoxelShapeUtils {
    private static final ThreadLocal<VoxelShape> TEMP_SHAPE = new ThreadLocal<>();

    public static VoxelShape rotateHorizontalClockwise(final VoxelShape shape) {
        TEMP_SHAPE.set(VoxelShapes.empty());
        shape.forEachBox((minX, minY, minZ, maxX, maxY, maxZ) -> {
            TEMP_SHAPE.set(VoxelShapes.or(TEMP_SHAPE.get(),
                    VoxelShapes.create(minZ, minY, 1.0 - minX, maxZ, maxY, 1.0 - maxX)
            ));
        });
        return TEMP_SHAPE.get();
    }
}
