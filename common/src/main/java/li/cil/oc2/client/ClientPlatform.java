/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import dev.architectury.injectables.annotations.ExpectPlatform;

public final class ClientPlatform {
    @ExpectPlatform
    public static void setHotbarVisible(final boolean value) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static boolean isStencilEnabled(final RenderTarget target) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void enableStencil(final RenderTarget target) {
        throw new AssertionError();
    }

    private ClientPlatform() {
    }
}
