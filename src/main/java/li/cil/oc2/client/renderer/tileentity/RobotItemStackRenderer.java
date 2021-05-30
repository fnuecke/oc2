package li.cil.oc2.client.renderer.tileentity;

import com.mojang.blaze3d.matrix.PoseStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import li.cil.oc2.client.renderer.entity.model.RobotModel;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.tileentity.ItemStackBlockEntityRenderer;
import net.minecraft.item.ItemStack;

public final class RobotItemStackRenderer extends ItemStackBlockEntityRenderer {
    private final RobotModel model = new RobotModel();

    ///////////////////////////////////////////////////////////////////

    @Override
    public void renderByItem(final ItemStack stack, final ItemCameraTransforms.TransformType transformType, final PoseStack matrixStack, final IRenderTypeBuffer buffer, final int combinedLight, final int combinedOverlay) {
        matrixStack.pushPose();

        matrixStack.translate(0.5, 0, 0.5);

        final IVertexBuilder builder = buffer.getBuffer(model.renderType(RobotModel.ROBOT_ENTITY_TEXTURE));
        model.renderToBuffer(matrixStack, builder, combinedLight, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);

        matrixStack.popPose();
    }
}
