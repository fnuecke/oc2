/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import li.cil.oc2.client.renderer.ModRenderType;
import li.cil.oc2.client.renderer.ProjectorDepthRenderer;
import li.cil.oc2.common.block.FlippableOrientableBlock;
import li.cil.oc2.common.blockentity.ProjectorBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public class ProjectorRenderer implements BlockEntityRenderer<ProjectorBlockEntity, ProjectorRenderer.State> {
    public static final class State extends BlockEntityRenderState {
        private boolean isProjecting;
        private boolean hasEnergy;
    }

    // --------------------------------------------------------------------- //

    private static final int LIGHT_COLOR_NEAR = 0x22FFFFFF;
    private static final int LIGHT_COLOR_FAR = 0x00FFFFFF;
    private static final int LENS_COLOR = 0xDDFFFFFF;
    private static final int MISSING_ENERGY_COLOR = 0xEEFF6666;
    private static final int LED_COLOR = 0xCC6688DD;

    private static final float LENS_RIGHT = 0 + 4 / 16f;
    private static final float LENS_LEFT = 1 - 4 / 16f;
    private static final float LENS_BOTTOM = 0 + 4 / 16f;
    private static final float LENS_TOP = 1 - 4 / 16f;

    // --------------------------------------------------------------------- //

    public ProjectorRenderer(final BlockEntityRendererProvider.Context ignored) {
    }

    // --------------------------------------------------------------------- //

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public boolean shouldRender(final ProjectorBlockEntity projector, final Vec3 position) {
        return !ProjectorDepthRenderer.isRenderingProjectorDepth() &&
            BlockEntityRenderer.super.shouldRender(projector, position);
    }

    @Override
    public boolean shouldRenderOffScreen() {
        // Render bounding box of projectors (vastly) exceeds their block position, so they need
        // to be treated as global renderers, and cannot be culled with their chunk.
        return true;
    }

    @Override
    public void extractRenderState(final ProjectorBlockEntity projector, final State state, final float partialTicks, final Vec3 cameraPosition,
                                   @Nullable final ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(projector, state, partialTicks, cameraPosition, crumbling);

        state.isProjecting = projector.isProjecting();
        state.hasEnergy = projector.hasEnergy();
        if (state.isProjecting && state.hasEnergy) {
            ProjectorDepthRenderer.addProjector(projector);
        }
    }

    @Override
    public void submit(final State state, final PoseStack stack, final SubmitNodeCollector collector, final CameraRenderState camera) {
        if (!state.isProjecting) {
            return;
        }

        stack.pushPose();

        alignToFrontFace(state, stack);
        stack.translate(-0.5, 0, 0.5);

        collector.submitCustomGeometry(stack, ModRenderType.getProjectorLight(), state.hasEnergy
            ? ProjectorRenderer::renderProjectorLight
            : ProjectorRenderer::renderMissingEnergyIndicator);

        stack.popPose();
    }

    // --------------------------------------------------------------------- //

    private static void alignToFrontFace(final State state, final PoseStack stack) {
        stack.translate(0.5f, 0.5f, 0.5f);
        stack.mulPose(state.blockState.getValue(FlippableOrientableBlock.ORIENTATION).getRotation());
        stack.mulPose(Axis.YP.rotationDegrees(180));
        stack.translate(0, -0.5f, 0);
    }

    private static void renderProjectorLight(final PoseStack.Pose pose, final VertexConsumer consumer) {
        final float leftFar = 1.25f;
        final float rightFar = -0.25f;
        final float topFar = 1.5f;
        final float bottomFar = 0 + 1 / 16f;

        // Top.
        consumer.addVertex(pose, leftFar, topFar, 1).setColor(LIGHT_COLOR_FAR); // top left far
        consumer.addVertex(pose, LENS_LEFT, LENS_TOP, 0).setColor(LIGHT_COLOR_NEAR); // top left near
        consumer.addVertex(pose, LENS_RIGHT, LENS_TOP, 0).setColor(LIGHT_COLOR_NEAR); // top right near
        consumer.addVertex(pose, rightFar, topFar, 1).setColor(LIGHT_COLOR_FAR); // top right far

        // Bottom.
        consumer.addVertex(pose, leftFar, bottomFar, 1).setColor(LIGHT_COLOR_FAR); // bottom left far
        consumer.addVertex(pose, LENS_LEFT, LENS_BOTTOM, 0).setColor(LIGHT_COLOR_NEAR); // bottom left near
        consumer.addVertex(pose, LENS_RIGHT, LENS_BOTTOM, 0).setColor(LIGHT_COLOR_NEAR); // bottom right near
        consumer.addVertex(pose, rightFar, bottomFar, 1).setColor(LIGHT_COLOR_FAR); // bottom right far

        // Left.
        consumer.addVertex(pose, leftFar, topFar, 1).setColor(LIGHT_COLOR_FAR); // top left far
        consumer.addVertex(pose, leftFar, bottomFar, 1).setColor(LIGHT_COLOR_FAR); // bottom left far
        consumer.addVertex(pose, LENS_LEFT, LENS_BOTTOM, 0).setColor(LIGHT_COLOR_NEAR); // bottom left near
        consumer.addVertex(pose, LENS_LEFT, LENS_TOP, 0).setColor(LIGHT_COLOR_NEAR); // top left near

        // Right.
        consumer.addVertex(pose, rightFar, topFar, 1).setColor(LIGHT_COLOR_FAR); // top right far
        consumer.addVertex(pose, LENS_RIGHT, LENS_TOP, 0).setColor(LIGHT_COLOR_NEAR); // top right near
        consumer.addVertex(pose, LENS_RIGHT, LENS_BOTTOM, 0).setColor(LIGHT_COLOR_NEAR); // bottom right near
        consumer.addVertex(pose, rightFar, bottomFar, 1).setColor(LIGHT_COLOR_FAR); // bottom right far

        renderLens(pose, consumer, LENS_COLOR);
        renderLed(pose, consumer);
    }

    private static void renderMissingEnergyIndicator(final PoseStack.Pose pose, final VertexConsumer consumer) {
        renderLens(pose, consumer, MISSING_ENERGY_COLOR);
        renderLed(pose, consumer);
    }

    private static void renderLens(final PoseStack.Pose pose, final VertexConsumer consumer, final int color) {
        final float lensDepth = -1 / 16f;
        consumer.addVertex(pose, LENS_RIGHT, LENS_BOTTOM, lensDepth).setColor(color);
        consumer.addVertex(pose, LENS_LEFT, LENS_BOTTOM, lensDepth).setColor(color);
        consumer.addVertex(pose, LENS_LEFT, LENS_TOP, lensDepth).setColor(color);
        consumer.addVertex(pose, LENS_RIGHT, LENS_TOP, lensDepth).setColor(color);
    }

    private static void renderLed(final PoseStack.Pose pose, final VertexConsumer consumer) {
        final float ledRight = 0 + 7 / 16f;
        final float ledLeft = 0 + 9 / 16f;
        final float ledBottom = 0 + 3 / 16f;
        final float ledTop = 0 + 4 / 16f;
        final float ledDepth = -0.75f / 16f;

        consumer.addVertex(pose, ledRight, ledBottom, ledDepth).setColor(LED_COLOR);
        consumer.addVertex(pose, ledLeft, ledBottom, ledDepth).setColor(LED_COLOR);
        consumer.addVertex(pose, ledLeft, ledTop, ledDepth).setColor(LED_COLOR);
        consumer.addVertex(pose, ledRight, ledTop, ledDepth).setColor(LED_COLOR);
    }
}
