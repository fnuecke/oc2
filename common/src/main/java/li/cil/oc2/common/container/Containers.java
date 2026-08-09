/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.container;

import li.cil.oc2.common.util.RegistryUtils;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import dev.architectury.registry.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import dev.architectury.registry.registries.RegistrySupplier;

public final class Containers {
    private static final DeferredRegister<MenuType<?>> CONTAINERS = RegistryUtils.getInitializerFor(Registries.MENU);

    ///////////////////////////////////////////////////////////////////

    public static final RegistrySupplier<MenuType<ComputerInventoryContainer>> COMPUTER = CONTAINERS.register("computer", () -> IForgeMenuType.create(ComputerInventoryContainer::createClient));
    public static final RegistrySupplier<MenuType<ComputerTerminalContainer>> COMPUTER_TERMINAL = CONTAINERS.register("computer_terminal", () -> IForgeMenuType.create(ComputerTerminalContainer::createClient));
    public static final RegistrySupplier<MenuType<RobotInventoryContainer>> ROBOT = CONTAINERS.register("robot", () -> IForgeMenuType.create(RobotInventoryContainer::createClient));
    public static final RegistrySupplier<MenuType<RobotTerminalContainer>> ROBOT_TERMINAL = CONTAINERS.register("robot_terminal", () -> IForgeMenuType.create(RobotTerminalContainer::createClient));
    public static final RegistrySupplier<MenuType<NetworkTunnelContainer>> NETWORK_TUNNEL = CONTAINERS.register("network_tunnel", () -> IForgeMenuType.create(NetworkTunnelContainer::createClient));

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
    }
}
