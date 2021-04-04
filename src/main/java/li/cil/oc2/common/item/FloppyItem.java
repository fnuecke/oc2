package li.cil.oc2.common.item;

import li.cil.oc2.client.item.CustomItemColors;
import li.cil.oc2.common.Constants;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.DyeColor;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;

public final class FloppyItem extends AbstractBlockDeviceItem implements IDyeableArmorItem {
    private static final int DEFAULT_CAPACITY = 512 * Constants.KILOBYTE;

    ///////////////////////////////////////////////////////////////////

    public FloppyItem() {
        super(DEFAULT_CAPACITY);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void fillItemGroup(final ItemGroup group, final NonNullList<ItemStack> items) {
        if (isInGroup(group)) {
            items.add(withCapacity(512 * Constants.KILOBYTE));
        }
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
