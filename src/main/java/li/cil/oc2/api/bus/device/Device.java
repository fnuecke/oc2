package li.cil.oc2.api.bus.device;

import li.cil.oc2.api.bus.DeviceBus;
import li.cil.oc2.api.bus.DeviceBusController;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.common.util.INBTSerializable;

/**
 * Base interface for objects that can be registered as devices on a {@link DeviceBus}.
 * <p>
 * Which types are handled/supported by a bus depends on context of the {@link DeviceBusController}
 * managing the bus.
 * <p>
 * Note that it is strongly encouraged for implementations to provide an overloaded
 * {@link Object#equals(Object)} and {@link Object#hashCode()} so that identical devices can be
 * detected.
 */
public interface Device extends INBTSerializable<CompoundNBT> {
    @Override
    default CompoundNBT serializeNBT() {
        return new CompoundNBT();
    }

    @Override
    default void deserializeNBT(final CompoundNBT tag) {
    }
}
