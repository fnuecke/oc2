package li.cil.oc2.common.item;

import li.cil.oc2.common.Constants;
import net.minecraft.item.ItemStack;

public final class MemoryItem extends AbstractStorageItem {
    private static final int DEFAULT_CAPACITY = 2 * Constants.MEGABYTE;

    ///////////////////////////////////////////////////////////////////

    public static ItemStack withCapacity(final int capacity) {
        return withCapacity(new ItemStack(Items.MEMORY_ITEM.get()), capacity);
    }

    ///////////////////////////////////////////////////////////////////

    public MemoryItem(final Properties properties) {
        super(properties, DEFAULT_CAPACITY);
    }
}
