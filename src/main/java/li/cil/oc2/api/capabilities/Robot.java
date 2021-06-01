package li.cil.oc2.api.capabilities;

import net.minecraftforge.items.ContainerHelper;

/**
 * This interface may be implemented by entities hosting devices to allow
 * modules installed in them to interact with them.
 */
public interface Robot {
    /**
     * Provides access to the inventory of the robot.
     * <p>
     * This does <em>not</em> include the hardware configuration part of the inventory.
     *
     * @return the inventory of the robot.
     */
    ContainerHelper getInventory();

    /**
     * The index of the currently selected slot in the inventory.
     *
     * @return the selected slot.
     */
    int getSelectedSlot();

    /**
     * Sets the index of the currently selected slot in the inventory.
     *
     * @param value the slot to select.
     */
    void setSelectedSlot(final int value);
}
