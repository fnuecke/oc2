package li.cil.oc2.common.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
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

    @Inject(method = "getMainRenderTarget", at = @At(value = "HEAD"), cancellable = true)
    private void getMainRenderTargetOverride(final CallbackInfoReturnable<RenderTarget> cir) {
        if (mainRenderTargetOverride != null) {
            cir.setReturnValue(mainRenderTargetOverride);
        }
    }
}
