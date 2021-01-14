package li.cil.oc2.common.vm;

import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.api.bus.device.DeviceType;
import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.api.bus.device.vm.VMDevice;
import li.cil.oc2.common.bus.AbstractDeviceBusElement;
import li.cil.oc2.common.bus.device.util.ItemDeviceInfo;
import li.cil.oc2.common.container.DeviceItemStackHandler;
import li.cil.oc2.common.container.TypedDeviceItemStackHandler;
import li.cil.oc2.common.util.ItemStackUtils;
import li.cil.oc2.common.util.TooltipUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.RegistryManager;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class AbstractVirtualMachineItemStackHandlers implements VirtualMachineItemStackHandlers {
    public static final class GroupDefinition {
        public final DeviceType deviceType;
        public final int count;

        public static GroupDefinition of(final DeviceType deviceType, final int count) {
            return new GroupDefinition(deviceType, count);
        }

        private GroupDefinition(final DeviceType deviceType, final int count) {
            this.deviceType = deviceType;
            this.count = count;
        }
    }

    ///////////////////////////////////////////////////////////////////

    private static final long ITEM_DEVICE_BASE_ADDRESS = 0x40000000L;
    private static final int ITEM_DEVICE_STRIDE = 0x1000;

    ///////////////////////////////////////////////////////////////////

    public static void addInformation(final ItemStack stack, final List<ITextComponent> tooltip) {
        final ForgeRegistry<DeviceType> registry = RegistryManager.ACTIVE.getRegistry(DeviceType.REGISTRY);
        if (registry != null) {
            TooltipUtils.addInventoryInformation(stack, tooltip,
                    registry.getValues().stream().map(deviceType ->
                            deviceType.getRegistryName().toString()).toArray(String[]::new));
        }
    }

    ///////////////////////////////////////////////////////////////////

    public final DeviceBusElement busElement = new BusElement();

    // NB: linked hash map such that order of parameters in constructor is retained.
    //     This is relevant when assigning default addresses for devices.
    private final LinkedHashMap<DeviceType, DeviceItemStackHandler> itemHandlers = new LinkedHashMap<>();

    public final IItemHandler combinedItemHandlers;

    ///////////////////////////////////////////////////////////////////

    public AbstractVirtualMachineItemStackHandlers(final GroupDefinition... groups) {
        for (final GroupDefinition group : groups) {
            itemHandlers.put(group.deviceType, new ItemHandler(group.count, this::getDevices, group.deviceType));
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

    public OptionalLong getDefaultDeviceAddress(final VMDevice wrapper) {
        long address = ITEM_DEVICE_BASE_ADDRESS;

        for (final Map.Entry<DeviceType, DeviceItemStackHandler> entry : itemHandlers.entrySet()) {
            final DeviceType deviceType = entry.getKey();
            final DeviceItemStackHandler handler = entry.getValue();

            // Ahhh, such special casing, much wow. Honestly I don't expect this
            // special case to ever be needed for anything other than physical
            // memory, so it's fine. Prove me wrong.
            if (deviceType == DeviceTypes.MEMORY) {
                continue;
            }

            for (int i = 0; i < handler.getSlots(); i++) {
                final Collection<ItemDeviceInfo> devices = handler.getBusElement().getDeviceGroup(i);
                for (final ItemDeviceInfo info : devices) {
                    if (Objects.equals(info.device, wrapper)) {
                        return OptionalLong.of(address);
                    }
                }
            }

            address += ITEM_DEVICE_STRIDE;
        }

        return OptionalLong.empty();
    }

    @Override
    public void exportDeviceDataToItemStacks() {
        for (final DeviceItemStackHandler handler : itemHandlers.values()) {
            handler.exportDeviceDataToItemStacks();
        }
    }

    public void exportToItemStack(final ItemStack stack) {
        final CompoundNBT tag = ItemStackUtils.getOrCreateTileEntityInventoryTag(stack);

        itemHandlers.forEach((deviceType, handler) ->
                tag.put(deviceType.getRegistryName().toString(), handler.serializeNBT()));
    }

    public CompoundNBT serialize() {
        final CompoundNBT tag = new CompoundNBT();

        itemHandlers.forEach((deviceType, handler) ->
                tag.put(deviceType.getRegistryName().toString(), handler.serializeNBT()));

        return tag;
    }

    public void deserialize(final CompoundNBT tag) {
        itemHandlers.forEach((deviceType, handler) ->
                handler.deserializeNBT(tag.getCompound(deviceType.getRegistryName().toString())));
    }

    ///////////////////////////////////////////////////////////////////

    protected abstract List<ItemDeviceInfo> getDevices(final ItemStack stack);

    protected void onContentsChanged(final DeviceItemStackHandler itemHandler, final int slot) {
    }

    ///////////////////////////////////////////////////////////////////

    private final class ItemHandler extends TypedDeviceItemStackHandler {
        public ItemHandler(final int size, final Function<ItemStack, List<ItemDeviceInfo>> deviceLookup, final DeviceType deviceType) {
            super(size, deviceLookup, deviceType);
        }

        @Override
        protected void onContentsChanged(final int slot) {
            super.onContentsChanged(slot);
            AbstractVirtualMachineItemStackHandlers.this.onContentsChanged(this, slot);
        }
    }

    private final class BusElement extends AbstractDeviceBusElement {
        @Override
        public Optional<Collection<LazyOptional<DeviceBusElement>>> getNeighbors() {
            return Optional.of(itemHandlers.values().stream()
                    .map(h -> LazyOptional.of(() -> (DeviceBusElement) h.getBusElement()))
                    .collect(Collectors.toList()));
        }
    }
}
