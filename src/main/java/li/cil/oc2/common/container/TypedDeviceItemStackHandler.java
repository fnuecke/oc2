package li.cil.oc2.common.container;

import li.cil.oc2.api.bus.device.DeviceType;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.common.bus.device.util.Devices;
import net.minecraft.item.ItemStack;

import java.util.function.Function;

public class TypedDeviceItemStackHandler extends DeviceItemStackHandler {
    private final DeviceType deviceType;

    ///////////////////////////////////////////////////////////////////

    public TypedDeviceItemStackHandler(final int size, final Function<ItemStack, ItemDeviceQuery> queryFactory, final DeviceType deviceType) {
        super(size, queryFactory);
        this.deviceType = deviceType;
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public boolean isItemValid(final int slot, final ItemStack stack) {
        return super.isItemValid(slot, stack) && Devices.getDeviceTypes(Devices.makeQuery(stack)).contains(deviceType);
    }
}
