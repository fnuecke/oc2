package li.cil.oc2.client.renderer.tileentity;

import com.mojang.blaze3d.matrix.MatrixStack;
import li.cil.oc2.common.block.DiskDriveBlock;
import li.cil.oc2.common.tileentity.DiskDriveTileEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.math.vector.Vector3f;

public final class DiskDriveTileEntityRenderer extends TileEntityRenderer<DiskDriveTileEntity> {
    public DiskDriveTileEntityRenderer(final TileEntityRendererDispatcher dispatcher) {
        super(dispatcher);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void render(final DiskDriveTileEntity tileEntity, final float partialTicks, final MatrixStack matrixStack, final IRenderTypeBuffer buffer, final int light, final int overlay) {
        final ItemStack stack = tileEntity.getFloppy();
        final Direction blockFacing = tileEntity.getBlockState().getValue(DiskDriveBlock.FACING);
        final int neighborLight = WorldRenderer.getLightColor(renderer.level, tileEntity.getBlockPos().relative(blockFacing));
        final ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();

        matrixStack.pushPose();

        matrixStack.translate(0.5f, 0.5f, 0.5f);
        matrixStack.mulPose(Vector3f.YN.rotationDegrees(blockFacing.toYRot()));
        matrixStack.translate(0.0f, 0.0f, 0.5f);
        matrixStack.mulPose(Vector3f.XN.rotationDegrees(90));
        matrixStack.translate(0.0f, 0.2375f, 2.5f / 16f);
        matrixStack.scale(0.55f, 0.55f, 0.55f);

        itemRenderer.renderStatic(stack, ItemCameraTransforms.TransformType.FIXED, neighborLight, overlay, matrixStack, buffer);

        matrixStack.popPose();
    }
}
