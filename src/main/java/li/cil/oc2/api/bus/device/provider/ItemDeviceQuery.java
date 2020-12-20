package li.cil.oc2.api.bus.device.provider;

import net.minecraft.item.ItemStack;

/**
 * Device query for an item stack.
 *
 * @see ItemDeviceProvider
 */
public interface ItemDeviceQuery {
    /**
     * The item stack this query is performed for.
     *
     * @return the item stack to get a device for.
     */
    ItemStack getItemStack();
}
