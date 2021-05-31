package li.cil.oc2.common.item;

import li.cil.oc2.common.util.TooltipUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ModItem extends Item {
    public ModItem(final Properties properties) {
        super(properties.tab(ItemGroup.COMMON));
    }

    public ModItem() {
        this(createProperties());
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void appendHoverText(final ItemStack itemStack, @Nullable final Level level, final List<Component> list, final TooltipFlag tooltipFlag) {
        super.appendHoverText(itemStack, level, list, tooltipFlag);
        TooltipUtils.tryAddDescription(itemStack, list);
    }

    ///////////////////////////////////////////////////////////////////

    protected static Properties createProperties() {
        return new Properties();
    }
}
