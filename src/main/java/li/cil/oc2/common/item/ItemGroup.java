package li.cil.oc2.common.item;

import li.cil.oc2.api.API;
import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.bus.device.data.BlockDeviceDataRegistration;
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

            items.add(Items.FLASH_MEMORY.get().withCapacity(4 * Constants.KILOBYTE));
            items.add(Items.FLASH_MEMORY.get().withFirmware(Firmwares.BUILDROOT.get()));

            items.add(Items.MEMORY.get().withCapacity(2 * Constants.MEGABYTE));
            items.add(Items.MEMORY.get().withCapacity(4 * Constants.MEGABYTE));
            items.add(Items.MEMORY.get().withCapacity(8 * Constants.MEGABYTE));

            items.add(Items.HARD_DRIVE.get().withCapacity(2 * Constants.MEGABYTE));
            items.add(Items.HARD_DRIVE.get().withCapacity(4 * Constants.MEGABYTE));
            items.add(Items.HARD_DRIVE.get().withCapacity(8 * Constants.MEGABYTE));
            items.add(Items.HARD_DRIVE.get().withData(BlockDeviceDataRegistration.BUILDROOT.get()));

            items.add(getPreconfiguredComputer());

            items.sort(Comparator.comparing(ItemStack::getTranslationKey));
        }

        private ItemStack getPreconfiguredComputer() {
            final ItemStack computer = new ItemStack(Items.COMPUTER.get());

            final CompoundNBT computerItems = ItemStackUtils.getOrCreateTileEntityInventoryTag(computer);
            computerItems.put(DeviceTypes.MEMORY.getRegistryName().toString(), makeInventoryTag(
                    Items.MEMORY.get().withCapacity(8 * Constants.MEGABYTE),
                    Items.MEMORY.get().withCapacity(8 * Constants.MEGABYTE),
                    Items.MEMORY.get().withCapacity(8 * Constants.MEGABYTE)
            ));
            computerItems.put(DeviceTypes.HARD_DRIVE.getRegistryName().toString(), makeInventoryTag(
                    Items.HARD_DRIVE.get().withData(BlockDeviceDataRegistration.BUILDROOT.get())
            ));
            computerItems.put(DeviceTypes.FLASH_MEMORY.getRegistryName().toString(), makeInventoryTag(
                    Items.FLASH_MEMORY.get().withFirmware(Firmwares.BUILDROOT.get())
            ));
            computerItems.put(DeviceTypes.CARD.getRegistryName().toString(), makeInventoryTag(
                    new ItemStack(Items.NETWORK_INTERFACE_CARD.get())
            ));

            return computer;
        }
    };
}
