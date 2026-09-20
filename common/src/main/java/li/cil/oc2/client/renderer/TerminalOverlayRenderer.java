/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import li.cil.oc2.common.vm.Terminal;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public class TerminalOverlayRenderer {
    public static void renderTerminal(final BlockPos blockPos, final Terminal terminal, final PoseStack stack, final MultiBufferSource bufferSource, final Vec3 cameraPosition) {
        // Render terminal content if close enough.
        if (Vec3.atCenterOf(blockPos).closerThan(cameraPosition, 6f)) {
            stack.pushPose();
            stack.translate(2, 2, -0.9f);

            // Scale to make terminal fit fully.
            final float textScaleX = 12f / terminal.getWidth();
            final float textScaleY = 7f / terminal.getHeight();
            final float scale = Math.min(textScaleX, textScaleY) * 0.95f;

            // Center it on both axes.
            final float scaleDeltaX = textScaleX - scale;
            final float scaleDeltaY = textScaleY - scale;
            stack.translate(
                terminal.getWidth() * scaleDeltaX * 0.5f,
                terminal.getHeight() * scaleDeltaY * 0.5f,
                0f);

            stack.scale(scale, scale, 1f);

            TerminalTextures.get(terminal).draw(stack);

            stack.popPose();
        } else {
            stack.pushPose();
            stack.translate(0, 0, -0.9f);

            Overlays.renderTerminal(stack.last().pose(), bufferSource);

            stack.popPose();
        }
    }
}
