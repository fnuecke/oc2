/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.item;

import li.cil.oc2.api.API;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

import java.util.function.IntSupplier;

public final class MemoryItem extends AbstractStorageItem {
    public MemoryItem(final Properties properties, final IntSupplier defaultCapacity) {
        super(properties.overrideDescription(Util.makeDescriptionId("item", Identifier.fromNamespaceAndPath(API.MOD_ID, "memory"))), defaultCapacity);
    }
}
