/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import li.cil.oc2.client.renderer.Overlays;
import li.cil.oc2.client.renderer.TerminalOverlayRenderer;
import li.cil.oc2.common.block.ComputerBlock;
import li.cil.oc2.common.blockentity.ComputerBlockEntity;
import li.cil.oc2.common.bus.CommonDeviceBusController;
import li.cil.oc2.common.vm.VMRunState;
import li.cil.oc2.common.vm.VirtualMachine;
import li.cil.oc2.common.vm.device.Terminal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import javax.annotation.Nullable;
import java.util.List;

public final class ComputerRenderer implements BlockEntityRenderer<ComputerBlockEntity, ComputerRenderer.State> {
    public static final class State extends BlockEntityRenderState {
        private Terminal terminal;
        private VirtualMachine virtualMachine;
        private CommonDeviceBusController.BusState busState;
        private VMRunState runState;
        private boolean isRunning;
    }

    // --------------------------------------------------------------------- //

    public ComputerRenderer(final BlockEntityRendererProvider.Context ignoredContext) {
    }

    // --------------------------------------------------------------------- //

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(final ComputerBlockEntity computer, final State state, final float partialTicks, final Vec3 cameraPosition,
                                   @Nullable final ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(computer, state, partialTicks, cameraPosition, crumbling);

        final VirtualMachine virtualMachine = computer.getVirtualMachine();
        state.terminal = computer.getTerminal();
        state.virtualMachine = virtualMachine;
        state.busState = virtualMachine.getBusState();
        state.runState = virtualMachine.getRunState();
        state.isRunning = virtualMachine.isRunning();
    }

    @Override
    public void submit(final State state, final PoseStack stack, final SubmitNodeCollector collector, final CameraRenderState camera) {
        final Direction blockFacing = state.blockState.getValue(ComputerBlock.FACING);
        final Vec3 cameraOffset = TerminalOverlayRenderer.getCameraOffset(stack);

        // If viewer is not in front of the block we can skip the rest, it cannot be visible.
        // We check against the center of the block instead of the actual relevant face for simplicity.
        final double projectedCameraPosition = cameraOffset.dot(Vec3.atLowerCornerOf(blockFacing.getUnitVec3i()));
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

        if (state.isRunning) {
            TerminalOverlayRenderer.renderTerminal(state.terminal, stack, collector, cameraOffset);
        } else {
            renderStatusText(state, stack, collector, cameraOffset);
        }

        stack.translate(0, 0, -0.1f);

        switch (state.busState) {
            case SCAN_PENDING:
            case INCOMPLETE:
                Overlays.renderStatus(stack, collector);
                break;
            case TOO_COMPLEX:
                Overlays.renderStatus(stack, collector, 1000);
                break;
            case MULTIPLE_CONTROLLERS:
                Overlays.renderStatus(stack, collector, 250);
                break;
            case READY:
                switch (state.runState) {
                    case STOPPED:
                        break;
                    case LOADING_DEVICES:
                        Overlays.renderStatus(stack, collector);
                        break;
                    case RUNNING:
                        Overlays.renderPower(stack, collector);
                        break;
                }
                break;
        }

        stack.popPose();
    }

    // --------------------------------------------------------------------- //

    private void renderStatusText(final State state, final PoseStack stack, final SubmitNodeCollector collector, final Vec3 cameraOffset) {
        if (cameraOffset.lengthSqr() >= 12 * 12) {
            return;
        }

        final Component bootError = state.virtualMachine.getError();
        if (bootError == null) {
            return;
        }

        stack.pushPose();
        stack.translate(3, 3, -0.9f);

        drawText(stack, collector, bootError);

        stack.popPose();
    }

    private void drawText(final PoseStack stack, final SubmitNodeCollector collector, final Component text) {
        final int maxWidth = 100;

        stack.pushPose();
        stack.scale(10f / maxWidth, 10f / maxWidth, 10f / maxWidth);

        final Font fontRenderer = Minecraft.getInstance().font;
        final List<FormattedText> wrappedText = fontRenderer.getSplitter().splitLines(text, maxWidth, Style.EMPTY);
        if (wrappedText.size() == 1) {
            final int textWidth = fontRenderer.width(text);
            drawTextLine(collector, stack, text.getString(), (maxWidth - textWidth) * 0.5f, 0);
        } else {
            for (int i = 0; i < wrappedText.size(); i++) {
                drawTextLine(collector, stack, wrappedText.get(i).getString(), 0, i * fontRenderer.lineHeight);
            }
        }

        stack.popPose();
    }

    private static void drawTextLine(final SubmitNodeCollector collector, final PoseStack stack, final String text, final float x, final float y) {
        collector.submitText(stack, x, y, Language.getInstance().getVisualOrder(FormattedText.of(text)), false,
            Font.DisplayMode.NORMAL, LightTexture.pack(15, 15), 0xFFEE3322, 0, 0);
    }
}
