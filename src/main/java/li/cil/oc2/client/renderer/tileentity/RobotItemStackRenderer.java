package li.cil.oc2.client.renderer.tileentity;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import li.cil.oc2.client.renderer.entity.model.RobotModel;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.tileentity.ItemStackTileEntityRenderer;
import net.minecraft.item.ItemStack;

public final class RobotItemStackRenderer extends ItemStackTileEntityRenderer {
    private final RobotModel model = new RobotModel();

    ///////////////////////////////////////////////////////////////////

    @Override
    public void func_239207_a_(final ItemStack stack, final ItemCameraTransforms.TransformType transformType, final MatrixStack matrixStack, final IRenderTypeBuffer buffer, final int combinedLight, final int combinedOverlay) {
        matrixStack.push();

        matrixStack.translate(0.5, 0, 0.5);

        final IVertexBuilder builder = buffer.getBuffer(model.getRenderType(RobotModel.ROBOT_ENTITY_TEXTURE));
        model.render(matrixStack, builder, combinedLight, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);

        matrixStack.pop();
    }
}
