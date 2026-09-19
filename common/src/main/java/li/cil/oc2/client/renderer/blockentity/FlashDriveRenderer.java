/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import li.cil.oc2.common.block.OrientableBlock;
import li.cil.oc2.common.blockentity.FlashDriveBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public final class FlashDriveRenderer implements BlockEntityRenderer<FlashDriveBlockEntity> {
    private static final float FLASH_MEMORY_Y = 10.5f / 16f;
    private static final float FLASH_MEMORY_Z = 9.0f / 16f;
    private static final float FLASH_MEMORY_SCALE = 6.0f / 16f;

    // --------------------------------------------------------------------- //

    private final BlockEntityRenderDispatcher renderer;

    // --------------------------------------------------------------------- //

    public FlashDriveRenderer(final BlockEntityRendererProvider.Context context) {
        this.renderer = context.getBlockEntityRenderDispatcher();
    }

    // --------------------------------------------------------------------- //

    @Override
    public void render(final FlashDriveBlockEntity flashDrive, final float partialTicks, final PoseStack stack, final MultiBufferSource bufferSource, final int light, final int overlay) {
        final ItemStack flashMemory = flashDrive.getMedia();
        if (flashMemory.isEmpty()) {
            return;
        }

        final BlockState state = flashDrive.getBlockState();
        final Direction facing = state.getValue(OrientableBlock.FACING);
        final int neighborLight = LevelRenderer.getLightColor(renderer.level, flashDrive.getBlockPos().relative(facing));
        final ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();

        stack.pushPose();

        stack.translate(0.5f, 0.5f, 0.5f);
        stack.mulPose(Axis.YN.rotationDegrees(OrientableBlock.getRotationY(state)));
        stack.mulPose(Axis.XN.rotationDegrees(OrientableBlock.getRotationX(state)));
        stack.translate(-0.5f, -0.5f, -0.5f);

        stack.translate(0.5f, FLASH_MEMORY_Y, FLASH_MEMORY_Z);
        stack.mulPose(Axis.XP.rotationDegrees(90));
        stack.scale(FLASH_MEMORY_SCALE, FLASH_MEMORY_SCALE, FLASH_MEMORY_SCALE);

        itemRenderer.renderStatic(flashMemory, ItemDisplayContext.FIXED, neighborLight, overlay, stack, bufferSource,
            flashDrive.getLevel(), (int) flashDrive.getBlockPos().asLong());

        stack.popPose();
    }
}
