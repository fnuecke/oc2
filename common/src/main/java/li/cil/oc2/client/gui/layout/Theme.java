/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui.layout;

import net.minecraft.resources.ResourceLocation;

public record Theme(ResourceLocation panel,
                          ResourceLocation toolbar,
                          ResourceLocation slot,
                          ResourceLocation inset,
                          ResourceLocation insetBlack,
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
            ResourceLocation.fromNamespaceAndPath(namespace, "ui/panel"),
            ResourceLocation.fromNamespaceAndPath(namespace, "ui/toolbar"),
            ResourceLocation.fromNamespaceAndPath(namespace, "ui/slot"),
            ResourceLocation.fromNamespaceAndPath(namespace, "ui/inset"),
            ResourceLocation.fromNamespaceAndPath(namespace, "ui/inset_black"),
            titleColor,
            labelColor);
    }
}
