package li.cil.oc2.common.vm;

import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.api.bus.device.DeviceType;
import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.api.bus.device.vm.VMDevice;
import li.cil.oc2.common.bus.AbstractDeviceBusElement;
import li.cil.oc2.common.bus.device.util.ItemDeviceInfo;
import li.cil.oc2.common.container.DeviceContainerHelper;
import li.cil.oc2.common.container.TypedDeviceContainerHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.Optional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class AbstractVMContainerHelpers implements VMContainerHelpers {
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

    private static final long ITEM_DEVICE_BASE_ADDRESS = 0x20000000L;
    private static final int ITEM_DEVICE_STRIDE = 0x1000;
    private static final long OTHER_DEVICE_BASE_ADDRESS = 0x30000000L;

    ///////////////////////////////////////////////////////////////////

    public final AbstractDeviceBusElement busElement = new BusElement();

    // NB: linked hash map such that order of parameters in constructor is retained.
    //     This is relevant when assigning default addresses for devices.
    private final LinkedHashMap<DeviceType, DeviceContainerHelper> itemHandlers = new LinkedHashMap<>();

    public final IItemHandler combinedItemHandlers;

    ///////////////////////////////////////////////////////////////////

    public AbstractVMContainerHelpers(final GroupDefinition... groups) {
        for (final GroupDefinition group : groups) {
            itemHandlers.put(group.deviceType, new ItemHandler(group.count, this::getDeviceQuery, group.deviceType));
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

        for (final Map.Entry<DeviceType, DeviceContainerHelper> entry : itemHandlers.entrySet()) {
            final DeviceType deviceType = entry.getKey();
            final DeviceContainerHelper handler = entry.getValue();

            for (int i = 0; i < handler.getSlots(); i++) {
                final Collection<ItemDeviceInfo> devices = handler.getBusElement().getDeviceGroup(i);
                for (final ItemDeviceInfo info : devices) {
                    if (Objects.equals(info.device, wrapper)) {
                        // Ahhh, such special casing, much wow. Honestly I don't expect this
                        // special case to ever be needed for anything other than physical
                        // memory, so it's fine. Prove me wrong.
                        if (deviceType == DeviceTypes.MEMORY) {
                            return OptionalLong.empty();
                        } else {
                            return OptionalLong.of(address);
                        }
                    }
                }

                address += ITEM_DEVICE_STRIDE;
            }
        }

        return OptionalLong.of(OTHER_DEVICE_BASE_ADDRESS);
    }

    @Override
    public void exportDeviceDataToItemStacks() {
        for (final DeviceContainerHelper handler : itemHandlers.values()) {
            handler.exportDeviceDataToItemStacks();
        }
    }

    public void serialize(final CompoundTag tag) {
        itemHandlers.forEach((deviceType, handler) ->
                tag.put(deviceType.getRegistryName().toString(), handler.serializeNBT()));
    }

    public CompoundTag serialize() {
        final CompoundTag tag = new CompoundTag();
        serialize(tag);
        return tag;
    }

    public void deserialize(final CompoundTag tag) {
        itemHandlers.forEach((deviceType, handler) ->
                handler.deserializeNBT(tag.getCompound(deviceType.getRegistryName().toString())));
    }

    ///////////////////////////////////////////////////////////////////

    protected abstract ItemDeviceQuery getDeviceQuery(final ItemStack stack);

    protected void onContentsChanged(final DeviceContainerHelper itemHandler, final int slot) {
    }

    ///////////////////////////////////////////////////////////////////

    private final class ItemHandler extends TypedDeviceContainerHelper {
        public ItemHandler(final int size, final Function<ItemStack, ItemDeviceQuery> queryFactory, final DeviceType deviceType) {
            super(size, queryFactory, deviceType);
        }

        @Override
        protected void onContentsChanged(final int slot) {
            super.onContentsChanged(slot);
            AbstractVMContainerHelpers.this.onContentsChanged(this, slot);
        }
    }

    private final class BusElement extends AbstractDeviceBusElement {
        @Override
        public Optional<Collection<Optional<DeviceBusElement>>> getNeighbors() {
            return Optional.of(itemHandlers.values().stream()
                    .map(h -> Optional.of(() -> (DeviceBusElement) h.getBusElement()))
                    .collect(Collectors.toList()));
        }
    }
}
