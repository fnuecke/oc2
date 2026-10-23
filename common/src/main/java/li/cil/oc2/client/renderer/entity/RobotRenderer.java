/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import li.cil.oc2.client.renderer.ModRenderType;
import li.cil.oc2.client.renderer.Overlays;
import li.cil.oc2.client.renderer.entity.model.RobotModel;
import li.cil.oc2.common.entity.Robot;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;

public final class RobotRenderer extends EntityRenderer<Robot, RobotRenderState> {
    private static final Vector3f GLOW_COLOR = new Vector3f(0.16f, 0.43f, 0.45f);
    private static final Vector3f GLOW_COLOR_BRIGHT = new Vector3f(0.35f, 0.95f, 1f);

    private static final AABB GLOW_BOUNDS = new AABB(-6.25 / 16.0, 7 / 16.0, -6.25 / 16.0, 6.25 / 16.0, 8 / 16.0, 6.25 / 16.0);

    private static final float STATUS_INNER_X = 2 / 16f;
    private static final float STATUS_OUTER_X = 4 / 16f;
    private static final float STATUS_Y = 10 / 16f;
    private static final float STATUS_HEIGHT = 1 / 16f;
    private static final float STATUS_Z = 7 / 16f;

    private final RobotModel model;

    // --------------------------------------------------------------------- //

    public RobotRenderer(final EntityRendererProvider.Context context) {
        super(context);
        model = new RobotModel(context.bakeLayer(RobotModel.ROBOT_MODEL_LAYER));
    }

    // --------------------------------------------------------------------- //

    @Override
    public RobotRenderState createRenderState() {
        return new RobotRenderState();
    }

    @Override
    public void extractRenderState(final Robot entity, final RobotRenderState renderState, final float partialTicks) {
        super.extractRenderState(entity, renderState, partialTicks);

        final Robot.AnimationState state = entity.getAnimationState();

        // NB: we don't entityYaw given to use because that uses a plain lerp which can lead to ugly
        //     jumps in case we get a wrapped rotationYaw synced from the server (leading to ~360
        //     degree delta to the last known previous rotation). Haven't figured out where to
        //     alternatively prevent this wrapping or patch the prev value instead.
        final float partialRotation = Mth.degreesDifferenceAbs(entity.yRotO, entity.getYRot()) * partialTicks;
        renderState.rotation = Mth.approachDegrees(entity.yRotO, entity.getYRot(), partialRotation);
        renderState.baseY = state.getBaseRenderOffsetY(partialTicks);
        renderState.topY = state.getTopRenderOffsetY(partialTicks);
        renderState.topRotationY = state.getTopRenderRotationY(partialTicks);
        renderState.isRunning = entity.getVirtualMachine().isRunning();
        renderState.statusColor = entity.getStatusColor();
        renderState.statusValue = entity.getStatusValue();
        renderState.gameTime = entity.level().getGameTime();
        renderState.partialTicks = partialTicks;
    }

    @Override
    public void submit(final RobotRenderState state, final PoseStack stack, final SubmitNodeCollector collector, final CameraRenderState camera) {
        stack.pushPose();
        stack.mulPose(Axis.YN.rotationDegrees(state.rotation));

        model.submit(stack, collector, state.lightCoords, state.baseY, state.topY, state.topRotationY);

        if (state.isRunning) {
            stack.pushPose();
            model.applyTopTransform(stack, state.topY, state.topRotationY);
            renderStatusLights(stack, collector, state.statusColor, state.statusValue);
            stack.popPose();
        }

        if (state.topY > Robot.AnimationState.TOP_IDLE_Y) {
            stack.translate(0, state.baseY, 0);
            Overlays.renderBox(ModRenderType.getGlow(), stack, collector, GLOW_BOUNDS, GLOW_COLOR, GLOW_COLOR_BRIGHT, state.gameTime, state.partialTicks);
        }

        stack.popPose();

        super.submit(state, stack, collector, camera);
    }

    // --------------------------------------------------------------------- //

    private static void renderStatusLights(final PoseStack stack, final SubmitNodeCollector collector, final int color, final float value) {
        if (value <= 0) {
            return;
        }

        final float r = (color >>> 16) / 255f;
        final float g = ((color >>> 8) & 0xFF) / 255f;
        final float b = (color & 0xFF) / 255f;

        final float top = STATUS_Y + STATUS_HEIGHT * value;
        collector.submitCustomGeometry(stack, ModRenderType.getIndicator(), (pose, consumer) -> {
            addQuad(pose, consumer, -STATUS_OUTER_X, -STATUS_INNER_X, top, r, g, b);
            addQuad(pose, consumer, STATUS_INNER_X, STATUS_OUTER_X, top, r, g, b);
        });
    }

    private static void addQuad(final PoseStack.Pose matrix, final VertexConsumer consumer, final float x0, final float x1, final float y1, final float r, final float g, final float b) {
        addVertex(matrix, consumer, x0, STATUS_Y, r, g, b);
        addVertex(matrix, consumer, x1, STATUS_Y, r, g, b);
        addVertex(matrix, consumer, x1, y1, r, g, b);
        addVertex(matrix, consumer, x0, y1, r, g, b);
    }

    private static void addVertex(final PoseStack.Pose matrix, final VertexConsumer consumer, final float x, final float y, final float r, final float g, final float b) {
        consumer.addVertex(matrix, x, y, STATUS_Z).setColor(r, g, b, 1f);
    }
}
