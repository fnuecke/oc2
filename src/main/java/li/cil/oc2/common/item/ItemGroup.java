package li.cil.oc2.common.item;

import li.cil.oc2.api.API;
import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.bus.device.data.BaseBlockDevices;
import li.cil.oc2.common.bus.device.data.Firmwares;
import li.cil.oc2.common.util.ItemStackUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.NonNullList;

import java.util.Comparator;

import static li.cil.oc2.common.util.NBTUtils.makeInventoryTag;

public final class ItemGroup {
    public static final net.minecraft.item.ItemGroup COMMON = new net.minecraft.item.ItemGroup(API.MOD_ID + ".common") {
        @Override
        public ItemStack createIcon() {
            return new ItemStack(Items.COMPUTER.get());
        }

        @Override
        public void fill(final NonNullList<ItemStack> items) {
            super.fill(items);

            items.add(FlashMemoryItem.withCapacity(4 * Constants.KILOBYTE));
            items.add(FlashMemoryItem.withFirmware(Firmwares.BUILDROOT.get()));

            items.add(MemoryItem.withCapacity(2 * Constants.MEGABYTE));
            items.add(MemoryItem.withCapacity(4 * Constants.MEGABYTE));
            items.add(MemoryItem.withCapacity(8 * Constants.MEGABYTE));

            items.add(HardDriveItem.withCapacity(2 * Constants.MEGABYTE));
            items.add(HardDriveItem.withCapacity(4 * Constants.MEGABYTE));
            items.add(HardDriveItem.withCapacity(8 * Constants.MEGABYTE));
            items.add(HardDriveItem.withBase(BaseBlockDevices.BUILDROOT.get()));

            items.add(getPreconfiguredComputer());

            items.sort(Comparator.comparing(ItemStack::getTranslationKey));
        }

        private ItemStack getPreconfiguredComputer() {
            final ItemStack computer = new ItemStack(Items.COMPUTER.get());

            final CompoundNBT computerItems = ItemStackUtils.getOrCreateTileEntityInventoryTag(computer);
            computerItems.put(DeviceTypes.MEMORY.getRegistryName().toString(), makeInventoryTag(
                    MemoryItem.withCapacity(8 * Constants.MEGABYTE),
                    MemoryItem.withCapacity(8 * Constants.MEGABYTE),
                    MemoryItem.withCapacity(8 * Constants.MEGABYTE)
            ));
            computerItems.put(DeviceTypes.HARD_DRIVE.getRegistryName().toString(), makeInventoryTag(
                    HardDriveItem.withBase(BaseBlockDevices.BUILDROOT.get())
            ));
            computerItems.put(DeviceTypes.FLASH_MEMORY.getRegistryName().toString(), makeInventoryTag(
                    FlashMemoryItem.withFirmware(Firmwares.BUILDROOT.get())
            ));
            computerItems.put(DeviceTypes.CARD.getRegistryName().toString(), makeInventoryTag(
                    new ItemStack(Items.NETWORK_INTERFACE_CARD.get())
            ));

            return computer;
        }
    };
}
