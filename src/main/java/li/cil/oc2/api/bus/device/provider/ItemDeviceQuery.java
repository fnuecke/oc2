package li.cil.oc2.api.bus.device.provider;

import net.minecraft.item.ItemStack;

public interface ItemDeviceQuery extends DeviceQuery {
    ItemStack getItemStack();
}
