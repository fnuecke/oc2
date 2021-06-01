package li.cil.oc2.common.container;

import li.cil.oc2.api.API;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class Containers {
    public static final MenuType<ComputerInventoryContainer> COMPUTER_CONTAINER = CONTAINERS.register("computer", () -> IForgeContainerType.create(ComputerInventoryContainer::create));
    public static final MenuType<ComputerTerminalContainer> COMPUTER_TERMINAL_CONTAINER = CONTAINERS.register("computer_terminal", () -> IForgeContainerType.create(ComputerTerminalContainer::create));
    public static final MenuType<RobotContainer> ROBOT_CONTAINER = CONTAINERS.register("robot", () -> IForgeContainerType.create(RobotContainer::create));
    public static final MenuType<RobotTerminalContainer> ROBOT_TERMINAL_CONTAINER = CONTAINERS.register("robot_terminal", () -> IForgeContainerType.create(RobotTerminalContainer::create));

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
        
    }
}
