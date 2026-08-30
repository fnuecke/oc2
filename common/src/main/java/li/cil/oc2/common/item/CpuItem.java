/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.item;

import li.cil.oc2.api.bus.device.vm.ArchitectureType;

public final class CpuItem extends ModItem {
    private final ArchitectureType architectureType;

    // --------------------------------------------------------------------- //

    public CpuItem(final ArchitectureType architectureType) {
        this.architectureType = architectureType;
    }

    // --------------------------------------------------------------------- //

    public ArchitectureType getArchitectureType() {
        return architectureType;
    }
}
