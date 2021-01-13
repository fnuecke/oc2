package li.cil.oc2.client.renderer.entity;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import li.cil.oc2.api.API;
import li.cil.oc2.common.entity.RobotEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.common.model.TransformationHelper;

public final class RobotEntityRenderer extends EntityRenderer<RobotEntity> {
    private final RobotModel model = new RobotModel();

    ///////////////////////////////////////////////////////////////////

    public RobotEntityRenderer(final EntityRendererManager renderManager) {
        super(renderManager);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public ResourceLocation getEntityTexture(final RobotEntity entity) {
        return new ResourceLocation(API.MOD_ID, "textures/entity/robot/robot.png");
    }

    @Override
    public void render(final RobotEntity entity, final float entityYaw, final float partialTicks, final MatrixStack matrixStack, final IRenderTypeBuffer buffer, final int packedLight) {
        final RobotEntity.AnimationState state = entity.getAnimationState();
        state.update(partialTicks, entity.world.rand);

        matrixStack.push();
        // NB: we don't entityYaw given to use because that uses a plain lerp which can lead to ugly
        //     jumps in case we get a wrapped rotationYaw synced from the server (leading to ~360
        //     degree delta to the last known previous rotation). Haven't figured out where to
        //     alternatively prevent this wrapping or patch the prev value instead.
        final float partialRotation = MathHelper.degreesDifferenceAbs(entity.prevRotationYaw, entity.rotationYaw) * partialTicks;
        final float rotation = MathHelper.approachDegrees(entity.prevRotationYaw, entity.rotationYaw, partialRotation);
        matrixStack.rotate(Vector3f.YN.rotationDegrees(rotation));

        model.setRotationAngles(entity, 0, 0, 0, 0, 0);

        final IVertexBuilder builder = buffer.getBuffer(model.getRenderType(getEntityTexture(entity)));
        model.render(matrixStack, builder, packedLight, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);

        matrixStack.pop();

        final RayTraceResult hit = Minecraft.getInstance().objectMouseOver;
        if (hit instanceof EntityRayTraceResult && entity == ((EntityRayTraceResult) hit).getEntity()) {
            super.renderName(entity, new StringTextComponent("hi"), matrixStack, buffer, packedLight);
        }
    }

    ///////////////////////////////////////////////////////////////////

    private static final class RobotModel extends EntityModel<RobotEntity> {
        private final ModelRenderer topRenderer;
        private final ModelRenderer baseRenderer;
        private final ModelRenderer coreRenderer;
        private float baseY, topY;
        private final float[] topRotation = new float[3];

        public RobotModel() {
            topRenderer = new ModelRenderer(this, 1, 1)
                    .setTextureSize(64, 64)
                    .addBox(-7, 8, -7, 14, 6, 14);
            baseRenderer = new ModelRenderer(this, 1, 23)
                    .setTextureSize(64, 64)
                    .addBox(-7, 0, -7, 14, 7, 14);
            coreRenderer = new ModelRenderer(this, 1, 34)
                    .setTextureSize(64, 64)
                    .addBox(-6, 7, -6, 12, 1, 12);
        }

        @Override
        public void setRotationAngles(final RobotEntity entity, final float limbSwing, final float limbSwingAmount, final float ageInTicks, final float netHeadYaw, final float headPitch) {
            final RobotEntity.AnimationState state = entity.getAnimationState();
            baseY = state.baseRenderOffsetY;
            topY = state.topRenderOffsetY;
            topRotation[1] = state.topRenderRotationY;
        }

        @Override
        public void render(final MatrixStack matrixStack, final IVertexBuilder buffer, final int packedLight, final int packedOverlay, final float red, final float green, final float blue, final float alpha) {
            matrixStack.push();
            matrixStack.translate(0, topY, 0);
            matrixStack.rotate(TransformationHelper.quatFromXYZ(topRotation, true));
            topRenderer.render(matrixStack, buffer, packedLight, packedOverlay);
            matrixStack.pop();

            matrixStack.push();
            matrixStack.translate(0, baseY, 0);
            baseRenderer.render(matrixStack, buffer, packedLight, packedOverlay);
            coreRenderer.render(matrixStack, buffer, LightTexture.packLight(15, 15), packedOverlay);
            matrixStack.pop();
        }
    }
}
