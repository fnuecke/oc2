/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui.layout;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

import java.util.List;

public final class Ui {
    private static final int LABEL_GAP = 2;
    private static final Insets TOOLBAR_PADDING = new Insets(4, 4, 3, 4);
    private static final int TOOLBAR_GAP = 2;
    private static final int HOTBAR_SIZE = 9;
    private static final int INVENTORY_COLUMNS = 9;

    // --------------------------------------------------------------------- //

    public static NodeStack section(final Component title, final Node content) {
        return NodeStack.vertical(LabelNode.truncating(title), content).spacing(LABEL_GAP).width(Size.GROW);
    }

    public static NodeStack insetBlack(final Node content) {
        return NodeStack.vertical(content).padding(1).background(Theme::insetBlack);
    }

    public static NodeStack playerInventory(final AbstractContainerMenu menu, final Inventory inventory) {
        final Slot[] slots = new Slot[HOTBAR_SIZE + 3 * INVENTORY_COLUMNS];
        for (final Slot slot : menu.slots) {
            final int index = slot.getContainerSlot();
            if (slot.container == inventory && index >= 0 && index < slots.length && slots[index] == null) {
                slots[index] = slot;
            }
        }
        final List<Slot> all = List.of(slots);
        return NodeStack.vertical(
            slotGrid(all.subList(HOTBAR_SIZE, slots.length), INVENTORY_COLUMNS),
            slotGrid(all.subList(0, HOTBAR_SIZE), INVENTORY_COLUMNS)
        );
    }

    public static NodeStack slotGrid(final List<Slot> slots, final int columns) {
        final NodeStack grid = NodeStack.vertical().spacing(0);
        for (int start = 0; start < slots.size(); start += columns) {
            final NodeStack row = NodeStack.horizontal().spacing(0);
            for (final Slot slot : slots.subList(start, Math.min(start + columns, slots.size()))) {
                row.add(new SlotNode(slot));
            }
            grid.add(row);
        }
        return grid;
    }

    public static NodeStack toolbar(final Node... items) {
        return NodeStack.vertical(items).padding(TOOLBAR_PADDING).spacing(TOOLBAR_GAP).background(Theme::toolbar);
    }
}
