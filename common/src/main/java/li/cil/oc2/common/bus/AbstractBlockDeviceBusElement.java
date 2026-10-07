/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus;

import li.cil.oc2.api.bus.BlockDeviceBusElement;
import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.bus.device.TypeNameDevice;
import li.cil.oc2.common.bus.device.util.BlockDeviceInfo;
import li.cil.oc2.common.bus.device.util.Devices;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.util.LevelUtils;
import li.cil.oc2.common.util.ServerScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;

import javax.annotation.Nullable;
import java.util.*;

import static li.cil.oc2.common.bus.device.provider.Providers.optionalKey;

public abstract class AbstractBlockDeviceBusElement extends AbstractGroupingDeviceBusElement<AbstractBlockDeviceBusElement.BlockEntry> implements BlockDeviceBusElement {
    private final DeviceBusElement[] neighborElements = new DeviceBusElement[Constants.BLOCK_FACE_COUNT];
    private final Capabilities.InvalidationHandle[] neighborHandles = new Capabilities.InvalidationHandle[Constants.BLOCK_FACE_COUNT];
    private final boolean[] hasScheduledNeighborUpdate = new boolean[Constants.BLOCK_FACE_COUNT];

    // --------------------------------------------------------------------- //

    public AbstractBlockDeviceBusElement() {
        super(Constants.BLOCK_FACE_COUNT);
    }

    // --------------------------------------------------------------------- //
    // DeviceBusElement

    @Override
    public Optional<Collection<DeviceBusElement>> getNeighbors() {
        if (!(getLevel() instanceof final ServerLevel level)) {
            return Optional.empty();
        }

        final ArrayList<DeviceBusElement> neighbors = new ArrayList<>();
        for (final Direction neighborDirection : Constants.DIRECTIONS) {
            if (!canScanContinueTowards(neighborDirection)) {
                continue;
            }

            final BlockPos neighborPos = getPosition().relative(neighborDirection);

            final ChunkPos chunkPos = new ChunkPos(neighborPos);
            if (!level.hasChunk(chunkPos.x, chunkPos.z)) {
                return Optional.empty();
            }

            listenToNeighbor(level, neighborDirection);

            final DeviceBusElement neighbor = Capabilities.get(level, neighborPos, Capabilities.DEVICE_BUS_ELEMENT, neighborDirection.getOpposite());
            neighborElements[neighborDirection.get3DDataValue()] = neighbor;
            if (neighbor != null) {
                neighbors.add(neighbor);
            }
        }

        return Optional.of(neighbors);
    }

    @Override
    public void invalidateDevices() {
        for (final Direction side : Direction.values()) {
            updateDevicesForNeighbor(side);
        }
    }

    // --------------------------------------------------------------------- //

    public void updateDevicesForNeighbor(final Direction side) {
        if (!(getLevel() instanceof final ServerLevel level)) {
            return;
        }

        final int index = side.get3DDataValue();
        final BlockPos neighborPos = getPosition().relative(side);

        // Without a controller we have no architecture, which can result in some
        // providers returning nothing or something different to what's saved.
        // In this state, we might discard persisted data of devices that don't get
        // provided in the current state. When a controller is added, we re-run this
        // anyway, so it's fine to just skip it for now.
        if (getControllers().isEmpty()) {
            // BUT! If this is a neighbor chunk unloading, we have to save the device
            // state of the devices in it, or we'll lose it .
            final ChunkPos neighborChunk = new ChunkPos(neighborPos);
            if (!level.hasChunk(neighborChunk.x, neighborChunk.z)) {
                setEntriesForGroupUnloaded(index);
            }
        } else {
            if (canDetectDevicesTowards(side)) {
                listenToNeighbor(level, side);
            }
            collectDevices(level, neighborPos, side).ifPresentOrElse(
                entries -> setEntriesForGroup(index, entries),
                () -> setEntriesForGroupUnloaded(index)
            );
        }
    }

    public void dropNeighborHandles() {
        for (final Capabilities.InvalidationHandle handle : neighborHandles) {
            if (handle != null) {
                handle.drop();
            }
        }
        Arrays.fill(neighborHandles, null);
        Arrays.fill(hasScheduledNeighborUpdate, false);
        Arrays.fill(neighborElements, null);
    }

    public void setRemoved() {
        dropNeighborHandles();

        final LevelAccessor level = getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }

        for (final Direction side : Direction.values()) {
            setEntriesForGroup(side.get3DDataValue(), Collections.emptySet());
        }

        scheduleScan();
    }

    // --------------------------------------------------------------------- //

    protected boolean canScanContinueTowards(@Nullable final Direction direction) {
        return true;
    }

    protected boolean canDetectDevicesTowards(@Nullable final Direction direction) {
        return canScanContinueTowards(direction);
    }

    protected Optional<Set<BlockEntry>> collectDevices(final ServerLevel level, final BlockPos pos, @Nullable final Direction side) {
        final BlockDeviceQuery query = Devices.makeQuery(getArchitectureType().orElse(null), level, pos, side != null ? side.getOpposite() : null);
        final HashSet<BlockEntry> entries = new HashSet<>();

        if (canDetectDevicesTowards(side)) {
            final Optional<List<BlockDeviceInfo>> loadedDevices = Devices.getDevices(query);
            if (loadedDevices.isPresent()) {
                for (final BlockDeviceInfo deviceInfo : loadedDevices.get()) {
                    entries.add(new BlockEntry(deviceInfo, side));
                }
            } else {
                return Optional.empty();
            }

            collectSyntheticDevices(level, pos, side, entries);
        }

        return Optional.of(entries);
    }

    protected void collectSyntheticDevices(final ServerLevel level, final BlockPos pos, @Nullable final Direction side, final HashSet<BlockEntry> entries) {
        if (entries.isEmpty()) {
            return;
        }

        final String blockName = LevelUtils.getBlockName(level, pos);
        if (blockName != null) {
            entries.add(new BlockEntry(new BlockDeviceInfo(null, new TypeNameDevice(blockName)), side));
        }
    }

    // --------------------------------------------------------------------- //

    private void listenToNeighbor(final ServerLevel level, final Direction side) {
        final int index = side.get3DDataValue();
        if (neighborHandles[index] == null) {
            neighborHandles[index] = Capabilities.listen(level, getPosition().relative(side), () -> {
                neighborHandles[index] = null;
                scheduleNeighborUpdate(level, side);
            });
        }
    }

    // Deferred: callbacks run inside the loader's capability invalidation, which must not re-query capabilities.
    private void scheduleNeighborUpdate(final ServerLevel level, final Direction side) {
        final int index = side.get3DDataValue();
        if (hasScheduledNeighborUpdate[index]) {
            return;
        }

        hasScheduledNeighborUpdate[index] = true;
        final ChunkPos chunk = new ChunkPos(getPosition());
        ServerScheduler.schedule(level, () -> {
            if (!hasScheduledNeighborUpdate[index] || !level.hasChunk(chunk.x, chunk.z)) {
                return;
            }

            hasScheduledNeighborUpdate[index] = false;
            if (canScanContinueTowards(side)) {
                listenToNeighbor(level, side);

                final BlockPos neighborPos = getPosition().relative(side);
                final ChunkPos neighborChunk = new ChunkPos(neighborPos);
                final DeviceBusElement neighbor = level.hasChunk(neighborChunk.x, neighborChunk.z)
                    ? Capabilities.get(level, neighborPos, Capabilities.DEVICE_BUS_ELEMENT, side.getOpposite())
                    : null;
                if (neighbor != neighborElements[index]) {
                    scheduleScan();
                }
            }
            updateDevicesForNeighbor(side);
        }, 1);
    }

    // --------------------------------------------------------------------- //

    protected static final class BlockEntry implements Entry {
        private final BlockDeviceInfo deviceInfo;
        @Nullable
        private final String dataKey;
        @Nullable
        private final Direction side;

        public BlockEntry(final BlockDeviceInfo deviceInfo, @Nullable final Direction side) {
            this.deviceInfo = deviceInfo;
            this.side = side;
            this.dataKey = optionalKey(deviceInfo.provider).orElse(null);
        }

        @Override
        public Optional<String> getDeviceDataKey() {
            return Optional.ofNullable(dataKey);
        }

        @Override
        public OptionalInt getDeviceEnergyConsumption() {
            return OptionalInt.of(deviceInfo.getEnergyConsumption());
        }

        @Override
        public Device getDevice() {
            return deviceInfo.device;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final BlockEntry that = (BlockEntry) o;
            return Objects.equals(dataKey, that.dataKey) && deviceInfo.device.equals(that.deviceInfo.device) && side == that.side;
        }

        @Override
        public int hashCode() {
            return Objects.hash(dataKey, deviceInfo.device, side);
        }

        @Override
        public String toString() {
            return deviceInfo.device.toString();
        }
    }
}
