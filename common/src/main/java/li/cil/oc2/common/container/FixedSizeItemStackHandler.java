/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.container;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;

public class FixedSizeItemStackHandler extends ItemStackHandler {
    public FixedSizeItemStackHandler(final int size) {
        super(size);
    }

    public FixedSizeItemStackHandler(final NonNullList<ItemStack> stacks) {
        super(stacks);
    }

    // --------------------------------------------------------------------- //

    public boolean isEmpty() {
        for (int slot = 0; slot < getSlots(); slot++) {
            if (!getStackInSlot(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected int readSize(final ValueInput input) {
        // Our size is fixed, don't trust NBT data we're loading.
        return getSlots();
    }
}
