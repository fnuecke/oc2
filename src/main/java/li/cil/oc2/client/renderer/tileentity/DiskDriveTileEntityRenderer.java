package li.cil.oc2.client.renderer.tileentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import li.cil.oc2.common.block.DiskDriveBlock;
import li.cil.oc2.common.tileentity.DiskDriveTileEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

public final class DiskDriveTileEntityRenderer implements BlockEntityRenderer<DiskDriveTileEntity> {
    private final BlockEntityRenderDispatcher renderer;

    ///////////////////////////////////////////////////////////////////

    public DiskDriveTileEntityRenderer(final BlockEntityRendererProvider.Context context) {
        this.renderer = context.getBlockEntityRenderDispatcher();
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void render(final DiskDriveTileEntity tileEntity, final float partialTicks, final PoseStack matrixStack, final MultiBufferSource buffer, final int light, final int overlay) {
        final ItemStack stack = tileEntity.getFloppy();
        final Direction blockFacing = tileEntity.getBlockState().getValue(DiskDriveBlock.FACING);
        final int neighborLight = LevelRenderer.getLightColor(renderer.level, tileEntity.getBlockPos().relative(blockFacing));
        final ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();

        matrixStack.pushPose();

        matrixStack.translate(0.5f, 0.5f, 0.5f);
        matrixStack.mulPose(Vector3f.YN.rotationDegrees(blockFacing.toYRot()));
        matrixStack.translate(0.0f, 0.0f, 0.5f);
        matrixStack.mulPose(Vector3f.XN.rotationDegrees(90));
        matrixStack.translate(0.0f, 0.2375f, 2.5f / 16f);
        matrixStack.scale(0.55f, 0.55f, 0.55f);

        itemRenderer.renderStatic(stack, ItemTransforms.TransformType.FIXED, neighborLight, overlay, matrixStack, buffer, (int) tileEntity.getBlockPos().asLong());

        matrixStack.popPose();
    }
}
