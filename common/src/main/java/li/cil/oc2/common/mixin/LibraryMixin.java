/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.mixin;

import com.mojang.blaze3d.audio.Library;
import li.cil.oc2.client.audio.SoundCardAudio;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Library.class)
public abstract class LibraryMixin {
    @Inject(method = "init", at = @At("TAIL"))
    private void handleInit(final CallbackInfo ci) {
        SoundCardAudio.handleContextCreated();
    }

    @Inject(method = "cleanup", at = @At("HEAD"))
    private void handleCleanup(final CallbackInfo ci) {
        SoundCardAudio.handleContextDestroyed();
    }
}
