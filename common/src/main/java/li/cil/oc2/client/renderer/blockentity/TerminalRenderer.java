/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import li.cil.oc2.client.renderer.Overlays;
import li.cil.oc2.client.renderer.TerminalOverlayRenderer;
import li.cil.oc2.common.block.OrientableBlock;
import li.cil.oc2.common.blockentity.TerminalBlockEntity;
import li.cil.oc2.common.vm.device.Terminal;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public final class TerminalRenderer implements BlockEntityRenderer<TerminalBlockEntity, TerminalRenderer.State> {
    public static final class State extends BlockEntityRenderState {
        private Terminal terminal;
        private boolean isConnected;
        private boolean hasRecentFrameError;
    }

    // --------------------------------------------------------------------- //

    public TerminalRenderer(final BlockEntityRendererProvider.Context ignoredContext) {
    }

    // --------------------------------------------------------------------- //

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(final TerminalBlockEntity terminal, final State state, final float partialTicks, final Vec3 cameraPosition,
                                   @Nullable final ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(terminal, state, partialTicks, cameraPosition, crumbling);

        state.terminal = terminal.getTerminal();
        state.isConnected = terminal.isConnected();
        state.hasRecentFrameError = terminal.hasRecentFrameError();
    }

    @Override
    public void submit(final State state, final PoseStack stack, final SubmitNodeCollector collector, final CameraRenderState camera) {
        final BlockState blockState = state.blockState;
        final Direction blockFacing = blockState.getValue(OrientableBlock.FACING);
        final Vec3 cameraOffset = TerminalOverlayRenderer.getCameraOffset(stack);

        final double projectedCameraPosition = cameraOffset.dot(Vec3.atLowerCornerOf(blockFacing.getUnitVec3i()));
        if (projectedCameraPosition <= 0) {
            return;
        }

        stack.pushPose();

        // Align with front face of block.
        stack.translate(0.5f, 0.5f, 0.5f);
        stack.mulPose(Axis.YN.rotationDegrees(OrientableBlock.getRotationY(blockState)));
        stack.mulPose(Axis.XN.rotationDegrees(OrientableBlock.getRotationX(blockState)));
        stack.translate(-0.5f, -0.5f, -0.5f);

        // Flip and align with top left corner.
        stack.translate(1, 1, 0);
        stack.scale(-1, -1, -1);

        // Scale to make 1/16th of the block one unit and align with top left of terminal area.
        final float pixelScale = 1 / 16f;
        stack.scale(pixelScale, pixelScale, pixelScale);

        stack.translate(0, 0, -10f);
        TerminalOverlayRenderer.renderTerminal(state.terminal, stack, collector, cameraOffset);

        stack.translate(0, 0, -0.1f);

        if (state.isConnected) {
            Overlays.renderPower(stack, collector);
        }

        if (state.hasRecentFrameError) {
            Overlays.renderStatus(stack, collector);
        }

        stack.popPose();
    }
}
