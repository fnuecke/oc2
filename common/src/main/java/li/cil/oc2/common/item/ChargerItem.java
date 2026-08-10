/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.item;

import li.cil.oc2.common.Config;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public final class ChargerItem extends ModBlockItem implements CreativeTabItemProvider {
    public ChargerItem(final Block block) {
        super(block);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void addCreativeTabItems(final CreativeModeTab.Output output) {
        if (Config.chargerUseEnergy()) {
            output.accept(new ItemStack(this));
        }
    }
}
