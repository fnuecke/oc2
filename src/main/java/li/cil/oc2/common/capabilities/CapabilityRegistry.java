/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.capabilities;

import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.api.bus.device.Device;
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

public final class CapabilityRegistry {
    static final Capability<IEnergyStorage> ENERGY_STORAGE = CapabilityManager.get(new CapabilityToken<>() { });
    static final Capability<IFluidHandler> FLUID_HANDLER = CapabilityManager.get(new CapabilityToken<>() { });
    static final Capability<IItemHandler> ITEM_HANDLER = CapabilityManager.get(new CapabilityToken<>() { });

    static final Capability<DeviceBusElement> DEVICE_BUS_ELEMENT = CapabilityManager.get(new CapabilityToken<>() { });
    static final Capability<Device> DEVICE = CapabilityManager.get(new CapabilityToken<>() { });
    static final Capability<RedstoneEmitter> REDSTONE_EMITTER = CapabilityManager.get(new CapabilityToken<>() { });
    static final Capability<NetworkInterface> NETWORK_INTERFACE = CapabilityManager.get(new CapabilityToken<>() { });
    static final Capability<TerminalUserProvider> TERMINAL_USER_PROVIDER = CapabilityManager.get(new CapabilityToken<>() { });
    static final Capability<Robot> ROBOT = CapabilityManager.get(new CapabilityToken<>() { });

    ///////////////////////////////////////////////////////////////////

    @SubscribeEvent
    public static void initialize(final RegisterCapabilitiesEvent event) {
        Capabilities.registerCapabilities(event::register);
    }
}
