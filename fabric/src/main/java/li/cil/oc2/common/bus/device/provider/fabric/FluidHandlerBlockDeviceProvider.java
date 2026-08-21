/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.provider.fabric;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.api.util.Invalidatable;
import li.cil.oc2.common.bus.device.provider.util.AbstractBlockDeviceProvider;
import li.cil.oc2.common.bus.device.rpc.fabric.FluidHandlerDevice;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.world.level.Level;

public final class FluidHandlerBlockDeviceProvider extends AbstractBlockDeviceProvider {
    @Override
    public Invalidatable<Device> getDevice(final BlockDeviceQuery query) {
        if (!(query.getLevel() instanceof final Level level)) {
            return Invalidatable.empty();
        }

        final Storage<FluidVariant> storage = FluidStorage.SIDED.find(
            level, query.getQueryPosition(), query.getQuerySide());
        if (storage == null) {
            return Invalidatable.empty();
        }

        return Invalidatable.of(new ObjectDevice(new FluidHandlerDevice(storage)));
    }
}
