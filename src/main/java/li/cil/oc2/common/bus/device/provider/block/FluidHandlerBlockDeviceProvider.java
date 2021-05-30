package li.cil.oc2.common.bus.device.provider.block;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.common.bus.device.provider.util.AbstractBlockEntityCapabilityDeviceProvider;
import li.cil.oc2.common.bus.device.util.IdentityProxy;
import li.cil.oc2.common.capabilities.Capabilities;
import net.minecraft.tileentity.BlockEntity;
import net.minecraftforge.common.util.Optional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

public final class FluidHandlerBlockDeviceProvider extends AbstractBlockEntityCapabilityDeviceProvider<IFluidHandler, BlockEntity> {
    public FluidHandlerBlockDeviceProvider() {
        super(() -> Capabilities.FLUID_HANDLER);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected Optional<Device> getBlockDevice(final BlockDeviceQuery query, final IFluidHandler value) {
        return Optional.of(() -> new ObjectDevice(new FluidHandlerDevice(value), "fluid_handler"));
    }

    ///////////////////////////////////////////////////////////////////

    public static final class FluidHandlerDevice extends IdentityProxy<IFluidHandler> {
        public FluidHandlerDevice(final IFluidHandler identity) {
            super(identity);
        }

        @Callback
        public int getTanks() {
            return identity.getTanks();
        }

        @Callback
        public FluidStack getFluidInTank(final int tank) {
            return identity.getFluidInTank(tank);
        }

        @Callback
        public int getTankCapacity(final int tank) {
            return identity.getTankCapacity(tank);
        }
    }
}
