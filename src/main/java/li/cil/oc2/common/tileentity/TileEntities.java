package li.cil.oc2.common.tileentity;

import li.cil.oc2.api.API;
import li.cil.oc2.common.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

public final class TileEntities {
    private static final DeferredRegister<BlockEntityType<?>> TILES = DeferredRegister.create(ForgeRegistries.TILE_ENTITIES, API.MOD_ID);

    ///////////////////////////////////////////////////////////////////

    public static final RegistryObject<BlockEntityType<RedstoneInterfaceBlockEntity>> REDSTONE_INTERFACE_TILE_ENTITY = register(Blocks.REDSTONE_INTERFACE, RedstoneInterfaceBlockEntity::new);
    public static final RegistryObject<BlockEntityType<BusCableBlockEntity>> BUS_CABLE_TILE_ENTITY = register(Blocks.BUS_CABLE, BusCableBlockEntity::new);
    public static final RegistryObject<BlockEntityType<ComputerBlockEntity>> COMPUTER_TILE_ENTITY = register(Blocks.COMPUTER, ComputerBlockEntity::new);
    public static final RegistryObject<BlockEntityType<NetworkConnectorBlockEntity>> NETWORK_CONNECTOR_TILE_ENTITY = register(Blocks.NETWORK_CONNECTOR, NetworkConnectorBlockEntity::new);
    public static final RegistryObject<BlockEntityType<NetworkHubBlockEntity>> NETWORK_HUB_TILE_ENTITY = register(Blocks.NETWORK_HUB, NetworkHubBlockEntity::new);
    public static final RegistryObject<BlockEntityType<DiskDriveBlockEntity>> DISK_DRIVE_TILE_ENTITY = register(Blocks.DISK_DRIVE, DiskDriveBlockEntity::new);
    public static final RegistryObject<BlockEntityType<ChargerBlockEntity>> CHARGER_TILE_ENTITY = register(Blocks.CHARGER, ChargerBlockEntity::new);
    public static final RegistryObject<BlockEntityType<CreativeEnergyBlockEntity>> CREATIVE_ENERGY_TILE_ENTITY = register(Blocks.CREATIVE_ENERGY, CreativeEnergyBlockEntity::new);

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
        TileEntities.TILES.register(FMLJavaModLoadingContext.get().getModEventBus());
    }

    ///////////////////////////////////////////////////////////////////

    @SuppressWarnings("ConstantConditions") // .build(null) is fine
    private static <B extends Block, T extends BlockEntity> RegistryObject<BlockEntityType<T>> register(final RegistryObject<B> block, final Supplier<T> factory) {
        return TILES.register(block.getId().getPath(), () -> BlockEntityType.Builder.of(factory, block.get()).build(null));
    }
}
