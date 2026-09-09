/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class IndicatorRenderer {
    private static final int PULSE_TICKS = 40;

    // --------------------------------------------------------------------- //

    public static void render(final PoseStack stack, final MultiBufferSource bufferSource, final AABB bounds, final Vector3f color, final Vector3f colorBright, final long gameTime, final float partialTicks) {
        final float phase = (gameTime % PULSE_TICKS + partialTicks) / PULSE_TICKS;
        final float blend = (1 + Mth.sin(phase * (float) (Math.PI * 2))) * 0.5f;

        final float r = Mth.lerp(blend, color.x(), colorBright.x());
        final float g = Mth.lerp(blend, color.y(), colorBright.y());
        final float b = Mth.lerp(blend, color.z(), colorBright.z());

        addBoxVertices(stack.last().pose(), bufferSource.getBuffer(ModRenderType.getIndicator()), bounds, r, g, b);
    }

    // --------------------------------------------------------------------- //

    private static void addBoxVertices(final Matrix4f matrix, final VertexConsumer consumer, final AABB bounds, final float r, final float g, final float b) {
        final float x0 = (float) bounds.minX;
        final float y0 = (float) bounds.minY;
        final float z0 = (float) bounds.minZ;
        final float x1 = (float) bounds.maxX;
        final float y1 = (float) bounds.maxY;
        final float z1 = (float) bounds.maxZ;

        // Wound counter-clockwise as seen from outside; the render type culls back faces.
        addVertex(matrix, consumer, x0, y0, z0, r, g, b);
        addVertex(matrix, consumer, x0, y1, z0, r, g, b);
        addVertex(matrix, consumer, x1, y1, z0, r, g, b);
        addVertex(matrix, consumer, x1, y0, z0, r, g, b);

        addVertex(matrix, consumer, x0, y0, z1, r, g, b);
        addVertex(matrix, consumer, x1, y0, z1, r, g, b);
        addVertex(matrix, consumer, x1, y1, z1, r, g, b);
        addVertex(matrix, consumer, x0, y1, z1, r, g, b);

        addVertex(matrix, consumer, x0, y0, z0, r, g, b);
        addVertex(matrix, consumer, x0, y0, z1, r, g, b);
        addVertex(matrix, consumer, x0, y1, z1, r, g, b);
        addVertex(matrix, consumer, x0, y1, z0, r, g, b);

        addVertex(matrix, consumer, x1, y0, z0, r, g, b);
        addVertex(matrix, consumer, x1, y1, z0, r, g, b);
        addVertex(matrix, consumer, x1, y1, z1, r, g, b);
        addVertex(matrix, consumer, x1, y0, z1, r, g, b);

        addVertex(matrix, consumer, x0, y0, z0, r, g, b);
        addVertex(matrix, consumer, x1, y0, z0, r, g, b);
        addVertex(matrix, consumer, x1, y0, z1, r, g, b);
        addVertex(matrix, consumer, x0, y0, z1, r, g, b);

        addVertex(matrix, consumer, x0, y1, z0, r, g, b);
        addVertex(matrix, consumer, x0, y1, z1, r, g, b);
        addVertex(matrix, consumer, x1, y1, z1, r, g, b);
        addVertex(matrix, consumer, x1, y1, z0, r, g, b);
    }

    private static void addVertex(final Matrix4f matrix, final VertexConsumer consumer, final float x, final float y, final float z, final float r, final float g, final float b) {
        consumer.addVertex(matrix, x, y, z).setColor(r, g, b, 1f);
    }

    private IndicatorRenderer() {
    }
}
