package li.cil.oc2.client.renderer.entity;

import com.mojang.blaze3d.matrix.PoseStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import li.cil.oc2.client.renderer.entity.model.RobotModel;
import li.cil.oc2.common.entity.RobotEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3f;

public final class RobotEntityRenderer extends EntityRenderer<RobotEntity> {
    private final RobotModel model = new RobotModel();

    ///////////////////////////////////////////////////////////////////

    public RobotEntityRenderer(final EntityRenderDispatcher renderManager) {
        super(renderManager);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public ResourceLocation getTextureLocation(final RobotEntity entity) {
        return RobotModel.ROBOT_ENTITY_TEXTURE;
    }

    @Override
    public void render(final RobotEntity entity, final float entityYaw, final float partialTicks, final PoseStack matrixStack, final MultiBufferSource buffer, final int packedLight) {
        final RobotEntity.AnimationState state = entity.getAnimationState();
        state.update(partialTicks, entity.level.random);

        matrixStack.pushPose();
        // NB: we don't entityYaw given to use because that uses a plain lerp which can lead to ugly
        //     jumps in case we get a wrapped rotationYaw synced from the server (leading to ~360
        //     degree delta to the last known previous rotation). Haven't figured out where to
        //     alternatively prevent this wrapping or patch the prev value instead.
        final float partialRotation = Mth.degreesDifferenceAbs(entity.yRotO, entity.yRot) * partialTicks;
        final float rotation = Mth.approachDegrees(entity.yRotO, entity.yRot, partialRotation);
        matrixStack.mulPose(Vector3f.YN.rotationDegrees(rotation));

        model.setupAnim(entity, 0, 0, 0, 0, 0);

        final VertexConsumer builder = buffer.getBuffer(model.renderType(getTextureLocation(entity)));
        model.renderToBuffer(matrixStack, builder, packedLight, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);

        matrixStack.popPose();

//        final RayTraceResult hit = Minecraft.getInstance().objectMouseOver;
//        if (hit instanceof EntityRayTraceResult && entity == ((EntityRayTraceResult) hit).getEntity()) {
//            super.renderName(entity, new StringTextComponent("hi"), matrixStack, buffer, packedLight);
//        }
    }
}
