/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus;

import li.cil.oc2.api.bus.BlockDeviceBusElement;
import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.common.util.ChunkLocation;
import li.cil.oc2.common.util.ChunkUtils;
import li.cil.oc2.common.util.ServerScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Collection;
import java.util.HashSet;

public final class BlockDeviceBusController extends CommonDeviceBusController {
    /**
     * Memoized closure for event callback, to allow unregistering it.
     */
    private final Runnable onBusChunkLoadedStateChanged = this::scheduleBusScan;

    /**
     * Chunks that contain bus elements. Used to mark these chunks dirty.
     * <p>
     * Specifically, this is used to mark chunks with bus interfaces dirty when a VM
     * using the bus is running, to ensure changes in devices will be persisted.
     */
    private final HashSet<ChunkLocation> busChunks = new HashSet<>();

    /**
     * Chunks that contain or are adjacent to bus elements. This is used to trigger bus
     * scans if these chunks load or unload, which is used to re-scan the bus, since we
     * only allow the bus to run when all chunks it (potentially) touches are loaded.
     */
    private final HashSet<ChunkLocation> trackedChunks = new HashSet<>();

    private final BlockEntity blockEntity;

    ///////////////////////////////////////////////////////////////////

    public BlockDeviceBusController(final DeviceBusElement root, final int baseEnergyConsumption, final BlockEntity blockEntity) {
        super(root, baseEnergyConsumption);
        this.blockEntity = blockEntity;
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void setDeviceContainersChanged() {
        super.setDeviceContainersChanged();
        for (final ChunkLocation location : busChunks) {
            location.tryGetLevel().ifPresent(level ->
                ChunkUtils.setLazyUnsaved(level, location.position()));
        }
    }

    @Override
    public void dispose() {
        super.dispose();

        removeListeners(trackedChunks);
        trackedChunks.clear();
        busChunks.clear();
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void onAfterBusScan() {
        super.onAfterBusScan();

        final LevelAccessor level = blockEntity.getLevel();
        if (level == null) {
            return;
        }

        busChunks.clear();
        final HashSet<ChunkLocation> newTrackedChunks = new HashSet<>();
        for (final DeviceBusElement element : getElements()) {
            if (element instanceof final BlockDeviceBusElement blockElement) {
                final LevelAccessor elementLevel = blockElement.getLevel();
                final BlockPos elementPosition = blockElement.getPosition();
                if (elementLevel != null) {
                    final ChunkLocation elementLocation = ChunkLocation.of(elementLevel, elementPosition);
                    busChunks.add(elementLocation);
                    newTrackedChunks.add(elementLocation);
                    newTrackedChunks.add(ChunkLocation.of(elementLevel, elementPosition.relative(Direction.NORTH)));
                    newTrackedChunks.add(ChunkLocation.of(elementLevel, elementPosition.relative(Direction.EAST)));
                    newTrackedChunks.add(ChunkLocation.of(elementLevel, elementPosition.relative(Direction.SOUTH)));
                    newTrackedChunks.add(ChunkLocation.of(elementLevel, elementPosition.relative(Direction.WEST)));
                }
            }
        }

        // Do not track the chunk the controller itself is in -- this is unneeded because
        // we expect the controller to be disposed if its chunk is unloaded.
        final ChunkLocation controllerChunkLocation = ChunkLocation.of(level, blockEntity.getBlockPos());
        busChunks.remove(controllerChunkLocation);
        newTrackedChunks.remove(controllerChunkLocation);

        final HashSet<ChunkLocation> removedChunks = new HashSet<>(trackedChunks);
        removedChunks.removeAll(newTrackedChunks);
        removeListeners(removedChunks);

        final HashSet<ChunkLocation> addedChunks = new HashSet<>(newTrackedChunks);
        newTrackedChunks.removeAll(trackedChunks);
        addListeners(addedChunks);

        trackedChunks.removeAll(removedChunks);
        trackedChunks.addAll(newTrackedChunks);
    }

    ///////////////////////////////////////////////////////////////////

    private void addListeners(final Collection<ChunkLocation> trackedChunks) {
        for (final ChunkLocation trackedChunk : trackedChunks) {
            trackedChunk.tryGetLevel().ifPresent(level -> {
                ServerScheduler.subscribeOnLoad(level, trackedChunk.position(), onBusChunkLoadedStateChanged);
                ServerScheduler.subscribeOnUnload(level, trackedChunk.position(), onBusChunkLoadedStateChanged);
            });
        }
    }

    private void removeListeners(final Collection<ChunkLocation> trackedChunks) {
        for (final ChunkLocation trackedChunk : trackedChunks) {
            trackedChunk.tryGetLevel().ifPresent(level -> {
                ServerScheduler.unsubscribeOnLoad(level, trackedChunk.position(), onBusChunkLoadedStateChanged);
                ServerScheduler.unsubscribeOnUnload(level, trackedChunk.position(), onBusChunkLoadedStateChanged);
            });
        }
    }
}
