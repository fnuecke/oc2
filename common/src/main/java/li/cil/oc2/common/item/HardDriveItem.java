/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.item;

import li.cil.oc2.api.API;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;

import javax.annotation.Nullable;

public final class HardDriveItem extends AbstractStorageItem implements ColoredItem {
    private final int defaultColor;
    @Nullable
    private String descriptionId;

    // ------------------------------------------------------------- //

    public HardDriveItem(final int capacity, final DyeColor defaultColor) {
        super(capacity);
        this.defaultColor = defaultColor.getTextureDiffuseColor();
    }

    // ------------------------------------------------------------- //

    @Override
    public int getColor(final ItemStack stack) {
        return DyedItemColor.getOrDefault(stack, defaultColor);
    }

    // ------------------------------------------------------------- //

    @Override
    protected String getOrCreateDescriptionId() {
        if (descriptionId == null) {
            descriptionId = Util.makeDescriptionId("item", ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "hard_drive"));
        }
        return descriptionId;
    }
}
