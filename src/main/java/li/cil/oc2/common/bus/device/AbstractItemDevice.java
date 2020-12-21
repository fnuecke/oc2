package li.cil.oc2.common.bus.device;

import li.cil.oc2.api.bus.device.ItemDevice;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;

public abstract class AbstractItemDevice extends AbstractObjectDevice<ItemStack> implements ItemDevice {
    public AbstractItemDevice(final ItemStack value) {
        super(value);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void exportToItemStack(final CompoundTag nbt) {
    }

    @Override
    public void importFromItemStack(final CompoundTag nbt) {
    }
}
