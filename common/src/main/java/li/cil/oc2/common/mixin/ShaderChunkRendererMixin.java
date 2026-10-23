/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.mixin;

import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import li.cil.oc2.client.renderer.ProjectorDepthRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.ShaderChunkRenderer", remap = false)
public abstract class ShaderChunkRendererMixin {
    @Inject(method = "begin", at = @At("TAIL"))
    private void rebindProjectorDepthTarget(final CallbackInfo ci) {
        final RenderTarget target = ProjectorDepthRenderer.getActiveProjectorDepthTarget();
        if (target != null) {
            final GlTexture colorTexture = (GlTexture) target.getColorTexture();
            final GlDevice device = (GlDevice) RenderSystem.getDevice();
            GlStateManager._glBindFramebuffer(GlConst.GL_FRAMEBUFFER, colorTexture.getFbo(device.directStateAccess(), target.getDepthTexture()));
        }
    }
}
