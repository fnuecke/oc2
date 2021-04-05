package li.cil.oc2.common.item;

import li.cil.oc2.api.API;
import li.cil.oc2.common.bus.device.item.ByteBufferFlashMemoryVMDevice;
import li.cil.oc2.common.util.NBTTagIds;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;

import javax.annotation.Nullable;

public final class FlashMemoryItem extends AbstractStorageItem {
    public FlashMemoryItem(final int defaultCapacity) {
        super(createProperties().maxStackSize(1), defaultCapacity);
    }

    ///////////////////////////////////////////////////////////////////

    @Nullable
    @Override
    public CompoundNBT getShareTag(final ItemStack stack) {
        final CompoundNBT tag = super.getShareTag(stack);
        if (tag != null && tag.contains(API.MOD_ID, NBTTagIds.TAG_COMPOUND)) {
            tag.getCompound(API.MOD_ID).remove(ByteBufferFlashMemoryVMDevice.DATA_TAG_NAME);
        }
        return tag;
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected String getDefaultTranslationKey() {
        return "item.oc2.flash_memory";
    }
}
