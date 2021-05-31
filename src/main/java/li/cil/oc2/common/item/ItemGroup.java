package li.cil.oc2.common.item;

import li.cil.oc2.api.API;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public final class ItemGroup {
    public static final CreativeModeTab COMMON = new CreativeModeTab(3,API.MOD_ID + ".common") {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(Items.COMPUTER);
        }
    };
}
