package li.cil.oc2.common.block;

import li.cil.oc2.api.API;
import net.minecraft.block.Block;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class Blocks {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, API.MOD_ID);

    ///////////////////////////////////////////////////////////////////

    public static final RegistryObject<ComputerBlock> COMPUTER = BLOCKS.register("computer", ComputerBlock::new);
    public static final RegistryObject<BusCableBlock> BUS_CABLE = BLOCKS.register("bus_cable", BusCableBlock::new);
    public static final RegistryObject<NetworkConnectorBlock> NETWORK_CONNECTOR = BLOCKS.register("network_connector", NetworkConnectorBlock::new);
    public static final RegistryObject<NetworkHubBlock> NETWORK_HUB = BLOCKS.register("network_hub", NetworkHubBlock::new);
    public static final RegistryObject<NetworkHubBlock> NETWORK_SWITCH = BLOCKS.register("network_switch", NetworkHubBlock::new);
    public static final RegistryObject<RedstoneInterfaceBlock> REDSTONE_INTERFACE = BLOCKS.register("redstone_interface", RedstoneInterfaceBlock::new);
    public static final RegistryObject<DiskDriveBlock> DISK_DRIVE = BLOCKS.register("disk_drive", DiskDriveBlock::new);
    public static final RegistryObject<ChargerBlock> CHARGER = BLOCKS.register("charger", ChargerBlock::new);
    public static final RegistryObject<CreativeEnergyBlock> CREATIVE_ENERGY = BLOCKS.register("creative_energy", CreativeEnergyBlock::new);

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
        BLOCKS.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
}
