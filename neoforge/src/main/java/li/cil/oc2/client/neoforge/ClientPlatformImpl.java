/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.neoforge;

import com.mojang.blaze3d.pipeline.RenderTarget;

public final class ClientPlatformImpl {
    private static boolean isHotbarVisible = true;

    public static void setHotbarVisible(final boolean value) {
        isHotbarVisible = value;
    }

    public static boolean isHotbarVisible() {
        return isHotbarVisible;
    }

    public static boolean isStencilEnabled(final RenderTarget target) {
        return target.isStencilEnabled();
    }

    public static void enableStencil(final RenderTarget target) {
        target.enableStencil();
    }

    private ClientPlatformImpl() {
    }
}
