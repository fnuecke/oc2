package li.cil.oc2.common.tileentity;

import li.cil.oc2.common.block.Blocks;
import li.cil.oc2.common.util.RegistryUtils;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class TileEntities {
    private static final DeferredRegister<BlockEntityType<?>> TILES = RegistryUtils.create(ForgeRegistries.BLOCK_ENTITIES);

    ///////////////////////////////////////////////////////////////////

    public static final RegistryObject<BlockEntityType<RedstoneInterfaceTileEntity>> REDSTONE_INTERFACE_TILE_ENTITY = register(Blocks.REDSTONE_INTERFACE, RedstoneInterfaceTileEntity::new);
    public static final RegistryObject<BlockEntityType<BusCableTileEntity>> BUS_CABLE_TILE_ENTITY = register(Blocks.BUS_CABLE, BusCableTileEntity::new);
    public static final RegistryObject<BlockEntityType<ComputerTileEntity>> COMPUTER_TILE_ENTITY = register(Blocks.COMPUTER, ComputerTileEntity::new);
    public static final RegistryObject<BlockEntityType<NetworkConnectorTileEntity>> NETWORK_CONNECTOR_TILE_ENTITY = register(Blocks.NETWORK_CONNECTOR, NetworkConnectorTileEntity::new);
    public static final RegistryObject<BlockEntityType<NetworkHubTileEntity>> NETWORK_HUB_TILE_ENTITY = register(Blocks.NETWORK_HUB, NetworkHubTileEntity::new);
    public static final RegistryObject<BlockEntityType<DiskDriveTileEntity>> DISK_DRIVE_TILE_ENTITY = register(Blocks.DISK_DRIVE, DiskDriveTileEntity::new);
    public static final RegistryObject<BlockEntityType<ChargerTileEntity>> CHARGER_TILE_ENTITY = register(Blocks.CHARGER, ChargerTileEntity::new);
    public static final RegistryObject<BlockEntityType<CreativeEnergyTileEntity>> CREATIVE_ENERGY_TILE_ENTITY = register(Blocks.CREATIVE_ENERGY, CreativeEnergyTileEntity::new);

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
    }

    ///////////////////////////////////////////////////////////////////

    @SuppressWarnings("ConstantConditions") // .build(null) is fine
    private static <B extends Block, T extends BlockEntity> RegistryObject<BlockEntityType<T>> register(final RegistryObject<B> block, final BlockEntityType.BlockEntitySupplier<T> factory) {
        return TILES.register(block.getId().getPath(), () -> BlockEntityType.Builder.of(factory, block.get()).build(null));
    }
}
