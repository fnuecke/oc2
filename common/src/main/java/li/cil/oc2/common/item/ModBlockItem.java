/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.item;

import li.cil.oc2.common.util.TooltipUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import javax.annotation.Nullable;
import java.util.List;

public class ModBlockItem extends BlockItem {
    public ModBlockItem(final Block block, final Properties properties) {
        super(block, properties);
    }

    public ModBlockItem(final Block block) {
        this(block, createProperties());
    }

    ///////////////////////////////////////////////////////////////////

    @Environment(EnvType.CLIENT)
    @Override
    public void appendHoverText(final ItemStack stack, final Item.TooltipContext context, final List<Component> tooltip, final TooltipFlag flag) {
        TooltipUtils.tryAddDescription(stack, tooltip);
        super.appendHoverText(stack, context, tooltip, flag);
    }

    ///////////////////////////////////////////////////////////////////

    protected static Properties createProperties() {
        return new Properties();
    }
}
