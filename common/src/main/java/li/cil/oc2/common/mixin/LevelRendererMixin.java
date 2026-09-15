/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import li.cil.oc2.client.renderer.ProjectorDepthRenderer;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Final
    private RenderBuffers renderBuffers;

    @Inject(method = "renderLevel", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", args = {"ldc=destroyProgress"}))
    private void onBeforeTransparencyChain(final CallbackInfo ci) {
        ProjectorDepthRenderer.onBeforeTransparencyChain(renderBuffers.bufferSource());
    }

    @Inject(method = "renderSectionLayer", at = @At("HEAD"))
    private void onBeforeTranslucentTerrain(final RenderType renderType, final double cameraX, final double cameraY, final double cameraZ, final Matrix4f frustumMatrix, final Matrix4f projectionMatrix, final CallbackInfo ci) {
        if (renderType == RenderType.translucent()) {
            ProjectorDepthRenderer.onBeforeTranslucentTerrain(frustumMatrix, projectionMatrix, minecraft.getTimer());
        }
    }

    @Inject(method = "renderSnowAndRain", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;depthMask(Z)V", shift = At.Shift.AFTER, remap = false))
    private void enableDepthForWeatherInDepthBuffer(final CallbackInfo ci) {
        if (ProjectorDepthRenderer.isRenderingProjectorDepth()
            && minecraft.options.graphicsMode().get().getId() >= GraphicsStatus.FABULOUS.getId()) {
            RenderSystem.depthMask(true);
        }
    }

    @Inject(method = {"entityTarget", "getItemEntityTarget", "getWeatherTarget"}, at = @At("HEAD"), cancellable = true)
    private void redirectToMainTarget(final CallbackInfoReturnable<RenderTarget> cir) {
        if (ProjectorDepthRenderer.isRenderingProjectorDepth()) {
            cir.setReturnValue(Minecraft.getInstance().getMainRenderTarget());
        }
    }
}
