/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.mixin;

import li.cil.oc2.client.renderer.ProjectorDepthRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Only applies to the vanilla cullTerrain. Bumped prio allows us to skip it when e.g. Sodium overwrites it.
 */
@Mixin(value = LevelRenderer.class, priority = 1100)
public abstract class LevelRendererVanillaTerrainMixin {
    @Shadow
    private double prevCamRotX;

    @Shadow
    protected abstract void applyFrustum(Frustum frustum);

    /**
     * Only do the minimal work to avoid kicking of an async rebuild that delays regular camera rebuilds.
     */
    @Inject(method = "cullTerrain", cancellable = true, require = 0, at = @At(value = "INVOKE_STRING",
        target = "Lnet/minecraft/util/profiling/ProfilerFiller;push(Ljava/lang/String;)V", args = "ldc=repositionCamera"))
    private void onlyApplyFrustumForProjectorDepth(final Camera camera, final Frustum frustum, final boolean spectator, final CallbackInfo ci) {
        if (ProjectorDepthRenderer.isRenderingProjectorDepth()) {
            applyFrustum(frustum);
            // Makes the player's cullTerrain re-apply its own frustum.
            prevCamRotX = Double.MIN_VALUE;
            ci.cancel();
        }
    }
}
