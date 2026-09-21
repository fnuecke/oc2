/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.rpc.block;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.api.util.Invalidatable;
import li.cil.oc2.common.bus.device.provider.util.AbstractBlockDeviceProvider;
import li.cil.oc2.common.bus.device.rpc.FluidHandlerDevice;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.fluid.FluidHandler;
import li.cil.oc2.common.fluid.FluidStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

public final class FluidHandlerBlockDeviceProvider extends AbstractBlockDeviceProvider {
    @Override
    public Invalidatable<Device> getDevice(final BlockDeviceQuery query) {
        if (!(query.getLevel() instanceof final Level level)) {
            return Invalidatable.empty();
        }

        final BlockPos pos = query.getQueryPosition().immutable();
        final Direction side = query.getQuerySide();
        final Invalidatable<FluidHandler> value = Capabilities.watch(level, pos, side, Capabilities.FLUID_HANDLER);
        if (!value.isPresent()) {
            return Invalidatable.empty();
        }

        final Invalidatable<Device> device = Invalidatable.of(new ObjectDevice(
            new FluidHandlerDevice(new PositionalFluidHandler(level, pos, side))));
        final Invalidatable.ListenerToken token = value.addListener(unused -> device.invalidate());
        device.addListener(unused -> token.removeListener());

        return device;
    }

    // --------------------------------------------------------------------- //

    private record PositionalFluidHandler(Level level, BlockPos pos, @Nullable Direction side) implements FluidHandler {
        @Override
        public int getTanks() {
            return handler().getTanks();
        }

        @Override
        public FluidStack getFluidInTank(final int tank) {
            return handler().getFluidInTank(tank);
        }

        @Override
        public int getTankCapacity(final int tank) {
            return handler().getTankCapacity(tank);
        }

        @Override
        public int fill(final FluidStack stack, final boolean simulate) {
            return handler().fill(stack, simulate);
        }

        @Override
        public FluidStack drain(final FluidStack stack, final boolean simulate) {
            return handler().drain(stack, simulate);
        }

        @Override
        public FluidStack drain(final int amount, final boolean simulate) {
            return handler().drain(amount, simulate);
        }

        private FluidHandler handler() {
            final FluidHandler handler = Capabilities.get(level, pos, Capabilities.FLUID_HANDLER, side);
            if (handler == null) {
                throw new IllegalStateException("fluid container is gone");
            }
            return handler;
        }
    }
}
