package li.cil.oc2.common.block.entity;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

public abstract class AbstractTileEntity extends BlockEntity {
    protected ChunkPos chunkPos;

    ///////////////////////////////////////////////////////////////////

    protected AbstractTileEntity(final BlockEntityType<?> tileEntityType) {
        super(tileEntityType);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void setLocation(final World world, final BlockPos pos) {
        super.setLocation(world, pos);
        chunkPos = new ChunkPos(pos);
        initialize();
    }

    @Override
    public void markRemoved() {
        super.markRemoved();
        dispose();
    }

//    @Override
//    public void onChunkUnloaded() {
//        dispose();
//    }

    ///////////////////////////////////////////////////////////////////

    protected void initialize() {
        final World world = getWorld();
        if (world == null) {
            return;
        }

        if (world.isClient()) {
            initializeClient();
        } else {
            initializeServer();
        }
    }

    protected void dispose() {
        final World world = getWorld();
        if (world == null) {
            return;
        }
        if (world.isClient()) {
            disposeClient();
        } else {
            disposeServer();
        }
    }

    protected void initializeClient() {
    }

    protected void initializeServer() {
    }

    protected void disposeClient() {
    }

    protected void disposeServer() {
    }
}
