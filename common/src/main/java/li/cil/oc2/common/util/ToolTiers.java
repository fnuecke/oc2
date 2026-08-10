/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;

import javax.annotation.Nullable;
import java.util.Locale;

public final class ToolTiers {
    @Nullable
    public static Tier byName(final ResourceLocation id) {
        if (!ResourceLocation.DEFAULT_NAMESPACE.equals(id.getNamespace())) {
            return null;
        }

        try {
            return Tiers.valueOf(id.getPath().toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException e) {
            return null;
        }
    }

    private ToolTiers() {
    }
}
