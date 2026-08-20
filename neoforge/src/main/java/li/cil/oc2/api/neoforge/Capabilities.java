/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.neoforge;

import li.cil.oc2.api.API;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.capabilities.ItemCapability;

/**
 * The NeoForge capabilities OpenComputers II provides.
 */
public final class Capabilities {
    public static final class Device {
        /**
         * Devices provided by a block, {@code oc2:device}.
         * <p>
         * Register a block with this to expose it as a device to an adjacent device bus.
         */
        public static final BlockCapability<li.cil.oc2.api.bus.device.Device, Direction> BLOCK =
                BlockCapability.createSided(ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "device"),
                        li.cil.oc2.api.bus.device.Device.class);

        /**
         * Devices provided by an item, {@code oc2:device}.
         * <p>
         * Register an item with this to expose it as a device while it is installed in a device slot.
         */
        public static final ItemCapability<li.cil.oc2.api.bus.device.Device, Void> ITEM =
                ItemCapability.createVoid(ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "device"),
                        li.cil.oc2.api.bus.device.Device.class);

        private Device() {
        }
    }

    public static final class DeviceBusElement {
        /**
         * Device bus connection points, {@code oc2:device_bus_element}.
         * <p>
         * Register a block with this to make it part of a device bus, e.g. to provide a custom cable.
         */
        public static final BlockCapability<li.cil.oc2.api.bus.DeviceBusElement, Direction> BLOCK =
                BlockCapability.createSided(ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "device_bus_element"),
                        li.cil.oc2.api.bus.DeviceBusElement.class);

        private DeviceBusElement() {
        }
    }

    public static final class RedstoneEmitter {
        /**
         * Redstone signal sources, {@code oc2:redstone_emitter}.
         * <p>
         * Register a block with this to have it emit a redstone signal on behalf of a device.
         */
        public static final BlockCapability<li.cil.oc2.api.capabilities.RedstoneEmitter, Direction> BLOCK =
                BlockCapability.createSided(ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "redstone_emitter"),
                        li.cil.oc2.api.capabilities.RedstoneEmitter.class);

        private RedstoneEmitter() {
        }
    }

    public static final class NetworkInterface {
        /**
         * Network bus participants, {@code oc2:network_interface}.
         * <p>
         * Register a block with this to have network connectors placed on it exchange ethernet frames
         * with it.
         */
        public static final BlockCapability<li.cil.oc2.api.capabilities.NetworkInterface, Direction> BLOCK =
                BlockCapability.createSided(ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "network_interface"),
                        li.cil.oc2.api.capabilities.NetworkInterface.class);

        private NetworkInterface() {
        }
    }

    public static final class TerminalUserProvider {
        /**
         * Terminal user lists provided by a block, {@code oc2:terminal_user_provider}.
         * <p>
         * Register a block with this to report the players currently using its terminal.
         */
        public static final BlockCapability<li.cil.oc2.api.capabilities.TerminalUserProvider, Direction> BLOCK =
                BlockCapability.createSided(ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "terminal_user_provider"),
                        li.cil.oc2.api.capabilities.TerminalUserProvider.class);

        /**
         * Terminal user lists provided by an entity, {@code oc2:terminal_user_provider}.
         * <p>
         * Register an entity with this to report the players currently using its terminal.
         */
        public static final EntityCapability<li.cil.oc2.api.capabilities.TerminalUserProvider, Direction> ENTITY =
                EntityCapability.createSided(ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "terminal_user_provider"),
                        li.cil.oc2.api.capabilities.TerminalUserProvider.class);

        private TerminalUserProvider() {
        }
    }

    public static final class Robot {
        /**
         * Device hosts that installed modules may interact with, {@code oc2:robot}.
         * <p>
         * Register an entity with this to let the modules installed in it interact with it.
         */
        public static final EntityCapability<li.cil.oc2.api.capabilities.Robot, Direction> ENTITY =
                EntityCapability.createSided(ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "robot"),
                        li.cil.oc2.api.capabilities.Robot.class);

        private Robot() {
        }
    }

    // ------------------------------------------------------------- //

    private Capabilities() {
    }
}
