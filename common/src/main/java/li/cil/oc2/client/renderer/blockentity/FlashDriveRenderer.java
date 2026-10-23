/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import li.cil.oc2.common.block.OrientableBlock;
import li.cil.oc2.common.blockentity.FlashDriveBlockEntity;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public final class FlashDriveRenderer implements BlockEntityRenderer<FlashDriveBlockEntity, MediaRenderState> {
    private static final float FLASH_MEMORY_Y = 10.5f / 16f;
    private static final float FLASH_MEMORY_Z = 9.0f / 16f;
    private static final float FLASH_MEMORY_SCALE = 6.0f / 16f;

    // --------------------------------------------------------------------- //

    private final ItemModelResolver itemModelResolver;

    // --------------------------------------------------------------------- //

    public FlashDriveRenderer(final BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    // --------------------------------------------------------------------- //

    @Override
    public MediaRenderState createRenderState() {
        return new MediaRenderState();
    }

    @Override
    public void extractRenderState(final FlashDriveBlockEntity flashDrive, final MediaRenderState state, final float partialTicks,
                                   final Vec3 cameraPosition, @Nullable final ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(flashDrive, state, partialTicks, cameraPosition, crumbling);

        final Direction facing = state.blockState.getValue(OrientableBlock.FACING);
        state.neighborLight = flashDrive.getLevel() != null
            ? LevelRenderer.getLightColor(flashDrive.getLevel(), flashDrive.getBlockPos().relative(facing))
            : state.lightCoords;
        itemModelResolver.updateForTopItem(state.media, flashDrive.getMedia(), ItemDisplayContext.FIXED,
            flashDrive.getLevel(), null, (int) flashDrive.getBlockPos().asLong());
    }

    @Override
    public void submit(final MediaRenderState state, final PoseStack stack, final SubmitNodeCollector collector, final CameraRenderState camera) {
        if (state.media.isEmpty()) {
            return;
        }

        final BlockState blockState = state.blockState;

        stack.pushPose();

        stack.translate(0.5f, 0.5f, 0.5f);
        stack.mulPose(Axis.YN.rotationDegrees(OrientableBlock.getRotationY(blockState)));
        stack.mulPose(Axis.XN.rotationDegrees(OrientableBlock.getRotationX(blockState)));
        stack.translate(-0.5f, -0.5f, -0.5f);

        stack.translate(0.5f, FLASH_MEMORY_Y, FLASH_MEMORY_Z);
        stack.mulPose(Axis.XP.rotationDegrees(90));
        stack.scale(FLASH_MEMORY_SCALE, FLASH_MEMORY_SCALE, FLASH_MEMORY_SCALE);

        state.media.submit(stack, collector, state.neighborLight, OverlayTexture.NO_OVERLAY, 0);

        stack.popPose();
    }
}
