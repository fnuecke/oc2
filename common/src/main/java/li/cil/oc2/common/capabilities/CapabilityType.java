/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.capabilities;

import net.minecraft.resources.ResourceLocation;

public final class CapabilityType<T> {
    private final ResourceLocation id;
    private final Class<T> type;

    public CapabilityType(final ResourceLocation id, final Class<T> type) {
        this.id = id;
        this.type = type;
    }

    public ResourceLocation id() {
        return id;
    }

    public Class<T> type() {
        return type;
    }

    @Override
    public String toString() {
        return "CapabilityType[" + id + "]";
    }
}
