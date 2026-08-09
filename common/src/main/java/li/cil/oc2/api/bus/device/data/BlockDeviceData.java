/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.bus.device.data;

import li.cil.sedna.api.device.BlockDevice;
import net.minecraft.network.chat.Component;

/**
 * Implementations of this interface that are registered with the registry for
 * this type can be used as read-only base block devices for read-write block
 * devices.
 * <p>
 * This is used for the built-in Linux root file-system, for example.
 * <p>
 * To make use of registered implementations, a hard drive item with the
 * string tag {@code oc2.base} referencing the implementation's registry id
 * must be created. For example, if the implementation's registry name is
 * {@code my_mod:my_block_device}:
 * <pre>
 * /give ? oc2:hard_drive{oc2:{data:"my_mod:my_block_device"}}
 * </pre>
 * The drive can be made readonly by also specifying the {@code readonly} tag:
 * <pre>
 * /give ? oc2:hard_drive{oc2:{data:"my_mod:my_block_device",readonly:true}}
 * </pre>
 */
public interface BlockDeviceData {
    /**
     * Gets the read-only base block device this implementation describes.
     *
     * @return the block device.
     */
    BlockDevice getBlockDevice();

    /**
     * The display name of this block device base. May be shown in the tooltip
     * of item devices using this base.
     *
     * @return the display name of this block device.
     */
    Component getDisplayName();
}
