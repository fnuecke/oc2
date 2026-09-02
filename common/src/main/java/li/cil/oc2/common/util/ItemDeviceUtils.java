/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public final class ItemDeviceUtils {
    public static final String ITEM_DEVICE_DATA_TAG_NAME = "item_device";

    // --------------------------------------------------------------------- //

    public static CompoundTag getItemDeviceData(final ItemStack stack) {
        return ItemStackUtils.getModDataTag(stack).getCompound(ITEM_DEVICE_DATA_TAG_NAME);
    }

    public static void setItemDeviceData(final ItemStack stack, final CompoundTag data) {
        ItemStackUtils.modifyModDataTag(stack, tag -> tag.put(ITEM_DEVICE_DATA_TAG_NAME, data));
    }

    public static CompoundTag getDeviceData(final ItemStack stack, final String key) {
        return getItemDeviceData(stack).getCompound(key);
    }

    public static void setDeviceData(final ItemStack stack, final String key, final CompoundTag data) {
        ItemStackUtils.modifyModDataTag(stack, tag ->
            NBTUtils.getOrCreateChildTag(tag, ITEM_DEVICE_DATA_TAG_NAME).put(key, data));
    }
}
