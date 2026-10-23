/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.inventory;

import net.minecraft.world.item.ItemStack;

public interface DeferredNotifyItemHandler extends ItemHandler {
    void setStackInSlotSilently(int slot, ItemStack stack);

    void notifySlotChanged(int slot);
}
