/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus;

import li.cil.oc2.api.bus.BlockDeviceBusElement;
import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.provider.BlockDeviceProvider;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.api.util.Invalidatable;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.bus.device.provider.Providers;
import li.cil.oc2.common.bus.device.rpc.TypeNameRPCDevice;
import li.cil.oc2.common.bus.device.util.BlockDeviceInfo;
import li.cil.oc2.common.bus.device.util.Devices;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.util.LevelUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.registries.IForgeRegistry;

import javax.annotation.Nullable;
import java.util.*;

import static li.cil.oc2.common.util.RegistryUtils.optionalKey;

public abstract class AbstractBlockDeviceBusElement extends AbstractGroupingDeviceBusElement<AbstractBlockDeviceBusElement.BlockEntry, BlockDeviceQuery> implements BlockDeviceBusElement {
    public AbstractBlockDeviceBusElement() {
        super(Constants.BLOCK_FACE_COUNT);
    }

    ///////////////////////////////////////////////////////////////////
    // DeviceBusElement

    @Override
    public Optional<Collection<LazyOptional<DeviceBusElement>>> getNeighbors() {
        final LevelAccessor level = getLevel();
        if (level == null || level.isClientSide()) {
            return Optional.empty();
        }

        final ArrayList<LazyOptional<DeviceBusElement>> neighbors = new ArrayList<>();
        for (final Direction neighborDirection : Constants.DIRECTIONS) {
            if (!canScanContinueTowards(neighborDirection)) {
                continue;
            }

            final BlockPos neighborPos = getPosition().relative(neighborDirection);

            final ChunkPos chunkPos = new ChunkPos(neighborPos);
            if (!level.hasChunk(chunkPos.x, chunkPos.z)) {
                return Optional.empty();
            }

            final BlockEntity blockEntity = level.getBlockEntity(neighborPos);
            if (blockEntity == null) {
                continue;
            }

            final LazyOptional<DeviceBusElement> capability = blockEntity.getCapability(Capabilities.deviceBusElement(), neighborDirection.getOpposite());
            if (capability.isPresent()) {
                neighbors.add(capability);
            }
        }

        return Optional.of(neighbors);
    }

    ///////////////////////////////////////////////////////////////////

    public void updateDevicesForNeighbor(final Direction side) {
        final LevelAccessor level = getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }

        final int index = side.get3DDataValue();
        collectDevices(level, getPosition().relative(side), side).ifPresentOrElse(
            queryResult -> setEntriesForGroup(index, queryResult),
            () -> setEntriesForGroupUnloaded(index)
        );
    }

    public void setRemoved() {
        final LevelAccessor level = getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }

        for (final Direction side : Direction.values()) {
            final int index = side.get3DDataValue();
            final BlockPos pos = getPosition().relative(side);
            final BlockDeviceQuery query = Devices.makeQuery(level, pos, side.getOpposite());
            setEntriesForGroup(index, new BlockQueryResult(query, Collections.emptySet()));
        }

        scheduleScan();
    }

    ///////////////////////////////////////////////////////////////////

    protected boolean canScanContinueTowards(@Nullable final Direction direction) {
        return true;
    }

    protected boolean canDetectDevicesTowards(@Nullable final Direction direction) {
        return canScanContinueTowards(direction);
    }

    protected Optional<BlockQueryResult> collectDevices(final LevelAccessor level, final BlockPos pos, @Nullable final Direction side) {
        final BlockDeviceQuery query = Devices.makeQuery(level, pos, side != null ? side.getOpposite() : null);
        final HashSet<BlockEntry> entries = new HashSet<>();

        if (canDetectDevicesTowards(side)) {
            final Optional<List<Invalidatable<BlockDeviceInfo>>> loadedDevices = Devices.getDevices(query);
            if (loadedDevices.isPresent()) {
                for (final Invalidatable<BlockDeviceInfo> deviceInfo : loadedDevices.get()) {
                    if (deviceInfo.isPresent()) {
                        entries.add(new BlockEntry(deviceInfo, side));
                    }
                }
            } else {
                return Optional.empty();
            }

            collectSyntheticDevices(level, pos, side, entries);
        }

        return Optional.of(new BlockQueryResult(query, entries));
    }

    protected void collectSyntheticDevices(final LevelAccessor level, final BlockPos pos, @Nullable final Direction side, final HashSet<BlockEntry> entries) {
        if (entries.isEmpty()) {
            return;
        }

        final String blockName = LevelUtils.getBlockName(level, pos);
        if (blockName != null) {
            entries.add(new BlockEntry(new BlockDeviceInfo(null, new TypeNameRPCDevice(blockName)), side));
        }
    }

    @Override
    protected void onEntryAdded(final BlockEntry entry) {
        super.onEntryAdded(entry);
        entry.addListener();
    }

    @Override
    protected void onEntryRemoved(final BlockEntry entry) {
        super.onEntryRemoved(entry);
        entry.removeListener();
    }

    @Override
    protected void onEntryRemoved(final String dataKey, final CompoundTag tag, @Nullable final BlockDeviceQuery query) {
        super.onEntryRemoved(dataKey, tag, query);
        assert query != null : "Passed null query for block device bus element.";
        final IForgeRegistry<BlockDeviceProvider> registry = Providers.blockDeviceProviderRegistry();
        final BlockDeviceProvider provider = registry.getValue(new ResourceLocation(dataKey));
        if (provider != null) {
            provider.unmount(query, tag);
        }
    }

    ///////////////////////////////////////////////////////////////////

    protected final class BlockQueryResult extends QueryResult {
        private final BlockDeviceQuery query;
        private final Set<BlockEntry> entries;

        public BlockQueryResult(final BlockDeviceQuery query, final Set<BlockEntry> entries) {
            this.query = query;
            this.entries = entries;
        }

        public BlockDeviceQuery getQuery() {
            return query;
        }

        @Override
        public Set<BlockEntry> getEntries() {
            return entries;
        }
    }

    protected final class BlockEntry implements Entry {
        private final Invalidatable<BlockDeviceInfo> deviceInfo;
        @Nullable private final String dataKey;
        private final Device device;
        @Nullable private final Direction side;
        private Invalidatable.ListenerToken token;

        public BlockEntry(final Invalidatable<BlockDeviceInfo> deviceInfo, @Nullable final Direction side) {
            this.deviceInfo = deviceInfo;
            this.side = side;

            // Grab these while the device info has not yet been invalidated. We still need to access
            // these even after the device has been invalidated to clean up.
            this.dataKey = optionalKey(deviceInfo.get().provider).orElse(null);
            this.device = deviceInfo.get().device;
        }

        public BlockEntry(final BlockDeviceInfo deviceInfo, @Nullable final Direction side) {
            this(Invalidatable.of(deviceInfo), side);
        }

        @Override
        public Optional<String> getDeviceDataKey() {
            return Optional.ofNullable(dataKey);
        }

        @Override
        public OptionalInt getDeviceEnergyConsumption() {
            return deviceInfo.isPresent() ? OptionalInt.of(deviceInfo.get().getEnergyConsumption()) : OptionalInt.empty();
        }

        @Override
        public Device getDevice() {
            return device;
        }

        public void addListener() {
            // Side can be null for the block that owns the bus element, e.g. in the computer, where the
            // block adds itself. In this case, we can skip the listener, since the bus element's existence
            // and validity is tightly coupled to the device source anyway.
            if (token == null && side != null) {
                token = deviceInfo.addListener(unused -> updateDevicesForNeighbor(side));
            }
        }

        public void removeListener() {
            if (token != null) {
                token.removeListener();
                token = null;
            }
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final BlockEntry that = (BlockEntry) o;
            return Objects.equals(dataKey, that.dataKey) && device.equals(that.device) && side == that.side;
        }

        @Override
        public int hashCode() {
            return Objects.hash(dataKey, device, side);
        }

        @Override
        public String toString() {
            return device.toString();
        }
    }
}
