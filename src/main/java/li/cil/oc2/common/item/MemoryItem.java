package li.cil.oc2.common.item;

public final class MemoryItem extends AbstractStorageItem {
    public MemoryItem(final int defaultCapacity) {
        super(defaultCapacity);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected String getDefaultTranslationKey() {
        return "item.oc2.memory";
    }
}
