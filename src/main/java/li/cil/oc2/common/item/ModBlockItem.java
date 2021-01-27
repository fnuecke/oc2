package li.cil.oc2.common.item;

import li.cil.oc2.common.util.TooltipUtils;
import net.minecraft.block.Block;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ModBlockItem extends BlockItem {
    public ModBlockItem(final Block block, final Properties properties) {
        super(block, properties.group(ItemGroup.COMMON));
    }

    public ModBlockItem(final Block block) {
        this(block, createProperties());
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void addInformation(final ItemStack stack, @Nullable final World world, final List<ITextComponent> tooltip, final ITooltipFlag flag) {
        TooltipUtils.tryAddDescription(stack, tooltip);
        this.getBlock().addInformation(stack, world, tooltip, flag);
    }

    ///////////////////////////////////////////////////////////////////

    protected static Properties createProperties() {
        return new Properties();
    }
}
