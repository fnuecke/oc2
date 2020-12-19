package li.cil.oc2.common.bus;

import li.cil.oc2.common.bus.device.DeviceInfo;
import li.cil.oc2.common.bus.device.Devices;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemStackHandler;

import java.util.HashSet;

public class ItemHandlerDeviceBusElement extends AbstractGroupingDeviceBusElement {
    private final ItemStackHandler handler;

    ///////////////////////////////////////////////////////////////////

    public ItemHandlerDeviceBusElement(final ItemStackHandler handler) {
        super(handler.getSlots());
        this.handler = handler;
    }

    public void handleSlotChanged(final int slot) {
        final HashSet<DeviceInfo> newDevices = new HashSet<>();
        final ItemStack stack = handler.getStackInSlot(slot);
        if (!stack.isEmpty()) {
            for (final LazyOptional<DeviceInfo> info : Devices.getDevices(stack)) {
                info.ifPresent(newDevices::add);
                info.addListener(unused -> handleSlotChanged(slot));
            }
        }

        setDevicesForGroup(slot, newDevices);
    }
}
