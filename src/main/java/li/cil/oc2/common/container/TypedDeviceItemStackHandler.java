package li.cil.oc2.common.container;

import li.cil.oc2.api.bus.device.DeviceType;
import li.cil.oc2.common.bus.device.Devices;
import net.minecraft.item.ItemStack;

public class TypedDeviceItemStackHandler extends DeviceItemStackHandler {
    private final DeviceType deviceType;

    public TypedDeviceItemStackHandler(final int size, final DeviceType deviceType) {
        super(size);
        this.deviceType = deviceType;
    }

    @Override
    public boolean isItemValid(final int slot, final ItemStack stack) {
        return super.isItemValid(slot, stack) && Devices.getDeviceTypes(stack).contains(deviceType);
    }
}
