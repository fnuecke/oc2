/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.mixin;

import li.cil.oc2.client.renderer.ProjectorDepthRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    private static final String LEVEL_RENDERER_RENDER_LEVEL = "Lnet/minecraft/client/renderer/LevelRenderer;renderLevel(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;Z)V";

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = LEVEL_RENDERER_RENDER_LEVEL))
    private void onBeforeLevel(final DeltaTracker deltaTracker, final CallbackInfo ci) {
        ProjectorDepthRenderer.onBeforeLevel(deltaTracker);
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = LEVEL_RENDERER_RENDER_LEVEL, shift = At.Shift.AFTER))
    private void onAfterLevel(final DeltaTracker deltaTracker, final CallbackInfo ci) {
        ProjectorDepthRenderer.onAfterLevel();
    }
}
