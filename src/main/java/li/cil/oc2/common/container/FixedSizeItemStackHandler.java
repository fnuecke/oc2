package li.cil.oc2.common.container;

import li.cil.oc2.common.util.NBTTagIds;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.NonNullList;
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

    @Override
    public void deserializeNBT(final CompoundNBT tag) {
        // Our size is fixed, don't trust NBT data we're loading.
        if (tag.contains(SIZE_TAG_NAME, NBTTagIds.TAG_INT)) {
            final CompoundNBT safeTag = tag.copy();
            safeTag.remove(SIZE_TAG_NAME);
            super.deserializeNBT(safeTag);
        } else {
            super.deserializeNBT(tag);
        }
    }
}
