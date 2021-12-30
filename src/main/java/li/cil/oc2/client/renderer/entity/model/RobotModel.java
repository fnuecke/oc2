package li.cil.oc2.client.renderer.entity.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc2.api.API;
import li.cil.oc2.common.entity.RobotEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.model.TransformationHelper;

public final class RobotModel extends EntityModel<RobotEntity> {
    public static final ModelLayerLocation ROBOT_MODEL_LAYER = new ModelLayerLocation(new ResourceLocation(API.MOD_ID, "robot"), "main");
    public static final ResourceLocation ROBOT_ENTITY_TEXTURE = new ResourceLocation(API.MOD_ID, "textures/entity/robot/robot.png");

    ///////////////////////////////////////////////////////////////////

    private final ModelPart topRenderer;
    private final ModelPart baseRenderer;
    private final ModelPart coreRenderer;
    private float baseY, topY;
    private final float[] topRotation = new float[3];

    ///////////////////////////////////////////////////////////////////

    public RobotModel(final ModelPart modelPart) {
        topRenderer = modelPart.getChild("top");
        baseRenderer = modelPart.getChild("base");
        coreRenderer = modelPart.getChild("core");
    }

    public static LayerDefinition createRobotLayer() {
        final MeshDefinition meshDefinition = new MeshDefinition();
        final PartDefinition partDefinition = meshDefinition.getRoot();
        partDefinition.addOrReplaceChild("top", CubeListBuilder.create()
                .texOffs(1, 1)
                .addBox(-7, 8, -7, 14, 6, 14),
            PartPose.ZERO);
        partDefinition.addOrReplaceChild("base", CubeListBuilder.create()
                .texOffs(1, 23)
                .addBox(-7, 0, -7, 14, 7, 14),
            PartPose.ZERO);
        partDefinition.addOrReplaceChild("core", CubeListBuilder.create()
                .texOffs(1, 34)
                .addBox(-6, 7, -6, 12, 1, 12),
            PartPose.ZERO);
        return LayerDefinition.create(meshDefinition, 64, 64);
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
