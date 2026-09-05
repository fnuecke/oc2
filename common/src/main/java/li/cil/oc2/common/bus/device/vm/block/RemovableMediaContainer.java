/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.vm.block;

import net.minecraft.world.item.ItemStack;

public interface RemovableMediaContainer {
    ItemStack getMediaItemStack();

    void setChanged();

    default void handleDataAccess() {
    }
}
