/* SPDX-License-Identifier: MIT */

package li.cil.oc2.mixin.fabric;

import li.cil.oc2.client.renderer.ProjectorDepthRenderer;
import net.minecraft.client.renderer.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Matches NeoForge's {@code ViewportEvent.RenderFog}.
 */
@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {
    @Inject(method = "setupFog", at = @At("TAIL"))
    private static void disableFogWhileRenderingProjectorDepth(final CallbackInfo ci) {
        ProjectorDepthRenderer.handleFog();
    }
}
