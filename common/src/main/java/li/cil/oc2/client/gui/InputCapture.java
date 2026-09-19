/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class InputCapture {
    private static boolean isEnabled;

    // --------------------------------------------------------------------- //

    public static boolean isEnabled() {
        return isEnabled;
    }

    public static void toggle() {
        isEnabled = !isEnabled;
    }
}
