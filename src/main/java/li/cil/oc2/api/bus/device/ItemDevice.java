package li.cil.oc2.api.bus.device;

import net.minecraft.nbt.CompoundNBT;

public interface ItemDevice extends Device {
    void exportToItemStack(CompoundNBT nbt);

    void importFromItemStack(CompoundNBT nbt);
}
