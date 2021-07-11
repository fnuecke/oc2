package li.cil.oc2.common.container;

import li.cil.oc2.common.util.RegistryUtils;
import net.minecraft.inventory.container.ContainerType;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class Containers {
    private static final DeferredRegister<ContainerType<?>> CONTAINERS = RegistryUtils.create(ForgeRegistries.CONTAINERS);

    ///////////////////////////////////////////////////////////////////

    public static final RegistryObject<ContainerType<ComputerInventoryContainer>> COMPUTER = CONTAINERS.register("computer", () -> IForgeContainerType.create(ComputerInventoryContainer::create));
    public static final RegistryObject<ContainerType<ComputerTerminalContainer>> COMPUTER_TERMINAL = CONTAINERS.register("computer_terminal", () -> IForgeContainerType.create(ComputerTerminalContainer::create));
    public static final RegistryObject<ContainerType<RobotContainer>> ROBOT = CONTAINERS.register("robot", () -> IForgeContainerType.create(RobotContainer::create));
    public static final RegistryObject<ContainerType<RobotTerminalContainer>> ROBOT_TERMINAL = CONTAINERS.register("robot_terminal", () -> IForgeContainerType.create(RobotTerminalContainer::create));

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
    }
}
