package li.cil.oc2.common.init;

import li.cil.oc2.Constants;
import li.cil.oc2.common.block.entity.BusCableTileEntity;
import li.cil.oc2.common.block.entity.ComputerTileEntity;
import li.cil.oc2.common.block.entity.RedstoneInterfaceTileEntity;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.registry.Registry;

import java.util.function.Supplier;

public final class TileEntities {
    public static final BlockEntityType<RedstoneInterfaceTileEntity> REDSTONE_INTERFACE_TILE_ENTITY = create(Blocks.REDSTONE_INTERFACE_BLOCK, RedstoneInterfaceTileEntity::new);
    public static final BlockEntityType<BusCableTileEntity> BUS_CABLE_TILE_ENTITY = create(Blocks.BUS_CABLE_BLOCK, BusCableTileEntity::new);
    public static final BlockEntityType<ComputerTileEntity> COMPUTER_TILE_ENTITY = create(Blocks.COMPUTER_BLOCK, ComputerTileEntity::new);

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
        Registry.register(Registry.BLOCK_ENTITY_TYPE, Constants.REDSTONE_INTERFACE_BLOCK_NAME, REDSTONE_INTERFACE_TILE_ENTITY);
        Registry.register(Registry.BLOCK_ENTITY_TYPE, Constants.BUS_CABLE_BLOCK_NAME, BUS_CABLE_TILE_ENTITY);
        Registry.register(Registry.BLOCK_ENTITY_TYPE, Constants.COMPUTER_BLOCK_NAME, COMPUTER_TILE_ENTITY);
    }

    ///////////////////////////////////////////////////////////////////

    private static <T extends BlockEntity> BlockEntityType<T> create(final Block block, final Supplier<T> factory) {
        return BlockEntityType.Builder.create(factory, block).build(null);
    }
}
