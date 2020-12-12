package li.cil.oc2.common.capabilities;

import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.api.bus.DeviceBusElement;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;

public final class Capabilities {
    @CapabilityInject(DeviceBusElement.class) @SuppressWarnings("FieldMayBeFinal")
    public static Capability<DeviceBusElement> DEVICE_BUS_ELEMENT_CAPABILITY = null;

    @CapabilityInject(DeviceBusController.class) @SuppressWarnings("FieldMayBeFinal")
    public static Capability<DeviceBusController> DEVICE_BUS_CONTROLLER_CAPABILITY = null;

    @CapabilityInject(IEnergyStorage.class) @SuppressWarnings("FieldMayBeFinal")
    public static Capability<IEnergyStorage> ENERGY_STORAGE_CAPABILITY = null;

    @CapabilityInject(IFluidHandler.class) @SuppressWarnings("FieldMayBeFinal")
    public static Capability<IFluidHandler> FLUID_HANDLER_CAPABILITY = null;

    @CapabilityInject(IItemHandler.class) @SuppressWarnings("FieldMayBeFinal")
    public static Capability<IItemHandler> ITEM_HANDLER_CAPABILITY = null;

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
        DeviceBusElementCapability.register();
        DeviceBusControllerCapability.register();
    }
}
