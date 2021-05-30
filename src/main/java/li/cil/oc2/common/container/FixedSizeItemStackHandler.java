package li.cil.oc2.common.container;

import li.cil.oc2.common.util.NBTTagIds;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;

/** ItemStackHandler is a Forge API */
public class FixedSizeContainerHelper extends ContainerHelper {
    private static final String SIZE_TAG_NAME = "Size";

    ///////////////////////////////////////////////////////////////////

    public FixedSizeContainerHelper() {
    }

    public FixedSizeContainerHelper(final int size) {
        super(size);
    }

    public FixedSizeContainerHelper(final NonNullList<ItemStack> stacks) {
        super(stacks);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void deserializeTag(final CompoundTag tag) {
        // Our size is fixed, don't trust NBT data we're loading.
        if (tag.contains(SIZE_TAG_NAME, NBTTagIds.TAG_INT)) {
            final CompoundTag safeTag = tag.copy();
            safeTag.remove(SIZE_TAG_NAME);
            super.deserializeTag(safeTag);
        } else {
            super.deserializeTag(tag);
        }
    }
}
