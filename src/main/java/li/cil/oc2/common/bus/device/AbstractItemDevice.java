package li.cil.oc2.common.bus.device;

import li.cil.oc2.api.bus.device.ItemDevice;
import net.minecraft.item.ItemStack;

public abstract class AbstractItemDevice extends AbstractObjectDevice<ItemStack> implements ItemDevice {
    public AbstractItemDevice(final ItemStack value) {
        super(value);
    }
}
