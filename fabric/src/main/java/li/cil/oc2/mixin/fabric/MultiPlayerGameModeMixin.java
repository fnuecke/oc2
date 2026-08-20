/* SPDX-License-Identifier: MIT */

package li.cil.oc2.mixin.fabric;

import li.cil.oc2.common.fabric.WrenchInteractionFabric;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {
    @Redirect(
            method = "performUseItemOn",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isSecondaryUseActive()Z")
    )
    private boolean letWrenchReachBlockInteraction(final LocalPlayer player) {
        return player.isSecondaryUseActive() && !WrenchInteractionFabric.isHoldingWrench(player);
    }
}
