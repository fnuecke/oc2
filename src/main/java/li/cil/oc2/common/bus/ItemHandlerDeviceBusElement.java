package li.cil.oc2.common.bus;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.common.bus.device.provider.Providers;
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
        final HashSet<Device> newDevices = new HashSet<>();
        final ItemStack stack = handler.getStackInSlot(slot);
        if (!stack.isEmpty()) {
            for (final LazyOptional<Device> device : Providers.getDevices(stack)) {
                device.ifPresent(newDevices::add);
                device.addListener(unused -> handleSlotChanged(slot));
            }
        }

        setDevicesForGroup(slot, newDevices);
    }
}
