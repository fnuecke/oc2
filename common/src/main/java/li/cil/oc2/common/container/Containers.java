/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.container;

import li.cil.oc2.common.util.RegistryUtils;
import net.minecraft.world.inventory.MenuType;
import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import dev.architectury.registry.registries.RegistrySupplier;

public final class Containers {
    private static final DeferredRegister<MenuType<?>> CONTAINERS = RegistryUtils.getInitializerFor(Registries.MENU);

    ///////////////////////////////////////////////////////////////////

    public static final RegistrySupplier<MenuType<ComputerInventoryContainer>> COMPUTER = CONTAINERS.register("computer", () -> MenuRegistry.ofExtended(ComputerInventoryContainer::createClient));
    public static final RegistrySupplier<MenuType<ComputerTerminalContainer>> COMPUTER_TERMINAL = CONTAINERS.register("computer_terminal", () -> MenuRegistry.ofExtended(ComputerTerminalContainer::createClient));
    public static final RegistrySupplier<MenuType<RobotInventoryContainer>> ROBOT = CONTAINERS.register("robot", () -> MenuRegistry.ofExtended(RobotInventoryContainer::createClient));
    public static final RegistrySupplier<MenuType<RobotTerminalContainer>> ROBOT_TERMINAL = CONTAINERS.register("robot_terminal", () -> MenuRegistry.ofExtended(RobotTerminalContainer::createClient));
    public static final RegistrySupplier<MenuType<NetworkTunnelContainer>> NETWORK_TUNNEL = CONTAINERS.register("network_tunnel", () -> MenuRegistry.ofExtended(NetworkTunnelContainer::createClient));

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
    }
}
