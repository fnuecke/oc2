package li.cil.oc2.common.container;

import li.cil.oc2.common.util.NBTTagIds;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.NonNullList;
import net.minecraftforge.items.ItemStackHandler;

public class FixedSizeItemStackHandler extends ItemStackHandler {
    private static final String SIZE_TAG_NAME = "Size";

    ///////////////////////////////////////////////////////////////////

    public FixedSizeItemStackHandler() {
    }

    public FixedSizeItemStackHandler(final int size) {
        super(size);
    }

    public FixedSizeItemStackHandler(final NonNullList<ItemStack> stacks) {
        super(stacks);
    }

    ///////////////////////////////////////////////////////////////////

    public boolean isEmpty() {
        for (int slot = 0; slot < getSlots(); slot++) {
            if (!getStackInSlot(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void deserializeNBT(final CompoundTag tag) {
        // Our size is fixed, don't trust NBT data we're loading.
        if (tag.contains(SIZE_TAG_NAME, NBTTagIds.TAG_INT)) {
            final CompoundTag safeTag = tag.copy();
            safeTag.remove(SIZE_TAG_NAME);
            super.deserializeNBT(safeTag);
        } else {
            super.deserializeNBT(tag);
        }
    }
}
