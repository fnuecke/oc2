/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.neoforge;

import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

public final class ClientPlatformImpl {
    private static boolean isHotbarVisible = true;

    public static void setHotbarVisible(final boolean value) {
        isHotbarVisible = value;
    }

    public static boolean isHotbarVisible() {
        return isHotbarVisible;
    }

    private ClientPlatformImpl() {
    }
}
