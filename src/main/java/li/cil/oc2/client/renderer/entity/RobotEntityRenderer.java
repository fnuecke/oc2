package li.cil.oc2.client.renderer.entity;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import li.cil.oc2.client.renderer.entity.model.RobotModel;
import li.cil.oc2.common.entity.RobotEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3f;

public final class RobotEntityRenderer extends EntityRenderer<RobotEntity> {
    private final RobotModel model = new RobotModel();

    ///////////////////////////////////////////////////////////////////

    public RobotEntityRenderer(final EntityRendererManager renderManager) {
        super(renderManager);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public ResourceLocation getTextureLocation(final RobotEntity entity) {
        return RobotModel.ROBOT_ENTITY_TEXTURE;
    }

    @Override
    public void render(final RobotEntity entity, final float entityYaw, final float partialTicks, final MatrixStack matrixStack, final IRenderTypeBuffer buffer, final int packedLight) {
        final RobotEntity.AnimationState state = entity.getAnimationState();
        state.update(partialTicks, entity.level.random);

        matrixStack.pushPose();
        // NB: we don't entityYaw given to use because that uses a plain lerp which can lead to ugly
        //     jumps in case we get a wrapped rotationYaw synced from the server (leading to ~360
        //     degree delta to the last known previous rotation). Haven't figured out where to
        //     alternatively prevent this wrapping or patch the prev value instead.
        final float partialRotation = MathHelper.degreesDifferenceAbs(entity.yRotO, entity.yRot) * partialTicks;
        final float rotation = MathHelper.approachDegrees(entity.yRotO, entity.yRot, partialRotation);
        matrixStack.mulPose(Vector3f.YN.rotationDegrees(rotation));

        model.setupAnim(entity, 0, 0, 0, 0, 0);

        final IVertexBuilder builder = buffer.getBuffer(model.renderType(getTextureLocation(entity)));
        model.renderToBuffer(matrixStack, builder, packedLight, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);

        matrixStack.popPose();

//        final RayTraceResult hit = Minecraft.getInstance().objectMouseOver;
//        if (hit instanceof EntityRayTraceResult && entity == ((EntityRayTraceResult) hit).getEntity()) {
//            super.renderName(entity, new StringTextComponent("hi"), matrixStack, buffer, packedLight);
//        }
    }
}
