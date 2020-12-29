package li.cil.oc2.common.capabilities;

import li.cil.oc2.api.bus.DeviceBusElement;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;

public final class Capabilities {
    @CapabilityInject(IEnergyStorage.class)
    public static Capability<IEnergyStorage> ENERGY_STORAGE = null;

    @CapabilityInject(IFluidHandler.class)
    public static Capability<IFluidHandler> FLUID_HANDLER = null;

    @CapabilityInject(IItemHandler.class)
    public static Capability<IItemHandler> ITEM_HANDLER = null;

    @CapabilityInject(DeviceBusElement.class)
    public static Capability<DeviceBusElement> DEVICE_BUS_ELEMENT = null;

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
        DeviceBusElementCapability.register();
    }
}
