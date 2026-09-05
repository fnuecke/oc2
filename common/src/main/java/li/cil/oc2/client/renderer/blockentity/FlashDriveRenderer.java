/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import li.cil.oc2.common.block.FlashDriveBlock;
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

public final class FlashDriveRenderer implements BlockEntityRenderer<FlashDriveBlockEntity> {
    private static final float CHIP_Z = 5.5f / 16f;
    private static final float CHIP_Y = 7.0f / 16f;
    private static final float CHIP_SCALE = 6.0f / 16f;

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

        final Direction facing = flashDrive.getBlockState().getValue(FlashDriveBlock.FACING);
        final int rotation = flashDrive.getBlockState().getValue(FlashDriveBlock.ROTATION);
        final int neighborLight = LevelRenderer.getLightColor(renderer.level, flashDrive.getBlockPos().relative(facing));
        final ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();

        stack.pushPose();

        stack.translate(0.5f, 0.5f, 0.5f);
        stack.mulPose(Axis.YN.rotationDegrees(getRotationY(facing, rotation)));
        stack.mulPose(Axis.XN.rotationDegrees(getRotationX(facing)));
        stack.translate(-0.5f, -0.5f, -0.5f);

        stack.translate(0.5f, CHIP_Y, CHIP_Z);
        stack.mulPose(Axis.ZP.rotationDegrees(180));
        stack.scale(CHIP_SCALE, CHIP_SCALE, CHIP_SCALE);

        itemRenderer.renderStatic(flashMemory, ItemDisplayContext.FIXED, neighborLight, overlay, stack, bufferSource,
            flashDrive.getLevel(), (int) flashDrive.getBlockPos().asLong());

        stack.popPose();
    }

    // --------------------------------------------------------------------- //

    public static int getRotationX(final Direction facing) {
        return switch (facing) {
            case UP -> 0;
            case DOWN -> 180;
            default -> 270;
        };
    }

    public static int getRotationY(final Direction facing, final int rotation) {
        return facing.getAxis().isVertical() ? rotation * 90 : (int) facing.toYRot();
    }
}
