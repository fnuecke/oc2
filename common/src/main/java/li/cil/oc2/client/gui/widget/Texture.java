/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui.widget;

import li.cil.oc2.api.API;
import net.minecraft.resources.Identifier;

public final class Texture {
    public final Identifier location;
    public final int width, height;

    public Texture(final String location, final int width, final int height) {
        this(Identifier.fromNamespaceAndPath(API.MOD_ID, location), width, height);
    }

    public Texture(final Identifier location, final int width, final int height) {
        this.location = location;
        this.width = width;
        this.height = height;
    }
}
