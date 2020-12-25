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

            items.add(RamItem.withCapacity(new ItemStack(Items.RAM_ITEM.get()), 2 * Constants.MEGABYTE));
            items.add(RamItem.withCapacity(new ItemStack(Items.RAM_ITEM.get()), 4 * Constants.MEGABYTE));
            items.add(RamItem.withCapacity(new ItemStack(Items.RAM_ITEM.get()), 8 * Constants.MEGABYTE));

            items.add(HddItem.withCapacity(new ItemStack(Items.HDD_ITEM.get()), 2 * Constants.MEGABYTE));
            items.add(HddItem.withCapacity(new ItemStack(Items.HDD_ITEM.get()), 4 * Constants.MEGABYTE));
            items.add(HddItem.withCapacity(new ItemStack(Items.HDD_ITEM.get()), 8 * Constants.MEGABYTE));
            items.add(HddItem.withBaseBlockDevice(new ItemStack(Items.HDD_ITEM.get()), "linux"));
        }
    };
}
