/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import li.cil.oc2.common.vm.device.Terminal;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class TerminalOverlayRenderer {
    // From the pose instead of world positions, so it also works for blocks on moving contraptions for example.
    public static Vec3 getCameraOffset(final PoseStack stack) {
        final Vector3f camera = new Matrix4f(stack.last().pose()).invertAffine().transformPosition(new Vector3f());
        return new Vec3(camera.x() - 0.5, camera.y() - 0.5, camera.z() - 0.5);
    }

    public static void renderTerminal(final Terminal terminal, final PoseStack stack, final MultiBufferSource bufferSource, final Vec3 cameraOffset) {
        // Render terminal content if close enough.
        if (cameraOffset.lengthSqr() < 6 * 6) {
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
