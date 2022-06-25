/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.item;

import li.cil.oc2.api.API;
import li.cil.oc2.common.bus.device.data.BlockDeviceDataRegistry;
import li.cil.oc2.common.util.ColorUtils;
import net.minecraft.Util;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Objects;

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
    public void fillItemCategory(final CreativeModeTab tab, final NonNullList<ItemStack> items) {
        super.fillItemCategory(tab, items);

        BlockDeviceDataRegistry.values().forEach(data -> {
            if (!Objects.equals(BlockDeviceDataRegistry.getKey(data), getDefaultData())) {
                final ItemStack stack = withData(data);
                if (!stack.isEmpty()) {
                    items.add(stack);
                }
            }
        });
    }

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
