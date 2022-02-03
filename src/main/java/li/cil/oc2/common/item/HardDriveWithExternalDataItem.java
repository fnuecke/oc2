/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.item;

import li.cil.oc2.api.API;
import li.cil.oc2.common.util.ColorUtils;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public final class HardDriveWithExternalDataItem extends AbstractBlockDeviceItem implements DyeableLeatherItem {
    private final int defaultColor;
    @Nullable private String descriptionId;

    ///////////////////////////////////////////////////////////////////

    public HardDriveWithExternalDataItem(final ResourceLocation defaultData, final DyeColor defaultColor) {
        super(defaultData);
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
