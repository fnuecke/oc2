package li.cil.oc2.common.container;

import li.cil.oc2.api.API;
import net.minecraft.inventory.container.ContainerType;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class Containers {
    private static final DeferredRegister<ContainerType<?>> CONTAINERS = DeferredRegister.create(ForgeRegistries.CONTAINERS, API.MOD_ID);

    ///////////////////////////////////////////////////////////////////

    public static final RegistryObject<ContainerType<ComputerInventoryContainer>> COMPUTER_CONTAINER = CONTAINERS.register("computer", () -> IForgeContainerType.create(ComputerInventoryContainer::create));
    public static final RegistryObject<ContainerType<ComputerTerminalContainer>> COMPUTER_TERMINAL_CONTAINER = CONTAINERS.register("computer_terminal", () -> IForgeContainerType.create(ComputerTerminalContainer::create));
    public static final RegistryObject<ContainerType<RobotContainer>> ROBOT_CONTAINER = CONTAINERS.register("robot", () -> IForgeContainerType.create(RobotContainer::create));
    public static final RegistryObject<ContainerType<RobotTerminalContainer>> ROBOT_TERMINAL_CONTAINER = CONTAINERS.register("robot_terminal", () -> IForgeContainerType.create(RobotTerminalContainer::create));

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
        CONTAINERS.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
}
