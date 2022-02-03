/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.bus.device.object;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Collection;

/**
 * This interface is used to declare additional type names for a device on targets of an {@link ObjectDevice}.
 * <p>
 * For example: {@link Block}s and {@link BlockEntity}s that contain {@link Callback} methods may implement
 * this interface to provide additional type names.
 */
public interface NamedDevice {
    /**
     * A list of additional type names to associate with any {@link ObjectDevice} used
     * to make available methods in an instance of a class implementing this interface.
     *
     * @return the list of additional type names.
     */
    Collection<String> getDeviceTypeNames();
}
