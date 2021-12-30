package li.cil.oc2.common.item;

import li.cil.oc2.common.util.TooltipUtils;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

import net.minecraft.world.item.Item.Properties;

public class ModBlockItem extends BlockItem {
    public ModBlockItem(final Block block, final Properties properties) {
        super(block, properties.tab(ItemGroup.COMMON));
    }

    public ModBlockItem(final Block block) {
        this(block, createProperties());
    }

    ///////////////////////////////////////////////////////////////////

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(final ItemStack stack, @Nullable final Level world, final List<Component> tooltip, final TooltipFlag flag) {
        TooltipUtils.tryAddDescription(stack, tooltip);
        super.appendHoverText(stack, world, tooltip, flag);
    }

    ///////////////////////////////////////////////////////////////////

    protected static Properties createProperties() {
        return new Properties();
    }
}
