/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.bus.device;

import li.cil.oc2.api.bus.DeviceBus;
import li.cil.oc2.api.bus.DeviceBusController;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.INBTSerializable;

import javax.annotation.Nullable;

/**
 * Base interface for objects that can be registered as devices on a {@link DeviceBus}.
 * <p>
 * Which types are handled/supported by a bus depends on context of the {@link DeviceBusController}
 * managing the bus.
 * <p>
 * May be provided as a capability on {@link BlockEntity}s.
 * <p>
 * Note that it is strongly encouraged for implementations to provide an overloaded
 * {@link Object#equals(Object)} and {@link Object#hashCode()} so that identical devices can be
 * detected.
 */
public interface Device extends INBTSerializable<CompoundTag> {
    /**
     * Called when the device is picked up by a {@link DeviceContainer}.
     * <p>
     * This only means the device will be hosted by the specified container, it has no implication
     * on whether the device is actually used by a computer, or whether that computer is running.
     *
     * @param container the container hosting this device.
     */
    default void setDeviceContainer(@Nullable final DeviceContainer container) {
    }

    /**
     * Called to serialize this device into its container's persistent storage.
     * <p>
     * When using this method, it may be necessary to call {@link DeviceContainer#setChanged()},
     * similar to how it is necessary to call {@link BlockEntity#setChanged()} when the serialized
     * representation of it changed. Unless this is called, no guarantee is made this method will
     * be called.
     *
     * @return the serialized state of this device.
     */
    @Override
    default CompoundTag serializeNBT() {
        return new CompoundTag();
    }

    /**
     * Called to deserialize this device from its container's persistent storage.
     * <p>
     * The passed tag will be what was last returned by {@link #serializeNBT()}.
     *
     * @param tag the serialized state of this device.
     */
    @Override
    default void deserializeNBT(final CompoundTag tag) {
    }
}
