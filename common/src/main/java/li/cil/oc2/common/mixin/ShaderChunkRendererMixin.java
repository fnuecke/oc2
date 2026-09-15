/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.mixin;

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
        ProjectorDepthRenderer.bindProjectorDepthTarget();
    }
}
