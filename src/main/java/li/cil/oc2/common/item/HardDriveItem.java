package li.cil.oc2.common.item;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.ItemStack;

public final class HardDriveItem extends AbstractStorageItem implements DyeableLeatherItem {
    private final int defaultColor;

    ///////////////////////////////////////////////////////////////////

    public HardDriveItem(final int capacity, final DyeColor defaultColor) {
        super(capacity);
        this.defaultColor = defaultColor.getId();
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public int getColor(final ItemStack stack) {
        return hasCustomColor(stack) ? DyeableLeatherItem.super.getColor(stack) : defaultColor;
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected String getOrCreateDescriptionId() {
        return "item.oc2.hard_drive";
    }
}
