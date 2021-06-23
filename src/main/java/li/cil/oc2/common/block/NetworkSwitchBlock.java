package li.cil.oc2.common.block;

import li.cil.oc2.common.tileentity.TileEntities;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockReader;

public class NetworkSwitchBlock extends NetworkHubBlock {
    @Override
    public TileEntity createTileEntity(final BlockState state, final IBlockReader world) {
        return TileEntities.NETWORK_SWITCH_TILE_ENTITY.get().create();
    }
}
