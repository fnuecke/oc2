package li.cil.oc2.client.renderer.entity.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc2.api.API;
import li.cil.oc2.common.entity.RobotEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.model.TransformationHelper;

public final class RobotModel extends EntityModel<RobotEntity> {
    public static final ResourceLocation ROBOT_ENTITY_TEXTURE = new ResourceLocation(API.MOD_ID, "textures/entity/robot/robot.png");

    ///////////////////////////////////////////////////////////////////

    private final ModelPart topRenderer;
    private final ModelPart baseRenderer;
    private final ModelPart coreRenderer;
    private float baseY, topY;
    private final float[] topRotation = new float[3];

    ///////////////////////////////////////////////////////////////////

    public RobotModel() {
        topRenderer = new ModelPart(this, 1, 1)
                .setTexSize(64, 64)
                .addBox(-7, 8, -7, 14, 6, 14);
        baseRenderer = new ModelPart(this, 1, 23)
                .setTexSize(64, 64)
                .addBox(-7, 0, -7, 14, 7, 14);
        coreRenderer = new ModelPart(this, 1, 34)
                .setTexSize(64, 64)
                .addBox(-6, 7, -6, 12, 1, 12);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void setupAnim(final RobotEntity entity, final float limbSwing, final float limbSwingAmount, final float ageInTicks, final float netHeadYaw, final float headPitch) {
        final RobotEntity.AnimationState state = entity.getAnimationState();
        baseY = state.baseRenderOffsetY;
        topY = state.topRenderOffsetY;
        topRotation[1] = state.topRenderRotationY;
    }

    @Override
    public void renderToBuffer(final PoseStack matrixStack, final VertexConsumer buffer, final int packedLight, final int packedOverlay, final float red, final float green, final float blue, final float alpha) {
        matrixStack.pushPose();
        matrixStack.translate(0, topY, 0);
        matrixStack.mulPose(TransformationHelper.quatFromXYZ(topRotation, true));
        topRenderer.render(matrixStack, buffer, packedLight, packedOverlay);
        matrixStack.popPose();

        matrixStack.pushPose();
        matrixStack.translate(0, baseY, 0);
        baseRenderer.render(matrixStack, buffer, packedLight, packedOverlay);
        coreRenderer.render(matrixStack, buffer, LightTexture.pack(15, 15), packedOverlay);
        matrixStack.popPose();
    }
}
