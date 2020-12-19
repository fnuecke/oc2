package li.cil.oc2.common.item;

import li.cil.oc2.Constants;
import li.cil.oc2.api.API;
import li.cil.oc2.common.init.Items;
import net.minecraft.item.ItemStack;

public final class ItemGroup {
    public static final net.minecraft.item.ItemGroup COMMON = new net.minecraft.item.ItemGroup(API.MOD_ID + "." + Constants.COMMON_ITEM_GROUP_NAME) {
        @Override
        public ItemStack createIcon() {
            return new ItemStack(Items.COMPUTER_ITEM.get());
        }
    };
}
