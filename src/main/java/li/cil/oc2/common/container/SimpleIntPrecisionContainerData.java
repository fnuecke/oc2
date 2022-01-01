package li.cil.oc2.common.container;

import net.minecraft.world.inventory.SimpleContainerData;

public final class SimpleIntPrecisionContainerData extends IntPrecisionContainerData {
    private final SimpleContainerData data;

    public SimpleIntPrecisionContainerData(final int size) {
        data = new SimpleContainerData(size * 2);
    }

    @Override
    public int get(final int index) {
        return data.get(index);
    }

    @Override
    public void set(final int index, final int value) {
        data.set(index, value & 0xFFFF);
    }

    @Override
    public int getCount() {
        return data.getCount();
    }
}
