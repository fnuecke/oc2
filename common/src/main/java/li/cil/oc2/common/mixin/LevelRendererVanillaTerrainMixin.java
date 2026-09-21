/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.mixin;

import li.cil.oc2.client.renderer.ProjectorDepthRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Only applies to the vanilla setupRender. Bumped prio allows us to skip it when e.g. Sodium overwrites it.
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
    @Inject(method = "setupRender", cancellable = true, require = 0, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher;setCamera(Lnet/minecraft/world/phys/Vec3;)V"))
    private void onlyApplyFrustumForProjectorDepth(final Camera camera, final Frustum frustum, final boolean hasCapturedFrustum, final boolean isSpectator, final CallbackInfo ci) {
        if (ProjectorDepthRenderer.isRenderingProjectorDepth()) {
            applyFrustum(LevelRenderer.offsetFrustum(frustum));
            // Makes the player's setupRender re-apply its own frustum.
            prevCamRotX = Double.MIN_VALUE;
            // Balances the push("camera") before the canceled call.
            Minecraft.getInstance().getProfiler().pop();
            ci.cancel();
        }
    }
}
