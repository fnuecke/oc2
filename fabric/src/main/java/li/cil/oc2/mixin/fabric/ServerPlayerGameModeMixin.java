/* SPDX-License-Identifier: MIT */

package li.cil.oc2.mixin.fabric;

import li.cil.oc2.common.fabric.WrenchInteractionFabric;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Matches NeoForge's {@code doesSneakBypassUse}.
 */
@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {
    @Redirect(
        method = "useItemOn",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;isSecondaryUseActive()Z")
    )
    private boolean letWrenchReachBlockInteraction(final ServerPlayer player) {
        return player.isSecondaryUseActive() && !WrenchInteractionFabric.isHoldingWrench(player);
    }
}
