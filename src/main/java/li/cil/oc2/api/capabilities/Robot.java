package li.cil.oc2.api.capabilities;

import net.minecraftforge.items.ItemStackHandler;

/**
 * This interface may be implemented by entities hosting devices to allow
 * modules installed in them to interact with them.
 */
public interface Robot {
    ItemStackHandler getInventory();

    int getSelectedSlot();

    void setSelectedSlot(final int value);
}
