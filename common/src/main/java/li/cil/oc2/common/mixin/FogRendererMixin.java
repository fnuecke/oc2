/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.mixin;

import li.cil.oc2.client.renderer.ProjectorDepthRenderer;
import net.minecraft.client.renderer.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {
    @Inject(method = "setupColor", at = @At("HEAD"), cancellable = true)
    private static void skipFogColorSetup(final CallbackInfo ci) {
        if (ProjectorDepthRenderer.isIsRenderingProjectorDepth()) {
            ci.cancel();
        }
    }
}
