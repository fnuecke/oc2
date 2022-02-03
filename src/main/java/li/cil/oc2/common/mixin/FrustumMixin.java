/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.mixin;

import li.cil.oc2.client.renderer.ProjectorDepthRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Frustum.class)
public abstract class FrustumMixin {
    /**
     * Skip offsetting the frustum to fully contain camera cube, since we have a very
     * tight frustum when rendering projector depth; so tight that the cube may never
     * fit inside, which would then leave to an endless loop (would shift out of bounds
     * and never get back in).
     */
    @Inject(method = "offsetToFullyIncludeCameraCube", at = @At("HEAD"), cancellable = true)
    private void skipOffset(final CallbackInfoReturnable<Frustum> ci) {
        if (ProjectorDepthRenderer.isIsRenderingProjectorDepth()) {
            ci.setReturnValue((Frustum) (Object) this);
        }
    }
}
