package li.cil.oc2.common.item;

import li.cil.oc2.common.util.TooltipUtils;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ModItem extends Item {
    public ModItem(final Properties properties) {
        super(properties);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void addInformation(final ItemStack stack, @Nullable final World world, final List<ITextComponent> tooltip, final ITooltipFlag flag) {
        super.addInformation(stack, world, tooltip, flag);
        TooltipUtils.tryAddDescription(stack, tooltip);
    }
}
