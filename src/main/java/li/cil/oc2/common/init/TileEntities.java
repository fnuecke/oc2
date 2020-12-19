package li.cil.oc2.common.init;

import li.cil.oc2.Constants;
import li.cil.oc2.api.API;
import li.cil.oc2.common.block.entity.BusCableTileEntity;
import li.cil.oc2.common.block.entity.ComputerTileEntity;
import li.cil.oc2.common.block.entity.RedstoneInterfaceTileEntity;
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

    public static final RegistryObject<TileEntityType<RedstoneInterfaceTileEntity>> REDSTONE_INTERFACE_TILE_ENTITY = register(Constants.REDSTONE_INTERFACE_BLOCK_NAME, Blocks.REDSTONE_INTERFACE_BLOCK, RedstoneInterfaceTileEntity::new);
    public static final RegistryObject<TileEntityType<BusCableTileEntity>> BUS_CABLE_TILE_ENTITY = register(Constants.BUS_CABLE_BLOCK_NAME, Blocks.BUS_CABLE_BLOCK, BusCableTileEntity::new);
    public static final RegistryObject<TileEntityType<ComputerTileEntity>> COMPUTER_TILE_ENTITY = register(Constants.COMPUTER_BLOCK_NAME, Blocks.COMPUTER_BLOCK, ComputerTileEntity::new);

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
        TileEntities.TILES.register(FMLJavaModLoadingContext.get().getModEventBus());
    }

    ///////////////////////////////////////////////////////////////////

    private static <T extends TileEntity> RegistryObject<TileEntityType<T>> register(final String name, final RegistryObject<Block> block, final Supplier<T> factory) {
        return TILES.register(name, () -> TileEntityType.Builder.create(factory, block.get()).build(null));
    }
}
