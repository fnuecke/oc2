/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.renderer.blockentity;

import com.mojang.math.Axis;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Vector3f;
import li.cil.oc2.common.block.DiskDriveBlock;
import li.cil.oc2.common.blockentity.DiskDriveBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

public final class DiskDriveRenderer implements BlockEntityRenderer<DiskDriveBlockEntity> {
    private final BlockEntityRenderDispatcher renderer;

    ///////////////////////////////////////////////////////////////////

    public DiskDriveRenderer(final BlockEntityRendererProvider.Context context) {
        this.renderer = context.getBlockEntityRenderDispatcher();
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void render(final DiskDriveBlockEntity diskDrive, final float partialTicks, final PoseStack stack, final MultiBufferSource bufferSource, final int light, final int overlay) {
        final ItemStack floppy = diskDrive.getFloppy();
        final Direction blockFacing = diskDrive.getBlockState().getValue(DiskDriveBlock.FACING);
        final int neighborLight = LevelRenderer.getLightColor(renderer.level, diskDrive.getBlockPos().relative(blockFacing));
        final ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();

        stack.pushPose();

        stack.translate(0.5f, 0.5f, 0.5f);
        stack.mulPose(Axis.YN.rotationDegrees(blockFacing.toYRot()));
        stack.translate(0.0f, 0.0f, 0.5f);
        stack.mulPose(Axis.XN.rotationDegrees(90));
        stack.translate(0.0f, 0.2375f, 2.5f / 16f);
        stack.scale(0.55f, 0.55f, 0.55f);

        itemRenderer.renderStatic(floppy, ItemDisplayContext.FIXED, neighborLight, overlay, stack, bufferSource,
            diskDrive.getLevel(), (int) diskDrive.getBlockPos().asLong());

        stack.popPose();
    }
}
