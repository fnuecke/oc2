package li.cil.oc2.common.bus;

import li.cil.oc2.api.bus.BlockDeviceBusElement;
import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.common.util.ServerScheduler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

public final class TileEntityDeviceBusController extends CommonDeviceBusController {
    private final Runnable onBusChunkLoadedStateChanged = this::scheduleBusScan;
    private final HashSet<TrackedChunk> trackedChunks = new HashSet<>();
    private final TileEntity tileEntity;

    ///////////////////////////////////////////////////////////////////

    public TileEntityDeviceBusController(final DeviceBusElement root, final int baseEnergyConsumption, final TileEntity tileEntity) {
        super(root, baseEnergyConsumption);
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

        final World world = tileEntity.getLevel();
        if (world == null) {
            return;
        }

        final HashSet<TrackedChunk> newTrackedChunks = new HashSet<>();
        for (final DeviceBusElement element : getElements()) {
            if (element instanceof BlockDeviceBusElement) {
                final BlockDeviceBusElement blockElement = (BlockDeviceBusElement) element;
                final IWorld elementWorld = blockElement.getLevel();
                final BlockPos elementPosition = blockElement.getPosition();
                newTrackedChunks.add(new TrackedChunk(elementWorld, elementPosition));
                newTrackedChunks.add(new TrackedChunk(elementWorld, elementPosition.relative(Direction.NORTH)));
                newTrackedChunks.add(new TrackedChunk(elementWorld, elementPosition.relative(Direction.EAST)));
                newTrackedChunks.add(new TrackedChunk(elementWorld, elementPosition.relative(Direction.SOUTH)));
                newTrackedChunks.add(new TrackedChunk(elementWorld, elementPosition.relative(Direction.WEST)));
            }
        }

        // Do not track the chunk the controller itself is in -- this is unneeded because
        // we expect the controller to be disposed if its chunk is unloaded.
        newTrackedChunks.remove(new TrackedChunk(world, tileEntity.getBlockPos()));

        final HashSet<TrackedChunk> removedChunks = new HashSet<>(trackedChunks);
        removedChunks.removeAll(newTrackedChunks);
        removeListeners(removedChunks);

        final HashSet<TrackedChunk> addedChunks = new HashSet<>(newTrackedChunks);
        newTrackedChunks.removeAll(trackedChunks);
        addListeners(addedChunks);

        trackedChunks.removeAll(removedChunks);
        trackedChunks.addAll(newTrackedChunks);
    }

    ///////////////////////////////////////////////////////////////////

    private void addListeners(final Collection<TrackedChunk> trackedChunks) {
        for (final TrackedChunk trackedChunk : trackedChunks) {
            final IWorld world = trackedChunk.world.get();
            ServerScheduler.scheduleOnLoad(world, trackedChunk.position, onBusChunkLoadedStateChanged);
            ServerScheduler.scheduleOnUnload(world, trackedChunk.position, onBusChunkLoadedStateChanged);
        }
    }

    private void removeListeners(final Collection<TrackedChunk> trackedChunks) {
        for (final TrackedChunk trackedChunk : trackedChunks) {
            final IWorld world = trackedChunk.world.get();
            if (world != null) {
                ServerScheduler.cancelOnLoad(world, trackedChunk.position, onBusChunkLoadedStateChanged);
                ServerScheduler.cancelOnUnload(world, trackedChunk.position, onBusChunkLoadedStateChanged);
            }
        }
    }

    private static final class TrackedChunk {
        public final WeakReference<IWorld> world;
        public final ChunkPos position;

        private TrackedChunk(final IWorld world, final BlockPos position) {
            this.world = new WeakReference<>(world);
            this.position = new ChunkPos(position);
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final TrackedChunk that = (TrackedChunk) o;
            return world.equals(that.world) && position.equals(that.position);
        }

        @Override
        public int hashCode() {
            return Objects.hash(world, position);
        }
    }
}
