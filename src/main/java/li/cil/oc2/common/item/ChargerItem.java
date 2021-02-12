package li.cil.oc2.common.item;

import li.cil.oc2.common.Config;
import net.minecraft.block.Block;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

public final class ChargerItem extends ModBlockItem {
    public ChargerItem(final Block block) {
        super(block);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void fillItemGroup(final ItemGroup group, final NonNullList<ItemStack> items) {
        if (Config.chargerUseEnergy()) {
            super.fillItemGroup(group, items);
        }
    }
}
