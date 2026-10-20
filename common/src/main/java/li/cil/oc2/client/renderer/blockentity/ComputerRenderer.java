/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import li.cil.oc2.client.renderer.Overlays;
import li.cil.oc2.client.renderer.TerminalOverlayRenderer;
import li.cil.oc2.common.block.ComputerBlock;
import li.cil.oc2.common.blockentity.ComputerBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.List;

public final class ComputerRenderer implements BlockEntityRenderer<ComputerBlockEntity> {
    public ComputerRenderer(final BlockEntityRendererProvider.Context context) {
    }

    // --------------------------------------------------------------------- //

    @Override
    public void render(final ComputerBlockEntity computer, final float partialTicks, final PoseStack stack, final MultiBufferSource bufferSource, final int light, final int overlay) {
        final Direction blockFacing = computer.getBlockState().getValue(ComputerBlock.FACING);
        final Vec3 cameraOffset = TerminalOverlayRenderer.getCameraOffset(stack);

        // If viewer is not in front of the block we can skip the rest, it cannot be visible.
        // We check against the center of the block instead of the actual relevant face for simplicity.
        final double projectedCameraPosition = cameraOffset.dot(Vec3.atLowerCornerOf(blockFacing.getNormal()));
        if (projectedCameraPosition <= 0) {
            return;
        }

        stack.pushPose();

        // Align with front face of block.
        final Quaternionf rotation = Axis.YN.rotationDegrees(blockFacing.toYRot() + 180);
        stack.translate(0.5f, 0, 0.5f);
        stack.mulPose(rotation);
        stack.translate(-0.5f, 0, -0.5f);

        // Flip and align with top left corner.
        stack.translate(1, 1, 0);
        stack.scale(-1, -1, -1);

        // Scale to make 1/16th of the block one unit and align with top left of terminal area.
        final float pixelScale = 1 / 16f;
        stack.scale(pixelScale, pixelScale, pixelScale);

        if (computer.getVirtualMachine().isRunning()) {
            TerminalOverlayRenderer.renderTerminal(computer.getTerminal(), stack, bufferSource, cameraOffset);
        } else {
            renderStatusText(computer, stack, bufferSource, cameraOffset);
        }

        stack.translate(0, 0, -0.1f);
        final Matrix4f matrix = stack.last().pose();

        switch (computer.getVirtualMachine().getBusState()) {
            case SCAN_PENDING:
            case INCOMPLETE:
                Overlays.renderStatus(matrix, bufferSource);
                break;
            case TOO_COMPLEX:
                Overlays.renderStatus(matrix, bufferSource, 1000);
                break;
            case MULTIPLE_CONTROLLERS:
                Overlays.renderStatus(matrix, bufferSource, 250);
                break;
            case READY:
                switch (computer.getVirtualMachine().getRunState()) {
                    case STOPPED:
                        break;
                    case LOADING_DEVICES:
                        Overlays.renderStatus(matrix, bufferSource);
                        break;
                    case RUNNING:
                        Overlays.renderPower(matrix, bufferSource);
                        break;
                }
                break;
        }

        stack.popPose();
    }

    // --------------------------------------------------------------------- //

    private void renderStatusText(final ComputerBlockEntity computer, final PoseStack stack, final MultiBufferSource bufferSource, final Vec3 cameraOffset) {
        if (cameraOffset.lengthSqr() >= 12 * 12) {
            return;
        }

        final Component bootError = computer.getVirtualMachine().getError();
        if (bootError == null) {
            return;
        }

        stack.pushPose();
        stack.translate(3, 3, -0.9f);

        drawText(stack, bufferSource, bootError);

        stack.popPose();
    }

    private void drawText(final PoseStack stack, final MultiBufferSource bufferSource, final Component text) {
        final int maxWidth = 100;

        stack.pushPose();
        stack.scale(10f / maxWidth, 10f / maxWidth, 10f / maxWidth);

        final Font fontRenderer = Minecraft.getInstance().font;
        final Matrix4f matrix = stack.last().pose();
        final List<FormattedText> wrappedText = fontRenderer.getSplitter().splitLines(text, maxWidth, Style.EMPTY);
        if (wrappedText.size() == 1) {
            final int textWidth = fontRenderer.width(text);
            drawTextLine(fontRenderer, text.getString(), (maxWidth - textWidth) * 0.5f, 0, matrix, bufferSource);
        } else {
            for (int i = 0; i < wrappedText.size(); i++) {
                drawTextLine(fontRenderer, wrappedText.get(i).getString(), 0, i * fontRenderer.lineHeight, matrix, bufferSource);
            }
        }

        stack.popPose();
    }

    private static void drawTextLine(final Font font, final String text, final float x, final float y,
                                     final Matrix4f matrix, final MultiBufferSource bufferSource) {
        font.drawInBatch(text, x, y, 0xFFEE3322, false, matrix, bufferSource,
            Font.DisplayMode.NORMAL, 0, LightTexture.pack(15, 15));
    }
}
