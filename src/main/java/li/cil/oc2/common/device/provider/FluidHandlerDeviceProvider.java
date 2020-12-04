package li.cil.oc2.common.device.provider;

import li.cil.oc2.api.bus.Device;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.provider.BlockDeviceQuery;
import li.cil.oc2.common.capabilities.Capabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

public class FluidHandlerDeviceProvider extends AbstractCapabilityAnyTileEntityDeviceProvider<IFluidHandler> {
    private static final String FLUID_HANDLER_TYPE_NAME = "fluidHandler";

    public FluidHandlerDeviceProvider() {
        super(() -> Capabilities.FLUID_HANDLER_CAPABILITY);
    }

    @Override
    protected LazyOptional<Device> getDeviceInterface(final BlockDeviceQuery query, final IFluidHandler value) {
        return LazyOptional.of(() -> new ObjectDevice(new FluidHandlerDevice(value), FLUID_HANDLER_TYPE_NAME));
    }

    public static final class FluidHandlerDevice extends AbstractObjectProxy<IFluidHandler> {
        public FluidHandlerDevice(final IFluidHandler fluidHandler) {
            super(fluidHandler);
        }

        @Callback
        public int getTanks() {
            return value.getTanks();
        }

        @Callback
        public FluidStack getFluidInTank(final int tank) {
            return value.getFluidInTank(tank);
        }

        @Callback
        public int getTankCapacity(final int tank) {
            return value.getTankCapacity(tank);
        }
    }
}
