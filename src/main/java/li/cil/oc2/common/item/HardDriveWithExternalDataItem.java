package li.cil.oc2.common.item;

import li.cil.oc2.api.API;
import net.minecraft.item.DyeColor;
import net.minecraft.item.IDyeableArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;

import javax.annotation.Nullable;

public final class HardDriveWithExternalDataItem extends AbstractBlockDeviceItem implements IDyeableArmorItem {
    private final int defaultColor;
    @Nullable private String descriptionId;

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
        if (descriptionId == null) {
            descriptionId = Util.makeDescriptionId("item", new ResourceLocation(API.MOD_ID, "hard_drive"));
        }
        return descriptionId;
    }
}
