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
import net.minecraftforge.items.wrapper.CombinedInvWrapper;

import java.util.*;
import java.util.function.Function;

public abstract class CommonVirtualMachineItemStackHandlers {
    private static final long ITEM_DEVICE_BASE_ADDRESS = 0x40000000L;
    private static final int ITEM_DEVICE_STRIDE = 0x1000;

    public static final String MEMORY_TAG_NAME = "memory";
    public static final String HARD_DRIVE_TAG_NAME = "hard_drive";
    public static final String FLASH_MEMORY_TAG_NAME = "flash_memory";
    public static final String CARD_TAG_NAME = "card";

    ///////////////////////////////////////////////////////////////////

    public static void addInformation(final ItemStack stack, final List<ITextComponent> tooltip) {
        TooltipUtils.addInventoryInformation(stack, tooltip,
                MEMORY_TAG_NAME,
                HARD_DRIVE_TAG_NAME,
                FLASH_MEMORY_TAG_NAME,
                CARD_TAG_NAME);
    }

    ///////////////////////////////////////////////////////////////////

    public final DeviceBusElement busElement = new BusElement();

    public final DeviceItemStackHandler memoryItemHandler;
    public final DeviceItemStackHandler hardDriveItemHandler;
    public final DeviceItemStackHandler flashMemoryItemHandler;
    public final DeviceItemStackHandler cardItemHandler;

    public final IItemHandler itemHandlers;

    ///////////////////////////////////////////////////////////////////

    public CommonVirtualMachineItemStackHandlers(final int memorySlots,
                                                 final int hardDriveSlots,
                                                 final int flashMemorySlots,
                                                 final int cardSlots) {
        memoryItemHandler = new ItemHandler(memorySlots, this::getDevices, DeviceTypes.MEMORY);
        hardDriveItemHandler = new ItemHandler(hardDriveSlots, this::getDevices, DeviceTypes.HARD_DRIVE);
        flashMemoryItemHandler = new ItemHandler(flashMemorySlots, this::getDevices, DeviceTypes.FLASH_MEMORY);
        cardItemHandler = new ItemHandler(cardSlots, this::getDevices, DeviceTypes.CARD);

        itemHandlers = new CombinedInvWrapper(memoryItemHandler, hardDriveItemHandler, flashMemoryItemHandler, cardItemHandler);
    }

    ///////////////////////////////////////////////////////////////////

    public Optional<IItemHandler> getItemHandler(final DeviceType deviceType) {
        if (deviceType == DeviceTypes.MEMORY) {
            return Optional.of(memoryItemHandler);
        } else if (deviceType == DeviceTypes.HARD_DRIVE) {
            return Optional.of(hardDriveItemHandler);
        } else if (deviceType == DeviceTypes.FLASH_MEMORY) {
            return Optional.of(flashMemoryItemHandler);
        } else if (deviceType == DeviceTypes.CARD) {
            return Optional.of(cardItemHandler);
        }
        return Optional.empty();
    }

    public boolean isEmpty() {
        for (int slot = 0; slot < itemHandlers.getSlots(); slot++) {
            if (!itemHandlers.getStackInSlot(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public OptionalLong getDefaultDeviceAddress(final VMDevice wrapper) {
        long address = ITEM_DEVICE_BASE_ADDRESS;

        for (int slot = 0; slot < hardDriveItemHandler.getSlots(); slot++) {
            final Collection<ItemDeviceInfo> devices = hardDriveItemHandler.getBusElement().getDeviceGroup(slot);
            for (final ItemDeviceInfo info : devices) {
                if (Objects.equals(info.device, wrapper)) {
                    return OptionalLong.of(address);
                }
            }

            address += ITEM_DEVICE_STRIDE;
        }

        for (int slot = 0; slot < cardItemHandler.getSlots(); slot++) {
            final Collection<ItemDeviceInfo> devices = cardItemHandler.getBusElement().getDeviceGroup(slot);
            for (final ItemDeviceInfo info : devices) {
                if (Objects.equals(info.device, wrapper)) {
                    return OptionalLong.of(address);
                }
            }

            address += ITEM_DEVICE_STRIDE;
        }

        return OptionalLong.empty();
    }

    public void exportDeviceDataToItemStacks() {
        memoryItemHandler.exportDeviceDataToItemStacks();
        hardDriveItemHandler.exportDeviceDataToItemStacks();
        flashMemoryItemHandler.exportDeviceDataToItemStacks();
        cardItemHandler.exportDeviceDataToItemStacks();
    }

    public void exportToItemStack(final ItemStack stack) {
        final CompoundNBT items = ItemStackUtils.getOrCreateTileEntityInventoryTag(stack);
        items.put(MEMORY_TAG_NAME, memoryItemHandler.serializeNBT());
        items.put(HARD_DRIVE_TAG_NAME, hardDriveItemHandler.serializeNBT());
        items.put(FLASH_MEMORY_TAG_NAME, flashMemoryItemHandler.serializeNBT());
        items.put(CARD_TAG_NAME, cardItemHandler.serializeNBT());
    }

    public CompoundNBT serialize() {
        final CompoundNBT tag = new CompoundNBT();

        tag.put(MEMORY_TAG_NAME, memoryItemHandler.serializeNBT());
        tag.put(HARD_DRIVE_TAG_NAME, hardDriveItemHandler.serializeNBT());
        tag.put(FLASH_MEMORY_TAG_NAME, flashMemoryItemHandler.serializeNBT());
        tag.put(CARD_TAG_NAME, cardItemHandler.serializeNBT());

        return tag;
    }

    public void deserialize(final CompoundNBT tag) {
        memoryItemHandler.deserializeNBT(tag.getCompound(MEMORY_TAG_NAME));
        hardDriveItemHandler.deserializeNBT(tag.getCompound(HARD_DRIVE_TAG_NAME));
        flashMemoryItemHandler.deserializeNBT(tag.getCompound(FLASH_MEMORY_TAG_NAME));
        cardItemHandler.deserializeNBT(tag.getCompound(CARD_TAG_NAME));
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
            CommonVirtualMachineItemStackHandlers.this.onContentsChanged(this, slot);
        }
    }

    private final class BusElement extends AbstractDeviceBusElement {
        @Override
        public Optional<Collection<LazyOptional<DeviceBusElement>>> getNeighbors() {
            return Optional.of(Arrays.asList(
                    LazyOptional.of(memoryItemHandler::getBusElement),
                    LazyOptional.of(hardDriveItemHandler::getBusElement),
                    LazyOptional.of(flashMemoryItemHandler::getBusElement),
                    LazyOptional.of(cardItemHandler::getBusElement)
            ));
        }
    }
}
