package li.cil.oc2.common.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import li.cil.oc2.client.renderer.ProjectorDepthRenderer;
import li.cil.oc2.common.ext.MinecraftExt;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin implements MinecraftExt {
    private RenderTarget mainRenderTargetOverride;

    @Override
    public void setMainRenderTargetOverride(@Nullable final RenderTarget renderTarget) {
        mainRenderTargetOverride = renderTarget;
    }

    /**
     * Redirect everything into the correct buffer while rendering projector depth.
     * <p>
     * Some things in level rendering may try to re-bind the main render target, so
     * we catch that and ensure we bind the projector depth buffer again.
     */
    @Inject(method = "getMainRenderTarget", at = @At("HEAD"), cancellable = true)
    private void getMainRenderTargetOverride(final CallbackInfoReturnable<RenderTarget> cir) {
        if (mainRenderTargetOverride != null) {
            cir.setReturnValue(mainRenderTargetOverride);
        }
    }

    /**
     * Avoid access to render targets that are null while rendering projector depth to skip some work.
     */
    @Inject(method = "useShaderTransparency", at = @At("HEAD"), cancellable = true)
    private static void noTransparencyWhileRenderingProjectorDepth(final CallbackInfoReturnable<Boolean> cir) {
        if (ProjectorDepthRenderer.isIsRenderingProjectorDepth()) {
            cir.setReturnValue(false);
        }
    }
}
