package li.cil.oc2.common.bus.device.provider;

import alexiil.mc.lib.attributes.fluid.FixedFluidInvView;
import com.google.gson.JsonObject;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.common.bus.device.AbstractObjectDevice;
import li.cil.oc2.common.bus.device.provider.util.AbstractBlockAttributeDeviceProvider;
import li.cil.oc2.common.capabilities.Capabilities;
import net.minecraft.block.entity.BlockEntity;

import java.util.Optional;

public final class FluidHandlerBlockDeviceProvider extends AbstractBlockAttributeDeviceProvider<FixedFluidInvView, BlockEntity> {
    private static final String FLUID_HANDLER_TYPE_NAME = "fluidHandler";

    ///////////////////////////////////////////////////////////////////

    public FluidHandlerBlockDeviceProvider() {
        super(() -> Capabilities.FLUID_HANDLER_CAPABILITY);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected Optional<Device> getBlockDevice(final BlockDeviceQuery query, final FixedFluidInvView value) {
        return Optional.of(new ObjectDevice(new FluidHandlerDevice(value), FLUID_HANDLER_TYPE_NAME));
    }

    ///////////////////////////////////////////////////////////////////

    public static final class FluidHandlerDevice extends AbstractObjectDevice<FixedFluidInvView> {
        public FluidHandlerDevice(final FixedFluidInvView fluidHandler) {
            super(fluidHandler);
        }

        @Callback
        public int getTanks() {
            return value.getTankCount();
        }

        @Callback
        public JsonObject getFluidInTank(final int tank) {
            return value.getInvFluid(tank).fluidKey.toJson();
        }

        @Callback
        public int getTankCapacity(final int tank) {
            return value.getTank(tank).getMaxAmount_F().as1620();
        }
    }
}
