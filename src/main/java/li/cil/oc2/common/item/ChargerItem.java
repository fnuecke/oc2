package li.cil.oc2.common.item;

import li.cil.oc2.common.Config;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public final class ChargerItem extends ModBlockItem {
    public ChargerItem(final Block block) {
        super(block);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void fillItemCategory(final CreativeModeTab group, final NonNullList<ItemStack> items) {
        if (Config.chargerUseEnergy()) {
            super.fillItemCategory(group, items);
        }
    }
}
