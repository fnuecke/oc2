package li.cil.oc2.common.item;

import li.cil.oc2.api.API;
import li.cil.oc2.common.util.ColorUtils;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.Util;

import javax.annotation.Nullable;

public final class HardDriveItem extends AbstractStorageItem implements DyeableLeatherItem {
    private final int defaultColor;
    @Nullable private String descriptionId;

    ///////////////////////////////////////////////////////////////////

    public HardDriveItem(final int capacity, final DyeColor defaultColor) {
        super(capacity);
        this.defaultColor = ColorUtils.textureDiffuseColorsToRGB(defaultColor.getTextureDiffuseColors());
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public int getColor(final ItemStack stack) {
        return hasCustomColor(stack) ? DyeableLeatherItem.super.getColor(stack) : defaultColor;
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
