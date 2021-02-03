package li.cil.oc2.common.container;

import li.cil.oc2.api.bus.device.DeviceType;
import li.cil.oc2.common.bus.device.util.Devices;
import li.cil.oc2.common.bus.device.util.ItemDeviceInfo;
import net.minecraft.item.ItemStack;

import java.util.List;
import java.util.function.Function;

public class TypedDeviceItemStackHandler extends DeviceItemStackHandler {
    private final DeviceType deviceType;

    ///////////////////////////////////////////////////////////////////

    public TypedDeviceItemStackHandler(final int size, final Function<ItemStack, List<ItemDeviceInfo>> deviceLookup, final DeviceType deviceType) {
        super(size, deviceLookup);
        this.deviceType = deviceType;
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public boolean isItemValid(final int slot, final ItemStack stack) {
        return super.isItemValid(slot, stack) && Devices.getDeviceTypes(stack).contains(deviceType);
    }
}
