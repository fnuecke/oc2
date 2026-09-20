/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import li.cil.oc2.client.renderer.ModRenderType;
import li.cil.oc2.client.renderer.Overlays;
import li.cil.oc2.client.renderer.entity.model.RobotModel;
import li.cil.oc2.common.entity.Robot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;

public final class RobotRenderer extends EntityRenderer<Robot> {
    private static final Vector3f GLOW_COLOR = new Vector3f(0.16f, 0.43f, 0.45f);
    private static final Vector3f GLOW_COLOR_BRIGHT = new Vector3f(0.35f, 0.95f, 1f);

    private static final AABB GLOW_BOUNDS = new AABB(-6.25 / 16.0, 7 / 16.0, -6.25 / 16.0, 6.25 / 16.0, 8 / 16.0, 6.25 / 16.0);

    private final RobotModel model;

    // --------------------------------------------------------------------- //

    public RobotRenderer(final EntityRendererProvider.Context context) {
        super(context);
        model = new RobotModel(context.bakeLayer(RobotModel.ROBOT_MODEL_LAYER));
    }

    // --------------------------------------------------------------------- //

    @Override
    public ResourceLocation getTextureLocation(final Robot entity) {
        return RobotModel.ROBOT_ENTITY_TEXTURE;
    }

    @Override
    public void render(final Robot entity, final float entityYaw, final float partialTicks, final PoseStack stack, final MultiBufferSource bufferSource, final int packedLight) {
        final Robot.AnimationState state = entity.getAnimationState();
        final long gameTime = entity.level().getGameTime();
        final float time = gameTime + partialTicks;
        float deltaTime = Minecraft.getInstance().getTimer().getGameTimeDeltaTicks();
        state.update(time, deltaTime, entity.level().random);

        stack.pushPose();
        // NB: we don't entityYaw given to use because that uses a plain lerp which can lead to ugly
        //     jumps in case we get a wrapped rotationYaw synced from the server (leading to ~360
        //     degree delta to the last known previous rotation). Haven't figured out where to
        //     alternatively prevent this wrapping or patch the prev value instead.
        final float partialRotation = Mth.degreesDifferenceAbs(entity.yRotO, entity.getYRot()) * partialTicks;
        final float rotation = Mth.approachDegrees(entity.yRotO, entity.getYRot(), partialRotation);
        stack.mulPose(Axis.YN.rotationDegrees(rotation));

        model.setupAnim(entity, 0, 0, 0, 0, 0);

        final VertexConsumer consumer = bufferSource.getBuffer(model.renderType(getTextureLocation(entity)));
        model.renderToBuffer(stack, consumer, packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);

        if (state.topRenderOffsetY > Robot.AnimationState.TOP_IDLE_Y) {
            stack.translate(0, state.baseRenderOffsetY, 0);
            Overlays.renderBox(ModRenderType.getGlow(), stack, bufferSource, GLOW_BOUNDS, GLOW_COLOR, GLOW_COLOR_BRIGHT, gameTime, partialTicks);
        }

        stack.popPose();
    }
}
