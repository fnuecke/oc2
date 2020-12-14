package li.cil.oc2.api.bus.device;

import li.cil.oc2.api.bus.DeviceBus;
import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.api.bus.DeviceBusElement;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.Optional;
import java.util.UUID;

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
    /**
     * An identifier used to associate save data with this device.
     * <p>
     * This is <em>required</em> for a device to be serialized. When this returns
     * {@link Optional#empty()}, {@link #serializeNBT()} and {@link #deserializeNBT(CompoundNBT)}
     * will not be called.
     * <p>
     * Not to be confused with the identifiers used for devices on a {@link DeviceBus},
     * which are assigned by {@link DeviceBusElement}s to the devices they contain.
     *
     * @return a stable, unique identifier for this specific device type.
     */
    default Optional<UUID> getSerializationKey() {
        return Optional.empty();
    }

    @Override
    default CompoundNBT serializeNBT() {
        return new CompoundNBT();
    }

    @Override
    default void deserializeNBT(final CompoundNBT nbt) {
    }
}
