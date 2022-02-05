/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.bus.device;

import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Back-channel for devices to send messages to their containing context, such as Bus Interfaces.
 */
public interface DeviceContainer {
    /**
     * Notifies the container managing a device it is managing has changed.
     * <p>
     * This should be called by devices when they need to be serialized. It is the equivalent of the
     * {@link BlockEntity#setChanged()} method for {@link Device}s .
     */
    void setChanged();
}
