package li.cil.oc2.common.item;

import li.cil.oc2.Constants;
import li.cil.oc2.api.API;
import li.cil.oc2.common.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

public final class ItemGroup {
    public static final net.minecraft.item.ItemGroup COMMON = new net.minecraft.item.ItemGroup(API.MOD_ID + ".common") {
        @Override
        public ItemStack createIcon() {
            return new ItemStack(Items.COMPUTER_ITEM.get());
        }

        @Override
        public void fill(final NonNullList<ItemStack> items) {
            super.fill(items);

            items.add(MemoryItem.withCapacity(new ItemStack(Items.MEMORY_ITEM.get()), 2 * Constants.MEGABYTE));
            items.add(MemoryItem.withCapacity(new ItemStack(Items.MEMORY_ITEM.get()), 4 * Constants.MEGABYTE));
            items.add(MemoryItem.withCapacity(new ItemStack(Items.MEMORY_ITEM.get()), 8 * Constants.MEGABYTE));

            items.add(HardDriveItem.withCapacity(new ItemStack(Items.HARD_DRIVE_ITEM.get()), 2 * Constants.MEGABYTE));
            items.add(HardDriveItem.withCapacity(new ItemStack(Items.HARD_DRIVE_ITEM.get()), 4 * Constants.MEGABYTE));
            items.add(HardDriveItem.withCapacity(new ItemStack(Items.HARD_DRIVE_ITEM.get()), 8 * Constants.MEGABYTE));
            items.add(HardDriveItem.withBaseBlockDevice(new ItemStack(Items.HARD_DRIVE_ITEM.get()), "linux"));
        }
    };
}
