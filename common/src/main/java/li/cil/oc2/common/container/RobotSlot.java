/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.container;

import li.cil.oc2.api.inventory.ItemHandler;
import net.minecraft.world.item.ItemStack;

public final class RobotSlot extends SlotItemHandler {
    public RobotSlot(final ItemHandler itemHandler, final int index, final int xPosition, final int yPosition) {
        super(itemHandler, index, xPosition, yPosition);
    }

    @Override
    public boolean mayPlace(final ItemStack stack) {
        return super.mayPlace(stack) && stack.getItem().canFitInsideContainerItems();
    }
}
