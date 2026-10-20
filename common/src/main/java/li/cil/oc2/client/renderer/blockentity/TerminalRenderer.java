/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import li.cil.oc2.client.renderer.Overlays;
import li.cil.oc2.client.renderer.TerminalOverlayRenderer;
import li.cil.oc2.common.block.OrientableBlock;
import li.cil.oc2.common.blockentity.TerminalBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class TerminalRenderer implements BlockEntityRenderer<TerminalBlockEntity> {
    public TerminalRenderer(final BlockEntityRendererProvider.Context context) {
    }

    // --------------------------------------------------------------------- //

    @Override
    public void render(final TerminalBlockEntity terminal, final float partialTicks, final PoseStack stack, final MultiBufferSource bufferSource, final int light, final int overlay) {
        final BlockState state = terminal.getBlockState();
        final Direction blockFacing = state.getValue(OrientableBlock.FACING);
        final Vec3 cameraOffset = TerminalOverlayRenderer.getCameraOffset(stack);

        final double projectedCameraPosition = cameraOffset.dot(Vec3.atLowerCornerOf(blockFacing.getNormal()));
        if (projectedCameraPosition <= 0) {
            return;
        }

        stack.pushPose();

        // Align with front face of block.
        stack.translate(0.5f, 0.5f, 0.5f);
        stack.mulPose(Axis.YN.rotationDegrees(OrientableBlock.getRotationY(state)));
        stack.mulPose(Axis.XN.rotationDegrees(OrientableBlock.getRotationX(state)));
        stack.translate(-0.5f, -0.5f, -0.5f);

        // Flip and align with top left corner.
        stack.translate(1, 1, 0);
        stack.scale(-1, -1, -1);

        // Scale to make 1/16th of the block one unit and align with top left of terminal area.
        final float pixelScale = 1 / 16f;
        stack.scale(pixelScale, pixelScale, pixelScale);

        stack.translate(0, 0, -10f);
        TerminalOverlayRenderer.renderTerminal(terminal.getTerminal(), stack, bufferSource, cameraOffset);

        stack.translate(0, 0, -0.1f);

        if (terminal.isConnected()) {
            Overlays.renderPower(stack.last().pose(), bufferSource);
        }

        if (terminal.hasRecentFrameError()) {
            Overlays.renderStatus(stack.last().pose(), bufferSource);
        }

        stack.popPose();
    }
}
