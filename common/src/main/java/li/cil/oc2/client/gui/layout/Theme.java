/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui.layout;

import net.minecraft.resources.Identifier;

public record Theme(Identifier panel,
                          Identifier toolbar,
                          Identifier slot,
                          Identifier inset,
                          Identifier insetBlack,
                          int titleColor,
                          int labelColor
) {
    public Theme {
        if ((titleColor >>> 24) == 0 || (labelColor >>> 24) == 0) {
            throw new IllegalArgumentException("Text colors must not be fully transparent.");
        }
    }

    public static Theme of(final String namespace, final int titleColor, final int labelColor) {
        return new Theme(
            Identifier.fromNamespaceAndPath(namespace, "ui/panel"),
            Identifier.fromNamespaceAndPath(namespace, "ui/toolbar"),
            Identifier.fromNamespaceAndPath(namespace, "ui/slot"),
            Identifier.fromNamespaceAndPath(namespace, "ui/inset"),
            Identifier.fromNamespaceAndPath(namespace, "ui/inset_black"),
            titleColor,
            labelColor);
    }
}
