package li.cil.oc2.common.bus;

import li.cil.oc2.api.bus.BlockDeviceBusElement;
import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.common.util.ServerScheduler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import java.util.Collection;
import java.util.HashSet;

public class TileEntityDeviceBusController extends CommonDeviceBusController {
    private final Runnable onBusChunkLoadedStateChanged = this::scheduleBusScan;
    private final HashSet<ChunkPos> trackedChunks = new HashSet<>();
    private final TileEntity tileEntity;

    ///////////////////////////////////////////////////////////////////

    public TileEntityDeviceBusController(final DeviceBusElement root, final TileEntity tileEntity) {
        super(root);
        this.tileEntity = tileEntity;
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void dispose() {
        super.dispose();

        removeListeners(trackedChunks);
        trackedChunks.clear();
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void onAfterBusScan() {
        super.onAfterBusScan();

        final World world = tileEntity.getWorld();
        if (world == null) {
            return;
        }

        final HashSet<ChunkPos> newTrackedChunks = new HashSet<>();
        for (final DeviceBusElement element : getElements()) {
            if (element instanceof BlockDeviceBusElement) {
                final BlockPos position = ((BlockDeviceBusElement) element).getPosition();
                newTrackedChunks.add(new ChunkPos(position));
                newTrackedChunks.add(new ChunkPos(position.offset(Direction.NORTH)));
                newTrackedChunks.add(new ChunkPos(position.offset(Direction.EAST)));
                newTrackedChunks.add(new ChunkPos(position.offset(Direction.SOUTH)));
                newTrackedChunks.add(new ChunkPos(position.offset(Direction.WEST)));
            }
        }

        // Do not track the chunk the controller itself is in -- this is unneeded because
        // we expect the controller to be disposed if its chunk is unloaded.
        newTrackedChunks.remove(new ChunkPos(tileEntity.getPos()));

        final HashSet<ChunkPos> removedChunks = new HashSet<>(trackedChunks);
        removedChunks.removeAll(newTrackedChunks);
        removeListeners(removedChunks);

        final HashSet<ChunkPos> addedChunks = new HashSet<>(newTrackedChunks);
        newTrackedChunks.removeAll(trackedChunks);
        addListeners(world, addedChunks);

        trackedChunks.removeAll(removedChunks);
        trackedChunks.addAll(newTrackedChunks);
    }

    ///////////////////////////////////////////////////////////////////

    private void addListeners(final World world, final Collection<ChunkPos> chunks) {
        for (final ChunkPos chunkPos : chunks) {
            ServerScheduler.scheduleOnLoad(world, chunkPos, onBusChunkLoadedStateChanged);
            ServerScheduler.scheduleOnUnload(world, chunkPos, onBusChunkLoadedStateChanged);
        }
    }

    private void removeListeners(final Collection<ChunkPos> chunks) {
        final World world = tileEntity.getWorld();
        for (final ChunkPos chunkPos : chunks) {
            ServerScheduler.cancelOnLoad(world, chunkPos, onBusChunkLoadedStateChanged);
            ServerScheduler.cancelOnUnload(world, chunkPos, onBusChunkLoadedStateChanged);
        }
    }
}
