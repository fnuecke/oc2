package li.cil.oc2.common.item;

import li.cil.oc2.client.item.CustomItemColors;
import li.cil.oc2.common.Constants;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.DyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public final class FloppyItem extends BlockDeviceItem {
    public FloppyItem(final Properties properties) {
        super(properties, 512 * Constants.KILOBYTE);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(final World world, final PlayerEntity player, final Hand hand) {
        // TODO replace this with recipes for coloring floppies
        final DyeColor[] colors = DyeColor.values();
        final DyeColor dye = colors[world.getRandom().nextInt(colors.length)];
        final int color = CustomItemColors.getColorByDye(dye);

        final ItemStack stack = player.getHeldItem(hand);
        final ItemStack coloredStack = CustomItemColors.withColor(stack, color);

        return ActionResult.resultSuccess(coloredStack);
    }
}
