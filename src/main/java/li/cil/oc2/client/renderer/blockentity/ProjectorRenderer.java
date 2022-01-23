package li.cil.oc2.client.renderer.blockentity;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalNotification;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import li.cil.oc2.client.renderer.ModRenderType;
import li.cil.oc2.common.block.ProjectorBlock;
import li.cil.oc2.common.blockentity.ProjectorBlockEntity;
import li.cil.oc2.common.bus.device.vm.ProjectorVMDevice;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

import java.time.Duration;
import java.util.BitSet;
import java.util.concurrent.ExecutionException;

public final class ProjectorRenderer implements BlockEntityRenderer<ProjectorBlockEntity> {
    private static final int LIGHT_COLOR_NEAR = 0x22FFFFFF;
    private static final int LIGHT_COLOR_FAR = 0x00FFFFFF;
    private static final int LENS_COLOR = 0xDDFFFFFF;
    private static final int LED_COLOR = 0xCC6688DD;

    private static final float LENS_RIGHT = 0 + 4 / 16f;
    private static final float LENS_LEFT = 1 - 4 / 16f;
    private static final float LENS_BOTTOM = 0 + 4 / 16f;
    private static final float LENS_TOP = 1 - 4 / 16f;

    private static final Cache<ProjectorBlockEntity, RenderInfo> textures = CacheBuilder.newBuilder()
        .expireAfterAccess(Duration.ofSeconds(5))
        .removalListener(ProjectorRenderer::handleNoLongerRendering)
        .build();

    ///////////////////////////////////////////////////////////////////

    public ProjectorRenderer(final BlockEntityRendererProvider.Context ignored) {
        MinecraftForge.EVENT_BUS.addListener(ProjectorRenderer::updateCache);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public boolean shouldRender(final ProjectorBlockEntity projector, final Vec3 position) {
        return projector.isProjecting() && BlockEntityRenderer.super.shouldRender(projector, position);
    }

    @Override
    public void render(final ProjectorBlockEntity projector, final float partialTicks, final PoseStack stack, final MultiBufferSource bufferSource, final int light, final int overlay) {
        stack.pushPose();

        // Align with front face of block.
        final Direction blockFacing = projector.getBlockState().getValue(ProjectorBlock.FACING);
        final Quaternion rotation = new Quaternion(Vector3f.YN, blockFacing.toYRot(), true);
        stack.translate(0.5f, 0, 0.5f);
        stack.mulPose(rotation);

        renderProjections(projector, stack, bufferSource);

        renderProjectorLight(stack, bufferSource);

        stack.popPose();
    }

    ///////////////////////////////////////////////////////////////////

    private void renderProjections(final ProjectorBlockEntity projector, final PoseStack stack, final MultiBufferSource bufferSource) {
        final ProjectorBlockEntity.VisibilityData visibilityData = projector.getVisibilityData();
        final BitSet[] visibilities = visibilityData.visibilities();
        if (hasNoVisibleTiles(visibilities)) {
            return;
        }

        final BlockPos projectorPos = projector.getBlockPos();
        final Frustum frustum = new Frustum(stack.last().pose(), RenderSystem.getProjectionMatrix());
        frustum.prepare(projectorPos.getX(), projectorPos.getY(), projectorPos.getZ());
        if (!frustum.isVisible(visibilityData.visibilityBounds())) {
            return;
        }

        stack.pushPose();

        stack.translate(0, 0, 0.49);

        final RenderType renderType = getUpdatedRenderType(projector);
        final VertexConsumer consumer = bufferSource.getBuffer(renderType);
        for (int distance = 0; distance < visibilities.length; distance++) {
            final BitSet visibility = visibilities[distance];
            if (!visibility.isEmpty()) {
                stack.pushPose();

                stack.translate(0, 0, distance + 1);

                renderProjection(stack, consumer, ProjectorBlockEntity.getLayerSize(distance), visibility);

                stack.popPose();
            }
        }

        stack.popPose();
    }

    private void renderProjection(final PoseStack stack, final VertexConsumer consumer, final ProjectorBlockEntity.LayerSize layerSize, final BitSet visibility) {
        final float width = layerSize.width();
        final float height = layerSize.height();
        final int discreteWidth = layerSize.discreteWidth();
        final int discreteHeight = layerSize.discreteHeight();
        final float uOffset = (discreteHeight / height - 1) / 2f;
        final float vOffset = (discreteWidth / width - 1) / 2f;

        stack.translate(-width / 2.0, (discreteHeight - height) / 2.0, 0);
        stack.scale(width, height, 1);
        final Matrix4f matrix = stack.last().pose();

        for (int index = visibility.nextSetBit(0); index >= 0; index = visibility.nextSetBit(index + 1)) {
            final int x = index % discreteWidth;
            final int y = index / discreteWidth;

            final float u0 = x / width - vOffset;
            final float u1 = (x + 1) / width - vOffset;
            final float v0 = y / height - uOffset;
            final float v1 = (y + 1) / height - uOffset;

            consumer.vertex(matrix, u0, v0, 0).uv(1 - u0, 1 - v0).endVertex();
            consumer.vertex(matrix, u1, v0, 0).uv(1 - u1, 1 - v0).endVertex();
            consumer.vertex(matrix, u1, v1, 0).uv(1 - u1, 1 - v1).endVertex();
            consumer.vertex(matrix, u0, v1, 0).uv(1 - u0, 1 - v1).endVertex();
        }
    }

    private void renderProjectorLight(final PoseStack stack, final MultiBufferSource bufferSource) {
        stack.pushPose();

        stack.translate(-0.5, 0, 0.5);
        final VertexConsumer consumer = bufferSource.getBuffer(ModRenderType.getProjectorLight());
        final Matrix4f matrix = stack.last().pose();

        final float leftFar = 1.25f;
        final float rightFar = -0.25f;
        final float topFar = 1.5f;
        final float bottomFar = 0 + 1 / 16f;

        // Top.
        consumer.vertex(matrix, leftFar, topFar, 1).color(LIGHT_COLOR_FAR).endVertex(); // top left far
        consumer.vertex(matrix, LENS_LEFT, LENS_TOP, 0).color(LIGHT_COLOR_NEAR).endVertex(); // top left near
        consumer.vertex(matrix, LENS_RIGHT, LENS_TOP, 0).color(LIGHT_COLOR_NEAR).endVertex(); // top right near
        consumer.vertex(matrix, rightFar, topFar, 1).color(LIGHT_COLOR_FAR).endVertex(); // top right far

        // Bottom.
        consumer.vertex(matrix, leftFar, bottomFar, 1).color(LIGHT_COLOR_FAR).endVertex(); // bottom left far
        consumer.vertex(matrix, LENS_LEFT, LENS_BOTTOM, 0).color(LIGHT_COLOR_NEAR).endVertex(); // bottom left near
        consumer.vertex(matrix, LENS_RIGHT, LENS_BOTTOM, 0).color(LIGHT_COLOR_NEAR).endVertex(); // bottom right near
        consumer.vertex(matrix, rightFar, bottomFar, 1).color(LIGHT_COLOR_FAR).endVertex(); // bottom right far

        // Left.
        consumer.vertex(matrix, leftFar, topFar, 1).color(LIGHT_COLOR_FAR).endVertex(); // top left far
        consumer.vertex(matrix, leftFar, bottomFar, 1).color(LIGHT_COLOR_FAR).endVertex(); // bottom left far
        consumer.vertex(matrix, LENS_LEFT, LENS_BOTTOM, 0).color(LIGHT_COLOR_NEAR).endVertex(); // bottom left near
        consumer.vertex(matrix, LENS_LEFT, LENS_TOP, 0).color(LIGHT_COLOR_NEAR).endVertex(); // top left near

        // Right.
        consumer.vertex(matrix, rightFar, topFar, 1).color(LIGHT_COLOR_FAR).endVertex(); // top right far
        consumer.vertex(matrix, LENS_RIGHT, LENS_TOP, 0).color(LIGHT_COLOR_NEAR).endVertex(); // top right near
        consumer.vertex(matrix, LENS_RIGHT, LENS_BOTTOM, 0).color(LIGHT_COLOR_NEAR).endVertex(); // bottom right near
        consumer.vertex(matrix, rightFar, bottomFar, 1).color(LIGHT_COLOR_FAR).endVertex(); // bottom right far

        renderLens(matrix, consumer);
        renderLed(matrix, consumer);

        stack.popPose();
    }

    private void renderLens(final Matrix4f matrix, final VertexConsumer consumer) {
        final float lensDepth = -1 / 16f;
        consumer.vertex(matrix, LENS_RIGHT, LENS_BOTTOM, lensDepth).color(LENS_COLOR).endVertex();
        consumer.vertex(matrix, LENS_LEFT, LENS_BOTTOM, lensDepth).color(LENS_COLOR).endVertex();
        consumer.vertex(matrix, LENS_LEFT, LENS_TOP, lensDepth).color(LENS_COLOR).endVertex();
        consumer.vertex(matrix, LENS_RIGHT, LENS_TOP, lensDepth).color(LENS_COLOR).endVertex();
    }

    private void renderLed(final Matrix4f matrix, final VertexConsumer consumer) {
        final float ledRight = 0 + 7 / 16f;
        final float ledLeft = 0 + 9 / 16f;
        final float ledBottom = 0 + 3 / 16f;
        final float ledTop = 0 + 4 / 16f;
        final float ledDepth = -0.75f / 16f;

        consumer.vertex(matrix, ledRight, ledBottom, ledDepth).color(LED_COLOR).endVertex();
        consumer.vertex(matrix, ledLeft, ledBottom, ledDepth).color(LED_COLOR).endVertex();
        consumer.vertex(matrix, ledLeft, ledTop, ledDepth).color(LED_COLOR).endVertex();
        consumer.vertex(matrix, ledRight, ledTop, ledDepth).color(LED_COLOR).endVertex();
    }

    private static boolean hasNoVisibleTiles(final BitSet[] visibilities) {
        for (final BitSet visibility : visibilities) {
            if (!visibility.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static RenderType getUpdatedRenderType(final ProjectorBlockEntity projector) {
        final RenderInfo renderInfo = getRenderInfo(projector);

        final NativeImage image = renderInfo.texture().getPixels();
        assert image != null;
        if (projector.applyFramebufferChanges(image::setPixelRGBA)) {
            renderInfo.texture().upload();
        }

        return renderInfo.renderType();
    }

    private static RenderInfo getRenderInfo(final ProjectorBlockEntity projector) {
        try {
            return ProjectorRenderer.textures.get(projector, () -> {
                    final DynamicTexture texture = new DynamicTexture(
                        ProjectorVMDevice.WIDTH,
                        ProjectorVMDevice.HEIGHT,
                        false
                    );
                    return new RenderInfo(texture, ModRenderType.getProjector(texture));
                }
            );
        } catch (final ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private static void updateCache(final TickEvent.ClientTickEvent event) {
        textures.cleanUp();
    }

    private static void handleNoLongerRendering(final RemovalNotification<ProjectorBlockEntity, RenderInfo> notification) {
        final RenderInfo renderInfo = notification.getValue();
        assert renderInfo != null;
        renderInfo.texture().close();
    }

    private record RenderInfo(DynamicTexture texture, RenderType renderType) { }
}
