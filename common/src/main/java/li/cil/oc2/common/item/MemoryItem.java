/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.item;

import li.cil.oc2.api.API;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.function.IntSupplier;

public final class MemoryItem extends AbstractStorageItem {
    @Nullable
    private String descriptionId;

    // --------------------------------------------------------------------- //

    public MemoryItem(final IntSupplier defaultCapacity) {
        super(defaultCapacity);
    }

    // --------------------------------------------------------------------- //

    @Override
    protected String getOrCreateDescriptionId() {
        if (descriptionId == null) {
            descriptionId = Util.makeDescriptionId("item", ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "memory"));
        }
        return descriptionId;
    }
}
