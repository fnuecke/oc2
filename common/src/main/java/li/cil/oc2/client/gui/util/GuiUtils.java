/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui.util;

import net.minecraft.client.gui.components.EditBox;

public final class GuiUtils {
    public static boolean clampToRange(final EditBox field, final int minimum, final int maximum) {
        final String value = field.getValue();
        if (value.isEmpty()) {
            return false;
        }

        final String digits = value.replaceFirst("^0+(?=.)", "");
        final int parsed = digits.length() > Integer.toString(maximum).length() ? maximum : Integer.parseInt(digits);
        final String clamped = Integer.toString(Math.clamp(parsed, minimum, maximum));
        if (clamped.equals(value)) {
            return false;
        }

        field.setValue(clamped);
        return true;
    }
}
