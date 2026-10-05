/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.provider.block;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.common.bus.device.EnergyHandlerDevice;
import li.cil.oc2.common.bus.device.provider.util.AbstractBlockEntityCapabilityDeviceProvider;
import li.cil.oc2.common.capabilities.CachedEnergyHandler;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.capabilities.CapabilityCache;
import li.cil.oc2.common.energy.EnergyHandler;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Optional;

public final class EnergyStorageBlockDeviceProvider extends AbstractBlockEntityCapabilityDeviceProvider<EnergyHandler, BlockEntity> {
    public EnergyStorageBlockDeviceProvider() {
        super(Capabilities.ENERGY_STORAGE);
    }

    // --------------------------------------------------------------------- //

    @Override
    protected Optional<Device> getBlockDevice(final BlockDeviceQuery query, final CapabilityCache<EnergyHandler> value) {
        return Optional.of(new ObjectDevice(new EnergyHandlerDevice(new CachedEnergyHandler(value))));
    }
}
