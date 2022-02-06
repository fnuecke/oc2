/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus;

import li.cil.oc2.api.bus.BlockDeviceBusElement;
import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.common.util.ChunkLocation;
import li.cil.oc2.common.util.ChunkUtils;
import li.cil.oc2.common.util.ServerScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Collection;
import java.util.HashSet;

public final class BlockEntityDeviceBusController extends CommonDeviceBusController {
    private final Runnable onBusChunkLoadedStateChanged = this::scheduleBusScan;
    private final HashSet<ChunkLocation> trackedChunks = new HashSet<>();
    private final BlockEntity blockEntity;

    ///////////////////////////////////////////////////////////////////

    public BlockEntityDeviceBusController(final DeviceBusElement root, final int baseEnergyConsumption, final BlockEntity blockEntity) {
        super(root, baseEnergyConsumption);
        this.blockEntity = blockEntity;
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void setDeviceContainersChanged() {
        super.setDeviceContainersChanged();
        for (final ChunkLocation trackedChunk : trackedChunks) {
            trackedChunk.tryGetLevel().ifPresent(level ->
                ChunkUtils.setLazyUnsaved(level, trackedChunk.position()));
        }
    }

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

        final Level level = blockEntity.getLevel();
        if (level == null) {
            return;
        }

        final HashSet<ChunkLocation> newTrackedChunks = new HashSet<>();
        for (final DeviceBusElement element : getElements()) {
            if (element instanceof final BlockDeviceBusElement blockElement) {
                final LevelAccessor elementLevel = blockElement.getLevel();
                final BlockPos elementPosition = blockElement.getPosition();
                if (elementLevel != null) {
                    newTrackedChunks.add(ChunkLocation.of(elementLevel, elementPosition));
                    newTrackedChunks.add(ChunkLocation.of(elementLevel, elementPosition.relative(Direction.NORTH)));
                    newTrackedChunks.add(ChunkLocation.of(elementLevel, elementPosition.relative(Direction.EAST)));
                    newTrackedChunks.add(ChunkLocation.of(elementLevel, elementPosition.relative(Direction.SOUTH)));
                    newTrackedChunks.add(ChunkLocation.of(elementLevel, elementPosition.relative(Direction.WEST)));
                }
            }
        }

        // Do not track the chunk the controller itself is in -- this is unneeded because
        // we expect the controller to be disposed if its chunk is unloaded.
        newTrackedChunks.remove(ChunkLocation.of(level, blockEntity.getBlockPos()));

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
                ServerScheduler.scheduleOnLoad(level, trackedChunk.position(), onBusChunkLoadedStateChanged);
                ServerScheduler.scheduleOnUnload(level, trackedChunk.position(), onBusChunkLoadedStateChanged);
            });
        }
    }

    private void removeListeners(final Collection<ChunkLocation> trackedChunks) {
        for (final ChunkLocation trackedChunk : trackedChunks) {
            trackedChunk.tryGetLevel().ifPresent(level -> {
                ServerScheduler.cancelOnLoad(level, trackedChunk.position(), onBusChunkLoadedStateChanged);
                ServerScheduler.cancelOnUnload(level, trackedChunk.position(), onBusChunkLoadedStateChanged);
            });
        }
    }
}
