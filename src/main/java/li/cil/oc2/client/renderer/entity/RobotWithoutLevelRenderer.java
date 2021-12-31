package li.cil.oc2.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc2.client.renderer.entity.model.RobotModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemStack;

public final class RobotWithoutLevelRenderer extends BlockEntityWithoutLevelRenderer {
    private final RobotModel model;

    ///////////////////////////////////////////////////////////////////

    public RobotWithoutLevelRenderer(final BlockEntityRenderDispatcher dispatcher, final EntityModelSet modelSet) {
        super(dispatcher, modelSet);
        model = new RobotModel(modelSet.bakeLayer(RobotModel.ROBOT_MODEL_LAYER));
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void renderByItem(final ItemStack itemStack, final ItemTransforms.TransformType transformType, final PoseStack poseStack, final MultiBufferSource bufferSource, final int combinedLight, final int combinedOverlay) {
        poseStack.pushPose();

        poseStack.translate(0.5, 0, 0.5);

        final VertexConsumer consumer = bufferSource.getBuffer(model.renderType(RobotModel.ROBOT_ENTITY_TEXTURE));
        model.renderToBuffer(poseStack, consumer, combinedLight, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);

        poseStack.popPose();
    }
}
