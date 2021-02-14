package li.cil.oc2.common.block;

import li.cil.oc2.api.API;
import li.cil.oc2.common.Constants;
import net.minecraft.block.Block;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class Blocks {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, API.MOD_ID);

    ///////////////////////////////////////////////////////////////////

    public static final RegistryObject<ComputerBlock> COMPUTER = BLOCKS.register(Constants.COMPUTER_BLOCK_NAME, ComputerBlock::new);
    public static final RegistryObject<BusCableBlock> BUS_CABLE = BLOCKS.register(Constants.BUS_CABLE_BLOCK_NAME, BusCableBlock::new);
    public static final RegistryObject<NetworkConnectorBlock> NETWORK_CONNECTOR = BLOCKS.register(Constants.NETWORK_CONNECTOR_BLOCK_NAME, NetworkConnectorBlock::new);
    public static final RegistryObject<NetworkHubBlock> NETWORK_HUB = BLOCKS.register(Constants.NETWORK_HUB_BLOCK_NAME, NetworkHubBlock::new);
    public static final RegistryObject<RedstoneInterfaceBlock> REDSTONE_INTERFACE = BLOCKS.register(Constants.REDSTONE_INTERFACE_BLOCK_NAME, RedstoneInterfaceBlock::new);
    public static final RegistryObject<DiskDriveBlock> DISK_DRIVE = BLOCKS.register(Constants.DISK_DRIVE_BLOCK_NAME, DiskDriveBlock::new);
    public static final RegistryObject<ChargerBlock> CHARGER = BLOCKS.register(Constants.CHARGER_BLOCK_NAME, ChargerBlock::new);
    public static final RegistryObject<CreativeEnergyBlock> CREATIVE_ENERGY = BLOCKS.register(Constants.CREATIVE_ENERGY_BLOCK_NAME, CreativeEnergyBlock::new);

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
        BLOCKS.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
}
