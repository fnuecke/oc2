package li.cil.oc2.common.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import li.cil.oc2.client.renderer.ProjectorDepthRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Inject(method = {"shouldShowEntityOutlines"}, at = @At("HEAD"), cancellable = true)
    private void skipOutlines(final CallbackInfoReturnable<Boolean> cir) {
        if (ProjectorDepthRenderer.isIsRenderingProjectorDepth()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = {"renderSky", "renderClouds", "renderDebug"}, at = @At("HEAD"), cancellable = true)
    private void skipExtraStuff(final CallbackInfo ci) {
        if (ProjectorDepthRenderer.isIsRenderingProjectorDepth()) {
            ci.cancel();
        }
    }

    @Inject(method = {"entityTarget", "getItemEntityTarget", "getParticlesTarget"}, at = @At("HEAD"), cancellable = true)
    private void redirectToMainTarget(final CallbackInfoReturnable<RenderTarget> cir) {
        if (ProjectorDepthRenderer.isIsRenderingProjectorDepth()) {
            cir.setReturnValue(Minecraft.getInstance().getMainRenderTarget());
        }
    }
}
