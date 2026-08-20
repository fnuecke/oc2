/* SPDX-License-Identifier: MIT */

package li.cil.oc2.mixin.fabric;

import li.cil.oc2.client.fabric.ClientPlatformImpl;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Matches canceling NeoForge's {@code VanillaGuiLayers.HOTBAR} layer.
 */
@Mixin(Gui.class)
public abstract class GuiMixin {
    @Inject(method = "renderItemHotbar", at = @At("HEAD"), cancellable = true)
    private void skipHiddenHotbar(final GuiGraphics graphics, final DeltaTracker deltaTracker, final CallbackInfo ci) {
        if (!ClientPlatformImpl.isHotbarVisible()) {
            ci.cancel();
        }
    }
}
