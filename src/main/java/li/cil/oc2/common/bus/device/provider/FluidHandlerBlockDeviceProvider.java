package li.cil.oc2.common.bus.device.provider;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.common.bus.device.provider.util.AbstractTileEntityCapabilityDeviceProvider;
import li.cil.oc2.common.bus.device.AbstractObjectDevice;
import li.cil.oc2.common.capabilities.Capabilities;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

public final class FluidHandlerBlockDeviceProvider extends AbstractTileEntityCapabilityDeviceProvider<IFluidHandler, TileEntity> {
    private static final String FLUID_HANDLER_TYPE_NAME = "fluidHandler";

    ///////////////////////////////////////////////////////////////////

    public FluidHandlerBlockDeviceProvider() {
        super(() -> Capabilities.FLUID_HANDLER_CAPABILITY);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected LazyOptional<Device> getBlockDevice(final BlockDeviceQuery query, final IFluidHandler value) {
        return LazyOptional.of(() -> new ObjectDevice(new FluidHandlerDevice(value), FLUID_HANDLER_TYPE_NAME));
    }

    ///////////////////////////////////////////////////////////////////

    public static final class FluidHandlerDevice extends AbstractObjectDevice<IFluidHandler> {
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
