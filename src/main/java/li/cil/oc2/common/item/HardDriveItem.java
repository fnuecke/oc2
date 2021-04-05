package li.cil.oc2.common.item;

import net.minecraft.item.DyeColor;
import net.minecraft.item.IDyeableArmorItem;
import net.minecraft.item.ItemStack;

public final class HardDriveItem extends AbstractStorageItem implements IDyeableArmorItem {
    private final int defaultColor;

    ///////////////////////////////////////////////////////////////////

    public HardDriveItem(final int capacity, final DyeColor defaultColor) {
        super(capacity);
        this.defaultColor = defaultColor.getColorValue();
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public int getColor(final ItemStack stack) {
        return hasColor(stack) ? IDyeableArmorItem.super.getColor(stack) : defaultColor;
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected String getDefaultTranslationKey() {
        return "item.oc2.hard_drive";
    }
}
