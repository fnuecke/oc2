package li.cil.oc2.common.tileentity;

import li.cil.oc2.api.API;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

public final class TileEntities {
    private static final DeferredRegister<TileEntityType<?>> TILES = DeferredRegister.create(ForgeRegistries.TILE_ENTITIES, API.MOD_ID);

    ///////////////////////////////////////////////////////////////////

    public static final RegistryObject<TileEntityType<RedstoneInterfaceTileEntity>> REDSTONE_INTERFACE_TILE_ENTITY = register(Constants.REDSTONE_INTERFACE_BLOCK_NAME, Blocks.REDSTONE_INTERFACE, RedstoneInterfaceTileEntity::new);
    public static final RegistryObject<TileEntityType<BusCableTileEntity>> BUS_CABLE_TILE_ENTITY = register(Constants.BUS_CABLE_BLOCK_NAME, Blocks.BUS_CABLE, BusCableTileEntity::new);
    public static final RegistryObject<TileEntityType<ComputerTileEntity>> COMPUTER_TILE_ENTITY = register(Constants.COMPUTER_BLOCK_NAME, Blocks.COMPUTER, ComputerTileEntity::new);
    public static final RegistryObject<TileEntityType<NetworkConnectorTileEntity>> NETWORK_CONNECTOR_TILE_ENTITY = register(Constants.NETWORK_CONNECTOR_BLOCK_NAME, Blocks.NETWORK_CONNECTOR, NetworkConnectorTileEntity::new);
    public static final RegistryObject<TileEntityType<NetworkHubTileEntity>> NETWORK_HUB_TILE_ENTITY = register(Constants.NETWORK_HUB_BLOCK_NAME, Blocks.NETWORK_HUB, NetworkHubTileEntity::new);

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
        TileEntities.TILES.register(FMLJavaModLoadingContext.get().getModEventBus());
    }

    ///////////////////////////////////////////////////////////////////

    @SuppressWarnings("ConstantConditions") // .build(null) is fine
    private static <B extends Block, T extends TileEntity> RegistryObject<TileEntityType<T>> register(final String name, final RegistryObject<B> block, final Supplier<T> factory) {
        return TILES.register(name, () -> TileEntityType.Builder.create(factory, block.get()).build(null));
    }
}
