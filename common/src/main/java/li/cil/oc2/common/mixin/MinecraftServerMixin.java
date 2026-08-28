/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.mixin;

import li.cil.oc2.common.serialization.BlobStorage;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @Inject(method = "saveEverything", at = @At("RETURN"))
    private void handleSavedEverything(final boolean suppressLog, final boolean flush, final boolean forced, final CallbackInfoReturnable<Boolean> cir) {
        BlobStorage.handleSaved();
    }
}
