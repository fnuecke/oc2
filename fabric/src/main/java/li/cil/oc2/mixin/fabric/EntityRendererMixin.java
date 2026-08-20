/* SPDX-License-Identifier: MIT */

package li.cil.oc2.mixin.fabric;

import li.cil.oc2.client.renderer.ProjectorDepthRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Matches NeoForge's {@code RenderNameTagEvent}.
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
    @Inject(method = "renderNameTag", at = @At("HEAD"), cancellable = true)
    private void skipNameTagsWhileRenderingProjectorDepth(final CallbackInfo ci) {
        if (ProjectorDepthRenderer.shouldSuppressNameplates()) {
            ci.cancel();
        }
    }
}
