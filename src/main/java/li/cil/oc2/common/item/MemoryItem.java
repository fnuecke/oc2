package li.cil.oc2.common.item;

import li.cil.oc2.common.Constants;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

public final class MemoryItem extends AbstractStorageItem {
    private static final int DEFAULT_CAPACITY = 2 * Constants.MEGABYTE;

    ///////////////////////////////////////////////////////////////////

    public MemoryItem() {
        super(DEFAULT_CAPACITY);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void fillItemGroup(final ItemGroup group, final NonNullList<ItemStack> items) {
        if (isInGroup(group)) {
            items.add(withCapacity(2 * Constants.MEGABYTE));
            items.add(withCapacity(4 * Constants.MEGABYTE));
            items.add(withCapacity(8 * Constants.MEGABYTE));
        }
    }
}
