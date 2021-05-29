package li.cil.oc2.common.item;

import li.cil.oc2.api.API;
import net.minecraft.item.ItemStack;

public final class ItemGroup {
    public static final net.minecraft.item.ItemGroup COMMON = new net.minecraft.item.ItemGroup(API.MOD_ID + ".common") {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(Items.COMPUTER.get());
        }
    };
}
