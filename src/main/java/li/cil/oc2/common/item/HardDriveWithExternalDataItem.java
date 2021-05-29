package li.cil.oc2.common.item;

import net.minecraft.item.DyeColor;
import net.minecraft.item.IDyeableArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public final class HardDriveWithExternalDataItem extends AbstractBlockDeviceItem implements IDyeableArmorItem {
    private final int defaultColor;

    ///////////////////////////////////////////////////////////////////

    public HardDriveWithExternalDataItem(final ResourceLocation defaultData, final DyeColor defaultColor) {
        super(defaultData);
        this.defaultColor = defaultColor.getColorValue();
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public int getColor(final ItemStack stack) {
        return hasCustomColor(stack) ? IDyeableArmorItem.super.getColor(stack) : defaultColor;
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected String getOrCreateDescriptionId() {
        return "item.oc2.hard_drive";
    }
}
