/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.vm;

import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.api.bus.device.DeviceType;
import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.api.bus.device.vm.VMDevice;
import li.cil.oc2.common.bus.AbstractDeviceBusElement;
import li.cil.oc2.common.bus.AbstractItemDeviceBusElement;
import li.cil.oc2.common.container.AbstractDeviceItemStackHandler;
import li.cil.oc2.common.container.AbstractTypedDeviceItemStackHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;

import java.util.*;
import java.util.stream.Collectors;

import static li.cil.oc2.common.util.RegistryUtils.key;

public abstract class AbstractVMItemStackHandlers implements VMItemStackHandlers {
    public record GroupDefinition(DeviceType deviceType, int count) { }

    ///////////////////////////////////////////////////////////////////

    private static final long ITEM_DEVICE_BASE_ADDRESS = 0x20000000L;
    private static final int ITEM_DEVICE_STRIDE = 0x1000;
    private static final long OTHER_DEVICE_BASE_ADDRESS = 0x30000000L;

    ///////////////////////////////////////////////////////////////////

    public final AbstractDeviceBusElement busElement = new VMBusElement();

    // NB: linked hash map such that order of parameters in constructor is retained.
    //     This is relevant when assigning default addresses for devices.
    private final LinkedHashMap<DeviceType, AbstractDeviceItemStackHandler> itemHandlers = new LinkedHashMap<>();

    public final IItemHandler combinedItemHandlers;

    ///////////////////////////////////////////////////////////////////

    public AbstractVMItemStackHandlers(final GroupDefinition... groups) {
        for (final GroupDefinition group : groups) {
            itemHandlers.put(group.deviceType, new VMItemHandler(group.count, group.deviceType));
        }

        combinedItemHandlers = new CombinedInvWrapper(itemHandlers.values().toArray(new IItemHandlerModifiable[0]));
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public Optional<IItemHandler> getItemHandler(final DeviceType deviceType) {
        return Optional.ofNullable(itemHandlers.get(deviceType));
    }

    @Override
    public boolean isEmpty() {
        for (int slot = 0; slot < combinedItemHandlers.getSlots(); slot++) {
            if (!combinedItemHandlers.getStackInSlot(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public OptionalLong getDeviceAddressBase(final VMDevice wrapper) {
        long address = ITEM_DEVICE_BASE_ADDRESS;

        for (final Map.Entry<DeviceType, AbstractDeviceItemStackHandler> entry : itemHandlers.entrySet()) {
            final DeviceType deviceType = entry.getKey();
            final AbstractDeviceItemStackHandler handler = entry.getValue();

            for (int i = 0; i < handler.getSlots(); i++) {
                if (handler.getBusElement().groupContains(i, wrapper)) {
                    // Ahhh, such special casing, much wow. Honestly I don't expect this
                    // special case to ever be needed for anything other than physical
                    // memory, so it's fine. Prove me wrong.
                    if (deviceType == DeviceTypes.MEMORY) {
                        return OptionalLong.empty();
                    } else {
                        return OptionalLong.of(address);
                    }
                }

                address += ITEM_DEVICE_STRIDE;
            }
        }

        return OptionalLong.of(OTHER_DEVICE_BASE_ADDRESS);
    }

    @Override
    public void exportDeviceDataToItemStacks() {
        for (final AbstractDeviceItemStackHandler handler : itemHandlers.values()) {
            handler.exportDeviceDataToItemStacks();
        }
    }

    public void saveItems(final CompoundTag tag) {
        itemHandlers.forEach((deviceType, handler) -> {
            if (!handler.isEmpty()) {
                tag.put(key(deviceType), handler.saveItems());
            }
        });
    }

    public CompoundTag saveItems() {
        final CompoundTag tag = new CompoundTag();
        saveItems(tag);
        return tag;
    }

    public void loadItems(final CompoundTag tag) {
        itemHandlers.forEach((deviceType, handler) ->
            handler.loadItems(tag.getCompound(key(deviceType))));
    }

    public void saveDevices(final CompoundTag tag) {
        itemHandlers.forEach((deviceType, handler) ->
            tag.put(key(deviceType), handler.saveDevices()));
    }

    public CompoundTag saveDevices() {
        final CompoundTag tag = new CompoundTag();
        saveDevices(tag);
        return tag;
    }

    public void loadDevices(final CompoundTag tag) {
        itemHandlers.forEach((deviceType, handler) ->
            handler.loadDevices(tag.getCompound(key(deviceType))));
    }

    ///////////////////////////////////////////////////////////////////

    protected abstract ItemDeviceQuery makeQuery(final ItemStack stack);

    protected void onChanged() {
    }

    ///////////////////////////////////////////////////////////////////

    private final class VMItemHandler extends AbstractTypedDeviceItemStackHandler {
        private final VMItemBusElement busElement;

        public VMItemHandler(final int size, final DeviceType deviceType) {
            super(size, deviceType);
            this.busElement = new VMItemBusElement(getSlots());
        }

        @Override
        public AbstractItemDeviceBusElement getBusElement() {
            return busElement;
        }

        @Override
        protected void onContentsChanged(final int slot) {
            super.onContentsChanged(slot);
            onChanged();
        }
    }

    private final class VMItemBusElement extends AbstractItemDeviceBusElement {
        public VMItemBusElement(final int groupCount) {
            super(groupCount);
        }

        @Override
        protected ItemDeviceQuery makeQuery(final ItemStack stack) {
            return AbstractVMItemStackHandlers.this.makeQuery(stack);
        }
    }

    private final class VMBusElement extends AbstractDeviceBusElement {
        @Override
        public Optional<Collection<LazyOptional<DeviceBusElement>>> getNeighbors() {
            return Optional.of(itemHandlers.values().stream()
                .map(handler -> LazyOptional.of(() -> (DeviceBusElement) handler.getBusElement()))
                .collect(Collectors.toList()));
        }
    }
}
