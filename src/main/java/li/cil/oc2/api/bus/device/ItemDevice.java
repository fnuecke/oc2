package li.cil.oc2.api.bus.device;

import net.minecraft.nbt.CompoundTag;

public interface ItemDevice extends Device {
    void exportToItemStack(CompoundTag nbt);

    void importFromItemStack(CompoundTag nbt);
}
