package li.cil.oc2.common.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import net.minecraft.client.renderer.SpriteCoordinateExpander;
import net.minecraft.client.renderer.block.model.BakedQuad;

import java.nio.ByteBuffer;

/**
 * Provides a safe wrapper around any kind of {@link VertexConsumer} implementation so that
 * call-chaining still works expected.
 * <p>
 * This is primarily a workaround for the broken chaining in {@link SpriteCoordinateExpander}.
 * It may return the inner {@link VertexConsumer} instead of itself from some methods,
 * breaking chaining due to UV remapping missing later on in the chain.
 */
public record ChainableVertexConsumer(VertexConsumer inner) implements VertexConsumer {
    @Override
    public VertexConsumer vertex(final double x, final double y, final double z) {
        inner.vertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer color(final int r, final int g, final int b, final int a) {
        inner.color(r, g, b, a);
        return this;
    }

    @Override
    public VertexConsumer uv(final float u, final float v) {
        inner.uv(u, v);
        return this;
    }

    @Override
    public VertexConsumer overlayCoords(final int u, final int v) {
        inner.overlayCoords(u, v);
        return this;
    }

    @Override
    public VertexConsumer uv2(final int u, final int v) {
        inner.uv2(u, v);
        return this;
    }

    @Override
    public VertexConsumer normal(final float x, final float y, final float z) {
        inner.normal(x, y, z);
        return this;
    }

    @Override
    public void endVertex() {
        inner.endVertex();
    }

    @Override
    public void defaultColor(final int r, final int g, final int b, final int a) {
        inner.defaultColor(r, g, b, a);
    }

    @Override
    public void unsetDefaultColor() {
        inner.unsetDefaultColor();
    }

    @Override
    public void vertex(final float x, final float y, final float z, final float r, final float g, final float b, final float a, final float u, final float v, final int overlay, final int uv2, final float nx, final float ny, final float nz) {
        inner.vertex(x, y, z, r, g, b, a, u, v, overlay, uv2, nx, ny, nz);
    }

    @Override
    public VertexConsumer color(final float r, final float g, final float b, final float a) {
        inner.color(r, g, b, a);
        return this;
    }

    @Override
    public VertexConsumer color(final int rgba) {
        inner.color(rgba);
        return this;
    }

    @Override
    public VertexConsumer uv2(final int uv) {
        inner.uv2(uv);
        return this;
    }

    @Override
    public VertexConsumer overlayCoords(final int uv) {
        inner.overlayCoords(uv);
        return this;
    }

    @Override
    public void putBulkData(final PoseStack.Pose pose, final BakedQuad quad, final float red, final float green, final float blue, final int overlayCoords, final int readExistingColor) {
        inner.putBulkData(pose, quad, red, green, blue, overlayCoords, readExistingColor);
    }

    @Override
    public void putBulkData(final PoseStack.Pose pose, final BakedQuad quad, final float[] baseBrightness, final float red, final float green, final float blue, final int[] lightmapCoords, final int overlayCoords, final boolean readExistingColor) {
        inner.putBulkData(pose, quad, baseBrightness, red, green, blue, lightmapCoords, overlayCoords, readExistingColor);
    }

    @Override
    public VertexConsumer vertex(final Matrix4f matrix, final float x, final float y, final float z) {
        inner.vertex(matrix, x, y, z);
        return this;
    }

    @Override
    public VertexConsumer normal(final Matrix3f matrix, final float x, final float y, final float z) {
        inner.normal(matrix, x, y, z);
        return this;
    }

    @Override
    public void putBulkData(final PoseStack.Pose matrixStack, final BakedQuad bakedQuad, final float red, final float green, final float blue, final int lightmapCoord, final int overlayColor, final boolean readExistingColor) {
        inner.putBulkData(matrixStack, bakedQuad, red, green, blue, lightmapCoord, overlayColor, readExistingColor);
    }

    @Override
    public void putBulkData(final PoseStack.Pose matrixEntry, final BakedQuad bakedQuad, final float red, final float green, final float blue, final float alpha, final int lightmapCoord, final int overlayColor) {
        inner.putBulkData(matrixEntry, bakedQuad, red, green, blue, alpha, lightmapCoord, overlayColor);
    }

    @Override
    public void putBulkData(final PoseStack.Pose matrixEntry, final BakedQuad bakedQuad, final float red, final float green, final float blue, final float alpha, final int lightmapCoord, final int overlayColor, final boolean readExistingColor) {
        inner.putBulkData(matrixEntry, bakedQuad, red, green, blue, alpha, lightmapCoord, overlayColor, readExistingColor);
    }

    @Override
    public void putBulkData(final PoseStack.Pose matrixEntry, final BakedQuad bakedQuad, final float[] baseBrightness, final float red, final float green, final float blue, final float alpha, final int[] lightmapCoords, final int overlayCoords, final boolean readExistingColor) {
        inner.putBulkData(matrixEntry, bakedQuad, baseBrightness, red, green, blue, alpha, lightmapCoords, overlayCoords, readExistingColor);
    }

    @Override
    public int applyBakedLighting(final int lightmapCoord, final ByteBuffer data) {
        return inner.applyBakedLighting(lightmapCoord, data);
    }

    @Override
    public void applyBakedNormals(final Vector3f generated, final ByteBuffer data, final Matrix3f normalTransform) {
        inner.applyBakedNormals(generated, data, normalTransform);
    }
}
