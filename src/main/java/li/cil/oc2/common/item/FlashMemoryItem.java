/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.item;

import li.cil.oc2.api.API;
import li.cil.oc2.common.bus.device.vm.item.ByteBufferFlashStorageDevice;
import li.cil.oc2.common.util.NBTTagIds;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public final class FlashMemoryItem extends AbstractStorageItem {
    @Nullable private String descriptionId;

    public FlashMemoryItem(final int defaultCapacity) {
        super(createProperties().stacksTo(1), defaultCapacity);
    }

    ///////////////////////////////////////////////////////////////////

    @Nullable
    @Override
    public CompoundTag getShareTag(final ItemStack stack) {
        final CompoundTag tag = super.getShareTag(stack);
        if (tag != null && tag.contains(API.MOD_ID, NBTTagIds.TAG_COMPOUND)) {
            tag.getCompound(API.MOD_ID).remove(ByteBufferFlashStorageDevice.DATA_TAG_NAME);
        }
        return tag;
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected String getOrCreateDescriptionId() {
        if (descriptionId == null) {
            descriptionId = Util.makeDescriptionId("item", Items.FLASH_MEMORY.getId());
        }
        return descriptionId;
    }
}
