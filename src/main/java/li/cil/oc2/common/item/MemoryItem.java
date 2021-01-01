package li.cil.oc2.common.item;

import li.cil.oc2.common.Constants;

public final class MemoryItem extends AbstractStorageItem {
    private static final int DEFAULT_CAPACITY = 2 * Constants.MEGABYTE;

    public MemoryItem(final Properties properties) {
        super(properties, DEFAULT_CAPACITY);
    }
}
