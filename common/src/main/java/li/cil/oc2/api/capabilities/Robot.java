/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.capabilities;

/**
 * This interface may be implemented by entities hosting devices to allow
 * modules installed in them to interact with them.
 */
public interface Robot {
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
