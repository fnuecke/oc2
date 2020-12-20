package li.cil.oc2.common.bus.device;

import li.cil.oc2.api.bus.device.ItemDevice;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;

public abstract class AbstractItemDevice extends AbstractObjectDevice<ItemStack> implements ItemDevice {
    public AbstractItemDevice(final ItemStack value) {
        super(value);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void exportToItemStack(final CompoundNBT nbt) {
    }

    @Override
    public void importFromItemStack(final CompoundNBT nbt) {
    }
}
