/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.fabric;

import li.cil.oc2.api.API;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.lookup.v1.entity.EntityApiLookup;
import net.fabricmc.fabric.api.lookup.v1.item.ItemApiLookup;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/**
 * The Fabric lookups OpenComputers II provides.
 */
public final class Lookups {
    public static final class Device {
        /**
         * Devices provided by a block, {@code oc2:device}.
         * <p>
         * Register a block with this to expose it as a device to an adjacent device bus.
         */
        public static final BlockApiLookup<li.cil.oc2.api.bus.device.Device, Direction> BLOCK =
                BlockApiLookup.get(ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "device"),
                        li.cil.oc2.api.bus.device.Device.class, Direction.class);

        /**
         * Devices provided by an item, {@code oc2:device}.
         * <p>
         * Register an item with this to expose it as a device while it is installed in a device slot.
         */
        public static final ItemApiLookup<li.cil.oc2.api.bus.device.Device, Void> ITEM =
                ItemApiLookup.get(ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "device"),
                        li.cil.oc2.api.bus.device.Device.class, Void.class);

        private Device() {
        }
    }

    public static final class DeviceBusElement {
        /**
         * Device bus connection points, {@code oc2:device_bus_element}.
         * <p>
         * Register a block with this to make it part of a device bus, e.g. to provide a custom cable.
         */
        public static final BlockApiLookup<li.cil.oc2.api.bus.DeviceBusElement, Direction> BLOCK =
                BlockApiLookup.get(ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "device_bus_element"),
                        li.cil.oc2.api.bus.DeviceBusElement.class, Direction.class);

        private DeviceBusElement() {
        }
    }

    public static final class RedstoneEmitter {
        /**
         * Redstone signal sources, {@code oc2:redstone_emitter}.
         * <p>
         * Register a block with this to have it emit a redstone signal on behalf of a device.
         */
        public static final BlockApiLookup<li.cil.oc2.api.capabilities.RedstoneEmitter, Direction> BLOCK =
                BlockApiLookup.get(ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "redstone_emitter"),
                        li.cil.oc2.api.capabilities.RedstoneEmitter.class, Direction.class);

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
        public static final BlockApiLookup<li.cil.oc2.api.capabilities.NetworkInterface, Direction> BLOCK =
                BlockApiLookup.get(ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "network_interface"),
                        li.cil.oc2.api.capabilities.NetworkInterface.class, Direction.class);

        private NetworkInterface() {
        }
    }

    public static final class TerminalUserProvider {
        /**
         * Terminal user lists provided by a block, {@code oc2:terminal_user_provider}.
         * <p>
         * Register a block with this to report the players currently using its terminal.
         */
        public static final BlockApiLookup<li.cil.oc2.api.capabilities.TerminalUserProvider, Direction> BLOCK =
                BlockApiLookup.get(ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "terminal_user_provider"),
                        li.cil.oc2.api.capabilities.TerminalUserProvider.class, Direction.class);

        /**
         * Terminal user lists provided by an entity, {@code oc2:terminal_user_provider}.
         * <p>
         * Register an entity with this to report the players currently using its terminal.
         */
        public static final EntityApiLookup<li.cil.oc2.api.capabilities.TerminalUserProvider, Direction> ENTITY =
                EntityApiLookup.get(ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "terminal_user_provider"),
                        li.cil.oc2.api.capabilities.TerminalUserProvider.class, Direction.class);

        private TerminalUserProvider() {
        }
    }

    public static final class Robot {
        /**
         * Device hosts that installed modules may interact with, {@code oc2:robot}.
         * <p>
         * Register an entity with this to let the modules installed in it interact with it.
         */
        public static final EntityApiLookup<li.cil.oc2.api.capabilities.Robot, Direction> ENTITY =
                EntityApiLookup.get(ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "robot"),
                        li.cil.oc2.api.capabilities.Robot.class, Direction.class);

        private Robot() {
        }
    }

    // ------------------------------------------------------------- //

    private Lookups() {
    }
}
