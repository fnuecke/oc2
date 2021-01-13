package li.cil.oc2.common.container;

import li.cil.oc2.api.API;
import li.cil.oc2.common.Constants;
import net.minecraft.inventory.container.ContainerType;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class Containers {
    private static final DeferredRegister<ContainerType<?>> CONTAINERS = DeferredRegister.create(ForgeRegistries.CONTAINERS, API.MOD_ID);

    ///////////////////////////////////////////////////////////////////

    public static final RegistryObject<ContainerType<ComputerContainer>> COMPUTER_CONTAINER = CONTAINERS.register(Constants.COMPUTER_BLOCK_NAME, () -> IForgeContainerType.create(ComputerContainer::create));
    public static final RegistryObject<ContainerType<RobotContainer>> ROBOT_CONTAINER = CONTAINERS.register(Constants.ROBOT_ENTITY_NAME, () -> IForgeContainerType.create(RobotContainer::create));

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
        CONTAINERS.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
}
