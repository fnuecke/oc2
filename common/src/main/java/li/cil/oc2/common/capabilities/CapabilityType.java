/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.capabilities;

import net.minecraft.resources.Identifier;

public final class CapabilityType<T> {
    private final Identifier id;
    private final Class<T> type;

    public CapabilityType(final Identifier id, final Class<T> type) {
        this.id = id;
        this.type = type;
    }

    public Identifier id() {
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
