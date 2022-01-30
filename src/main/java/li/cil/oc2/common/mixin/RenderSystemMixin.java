package li.cil.oc2.common.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import li.cil.oc2.client.renderer.ProjectorDepthRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderSystem.class)
public abstract class RenderSystemMixin {
    @Inject(method = "getShader", at = @At("HEAD"), cancellable = true)
    private static void overrideShader(final CallbackInfoReturnable<ShaderInstance> cir) {
        if (ProjectorDepthRenderer.isIsRenderingProjectorDepth()) {
            cir.setReturnValue(GameRenderer.getPositionShader());
        }
    }
}
