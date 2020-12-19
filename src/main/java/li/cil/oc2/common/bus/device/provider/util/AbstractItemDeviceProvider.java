package li.cil.oc2.common.bus.device.provider.util;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.provider.DeviceQuery;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;

public abstract class AbstractItemDeviceProvider extends AbstractDeviceProvider {
    private final Item item;

    protected AbstractItemDeviceProvider(final Item item) {
        this.item = item;
    }

    @Override
    public LazyOptional<Device> getDevice(final DeviceQuery query) {
        if (!(query instanceof ItemDeviceQuery)) {
            return LazyOptional.empty();
        }

        final ItemDeviceQuery itemDeviceQuery = (ItemDeviceQuery) query;
        final ItemStack stack = itemDeviceQuery.getItemStack();
        if (stack.isEmpty()) {
            return LazyOptional.empty();
        }

        if (stack.getItem() != item) {
            return LazyOptional.empty();
        }

        return getItemDevice(itemDeviceQuery);
    }

    protected abstract LazyOptional<Device> getItemDevice(final ItemDeviceQuery query);
}
