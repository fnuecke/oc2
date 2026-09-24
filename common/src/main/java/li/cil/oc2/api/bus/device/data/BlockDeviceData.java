/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.bus.device.data;

import li.cil.sedna.api.device.BlockDevice;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;

import javax.annotation.Nullable;

/**
 * Implementations of this interface that are registered with the registry for
 * this type can be used as read-only base block devices for read-write block
 * devices.
 * <p>
 * This is used for the built-in Linux root file-system, for example.
 * <p>
 * Storage items (HDDs, Floppies, Flash Memory) can be assigned a registered block
 * data via the registry id. This is stored in the {@code image} string tag of the
 * item's {@code oc2} mod data.
 * <p>
 * To spawn one with an implementation registered as {@code my_mod:my_block_device}:
 * <pre>
 * /give @s oc2:hard_drive_large[minecraft:custom_data={oc2:{image:"my_mod:my_block_device"}}]
 * </pre>
 * It is also possible to use data packs to register block device data. See the
 * {@code docs/datapack.md} for what to place where.
 */
public interface BlockDeviceData {
    /**
     * Gets the read-only base block device this implementation describes.
     *
     * @return the block device.
     */
    BlockDevice getBlockDevice();

    /**
     * The size of the block device this implementation describes.
     * <p>
     * Used where only the size is relevant, such as tooltips and item listings,
     * and as such is also called on the client.
     *
     * @return the size of the block device, in bytes.
     */
    long getCapacity();

    /**
     * The display name of this block device base. May be shown in the tooltip
     * of item devices using this base.
     *
     * @return the display name of this block device.
     */
    Component getDisplayName();

    /**
     * The color items preloaded with this data are tinted with.
     * {@code null} leaves the item its own default color.
     *
     * @return the color, or {@code null} to keep the item's default.
     */
    @Nullable
    default DyeColor getColor() {
        return null;
    }
}
