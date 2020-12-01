package li.cil.oc2.common.device.provider;

import li.cil.oc2.api.device.DeviceInterface;
import li.cil.oc2.api.device.object.Callback;
import li.cil.oc2.api.device.object.ObjectDeviceInterface;
import li.cil.oc2.api.device.provider.BlockDeviceQuery;
import li.cil.oc2.common.capabilities.Capabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

public class FluidHandlerDeviceInterfaceProvider extends AbstractCapabilityAnyTileEntityDeviceInterfaceProvider<IFluidHandler> {
    private static final String FLUID_HANDLER_TYPE_NAME = "fluidHandler";

    public FluidHandlerDeviceInterfaceProvider() {
        super(() -> Capabilities.FLUID_HANDLER_CAPABILITY);
    }

    @Override
    protected LazyOptional<DeviceInterface> getDeviceInterface(final BlockDeviceQuery query, final IFluidHandler value) {
        return LazyOptional.of(() -> new ObjectDeviceInterface(new FluidHandlerDevice(value), FLUID_HANDLER_TYPE_NAME));
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
