package li.cil.oc2.api.bus.device;

import net.minecraft.nbt.CompoundNBT;

/**
 * Specialized device type provided by {@link li.cil.oc2.api.bus.device.provider.ItemDeviceProvider}s.
 * <p>
 * This interface provides methods that allow the context in which an item based device is
 * created to copy data from and to the {@link net.minecraft.item.ItemStack} the device was
 * created for.
 * <p>
 * By default, no data is copied to and from the {@link net.minecraft.item.ItemStack}. Use
 * these methods for storing data that should survive the item the device is based on being
 * removed from the context (e.g. a computer) and being put back in some context.
 */
public interface ItemDevice extends Device {
    /**
     * Export data that should be copied to the {@link net.minecraft.item.ItemStack}
     * this device was created for to a tag that will be stored in the item.
     *
     * @param nbt the tag that will be written to the item.
     */
    default void exportToItemStack(final CompoundNBT nbt) {
    }

    /**
     * Import data that is present on the {@link net.minecraft.item.ItemStack} this
     * device was created for. The provided tag corresponds to the one presented to
     * the {@link #exportToItemStack(CompoundNBT)} method.
     *
     * @param nbt the tag that was read from the item.
     */
    default void importFromItemStack(final CompoundNBT nbt) {
    }
}
