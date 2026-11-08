/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui.layout;

import net.minecraft.resources.ResourceLocation;

public record Theme(ResourceLocation panel,
                          ResourceLocation toolbar,
                          ResourceLocation slot,
                          ResourceLocation inset,
                          ResourceLocation insetBlack,
                          int labelColor
) {
    public Theme {
        if ((labelColor >>> 24) == 0) {
            throw new IllegalArgumentException("Label color must not be fully transparent.");
        }
    }

    public static Theme of(final String namespace, final int labelColor) {
        return new Theme(
            ResourceLocation.fromNamespaceAndPath(namespace, "ui/panel"),
            ResourceLocation.fromNamespaceAndPath(namespace, "ui/toolbar"),
            ResourceLocation.fromNamespaceAndPath(namespace, "ui/slot"),
            ResourceLocation.fromNamespaceAndPath(namespace, "ui/inset"),
            ResourceLocation.fromNamespaceAndPath(namespace, "ui/inset_black"),
            labelColor);
    }
}
