package li.cil.oc2.common.device.provider;

import li.cil.oc2.api.device.Device;
import li.cil.oc2.api.device.object.Callback;
import li.cil.oc2.api.device.object.ObjectDevice;
import li.cil.oc2.api.device.provider.BlockDeviceQuery;
import li.cil.oc2.common.capabilities.Capabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

public class FluidHandlerDeviceProvider extends AbstractCapabilityAnyTileEntityDeviceProvider<IFluidHandler> {
    public FluidHandlerDeviceProvider() {
        super(() -> Capabilities.FLUID_HANDLER_CAPABILITY);
    }

    @Override
    protected LazyOptional<Device> getDevice(final BlockDeviceQuery query, final IFluidHandler value) {
        return LazyOptional.of(() -> new FluidHandlerDevice(value));
    }

    public static final class FluidHandlerDevice extends ObjectDevice {
        private final IFluidHandler fluidHandler;

        public FluidHandlerDevice(final IFluidHandler fluidHandler) {
            super("fluidHandler");
            this.fluidHandler = fluidHandler;
        }

        @Callback
        public int getTanks() {
            return fluidHandler.getTanks();
        }

        @Callback
        public FluidStack getFluidInTank(final int tank) {
            return fluidHandler.getFluidInTank(tank);
        }

        @Callback
        public int getTankCapacity(final int tank) {
            return fluidHandler.getTankCapacity(tank);
        }
    }
}
