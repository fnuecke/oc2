/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.container;

import li.cil.oc2.common.util.RegistryUtils;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class Containers {
    private static final DeferredRegister<MenuType<?>> CONTAINERS = RegistryUtils.create(ForgeRegistries.CONTAINERS);

    ///////////////////////////////////////////////////////////////////

    public static final RegistryObject<MenuType<ComputerInventoryContainer>> COMPUTER = CONTAINERS.register("computer", () -> IForgeMenuType.create(ComputerInventoryContainer::createClient));
    public static final RegistryObject<MenuType<ComputerTerminalContainer>> COMPUTER_TERMINAL = CONTAINERS.register("computer_terminal", () -> IForgeMenuType.create(ComputerTerminalContainer::createClient));
    public static final RegistryObject<MenuType<RobotInventoryContainer>> ROBOT = CONTAINERS.register("robot", () -> IForgeMenuType.create(RobotInventoryContainer::createClient));
    public static final RegistryObject<MenuType<RobotTerminalContainer>> ROBOT_TERMINAL = CONTAINERS.register("robot_terminal", () -> IForgeMenuType.create(RobotTerminalContainer::createClient));
    public static final RegistryObject<MenuType<NetworkTunnelContainer>> NETWORK_TUNNEL = CONTAINERS.register("network_tunnel", () -> IForgeMenuType.create(NetworkTunnelContainer::createClient));

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
    }
}
