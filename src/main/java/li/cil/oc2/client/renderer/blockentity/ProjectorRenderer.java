package li.cil.oc2.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import li.cil.oc2.api.API;
import li.cil.oc2.client.renderer.ModRenderType;
import li.cil.oc2.client.renderer.ProjectorDepthRenderer;
import li.cil.oc2.common.block.ProjectorBlock;
import li.cil.oc2.common.blockentity.ProjectorBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = API.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ProjectorRenderer implements BlockEntityRenderer<ProjectorBlockEntity> {
    private static final int LIGHT_COLOR_NEAR = 0x22FFFFFF;
    private static final int LIGHT_COLOR_FAR = 0x00FFFFFF;
    private static final int LENS_COLOR = 0xDDFFFFFF;
    private static final int LED_COLOR = 0xCC6688DD;

    private static final float LENS_RIGHT = 0 + 4 / 16f;
    private static final float LENS_LEFT = 1 - 4 / 16f;
    private static final float LENS_BOTTOM = 0 + 4 / 16f;
    private static final float LENS_TOP = 1 - 4 / 16f;

    ///////////////////////////////////////////////////////////////////

    public ProjectorRenderer(final BlockEntityRendererProvider.Context ignored) {
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public boolean shouldRender(final ProjectorBlockEntity projector, final Vec3 position) {
        return !ProjectorDepthRenderer.isIsRenderingProjectorDepth() &&
            projector.isProjecting() &&
            BlockEntityRenderer.super.shouldRender(projector, position);
    }

    @Override
    public boolean shouldRenderOffScreen(final ProjectorBlockEntity p_112306_) {
        // Render bounding box of projectors (vastly) exceeds their block position, so they need
        // to be treated as global renderers, and cannot be culled with their chunk.
        return true;
    }

    @Override
    public void render(final ProjectorBlockEntity projector, final float partialTicks, final PoseStack stack, final MultiBufferSource bufferSource, final int light, final int overlay) {
        stack.pushPose();

        // Align with front face of block.
        final Direction blockFacing = projector.getBlockState().getValue(ProjectorBlock.FACING);
        final Quaternion rotation = new Quaternion(Vector3f.YN, blockFacing.toYRot(), true);
        stack.translate(0.5f, 0, 0.5f);
        stack.mulPose(rotation);

        ProjectorDepthRenderer.addProjector(projector);

        renderProjectorLight(stack, bufferSource);

        stack.popPose();
    }

    ///////////////////////////////////////////////////////////////////

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
}
