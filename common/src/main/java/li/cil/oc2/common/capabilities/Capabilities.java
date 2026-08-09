/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.capabilities;

import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.capabilities.NetworkInterface;
import li.cil.oc2.api.capabilities.RedstoneEmitter;
import li.cil.oc2.api.capabilities.Robot;
import li.cil.oc2.api.capabilities.TerminalUserProvider;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;

import java.util.function.Consumer;

public final class Capabilities {
    public static Capability<IEnergyStorage> energyStorage() {
        return CapabilityRegistry.ENERGY_STORAGE;
    }

    public static Capability<IFluidHandler> fluidHandler() {
        return CapabilityRegistry.FLUID_HANDLER;
    }

    public static Capability<IItemHandler> itemHandler() {
        return CapabilityRegistry.ITEM_HANDLER;
    }

    public static Capability<DeviceBusElement> deviceBusElement() {
        return CapabilityRegistry.DEVICE_BUS_ELEMENT;
    }

    public static Capability<Device> device() {
        return CapabilityRegistry.DEVICE;
    }

    public static Capability<RedstoneEmitter> redstoneEmitter() {
        return CapabilityRegistry.REDSTONE_EMITTER;
    }

    public static Capability<NetworkInterface> networkInterface() {
        return CapabilityRegistry.NETWORK_INTERFACE;
    }

    public static Capability<TerminalUserProvider> terminalUserProvider() {
        return CapabilityRegistry.TERMINAL_USER_PROVIDER;
    }

    public static Capability<Robot> robot() {
        return CapabilityRegistry.ROBOT;
    }

    public static void registerCapabilities(final Consumer<Class<?>> registry) {
        registry.accept(DeviceBusElement.class);
        registry.accept(Device.class);
        registry.accept(RedstoneEmitter.class);
        registry.accept(NetworkInterface.class);
        registry.accept(TerminalUserProvider.class);
        registry.accept(Robot.class);
    }
}
