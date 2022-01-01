package li.cil.oc2.common.capabilities;

import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.api.capabilities.NetworkInterface;
import li.cil.oc2.api.capabilities.RedstoneEmitter;
import li.cil.oc2.api.capabilities.Robot;
import li.cil.oc2.api.capabilities.TerminalUserProvider;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;

public final class Capabilities {
    public static final Capability<IEnergyStorage> ENERGY_STORAGE = CapabilityManager.get(new CapabilityToken<>() { });
    public static final Capability<IFluidHandler> FLUID_HANDLER = CapabilityManager.get(new CapabilityToken<>() { });
    public static final Capability<IItemHandler> ITEM_HANDLER = CapabilityManager.get(new CapabilityToken<>() { });

    public static final Capability<DeviceBusElement> DEVICE_BUS_ELEMENT = CapabilityManager.get(new CapabilityToken<>() { });
    public static final Capability<RedstoneEmitter> REDSTONE_EMITTER = CapabilityManager.get(new CapabilityToken<>() { });
    public static final Capability<NetworkInterface> NETWORK_INTERFACE = CapabilityManager.get(new CapabilityToken<>() { });
    public static final Capability<TerminalUserProvider> TERMINAL_USER_PROVIDER = CapabilityManager.get(new CapabilityToken<>() { });
    public static final Capability<Robot> ROBOT = CapabilityManager.get(new CapabilityToken<>() { });

    ///////////////////////////////////////////////////////////////////

    @SubscribeEvent
    public static void initialize(final RegisterCapabilitiesEvent event) {
        event.register(DeviceBusElement.class);
        event.register(RedstoneEmitter.class);
        event.register(NetworkInterface.class);
        event.register(TerminalUserProvider.class);
        event.register(Robot.class);
    }
}
