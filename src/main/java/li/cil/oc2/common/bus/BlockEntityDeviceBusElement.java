package li.cil.oc2.common.bus;

import li.cil.oc2.api.bus.BlockDeviceBusElement;
import li.cil.oc2.api.bus.DeviceBus;
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
import li.cil.oc2.common.util.ServerScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.registries.IForgeRegistry;

import javax.annotation.Nullable;
import java.util.*;

import static java.util.Objects.requireNonNull;
import static li.cil.oc2.common.util.RegistryUtils.optionalKey;

public class BlockEntityDeviceBusElement extends AbstractGroupingDeviceBusElement<BlockEntityDeviceBusElement.BlockEntry, BlockDeviceQuery> implements BlockDeviceBusElement {
    private final BlockEntity blockEntity;

    ///////////////////////////////////////////////////////////////////

    public BlockEntityDeviceBusElement(final BlockEntity blockEntity) {
        super(Constants.BLOCK_FACE_COUNT);
        this.blockEntity = blockEntity;
    }

    ///////////////////////////////////////////////////////////////////

    @Nullable
    @Override
    public LevelAccessor getLevel() {
        return blockEntity.getLevel();
    }

    @Override
    public BlockPos getPosition() {
        return blockEntity.getBlockPos();
    }

    @Override
    public Optional<Collection<LazyOptional<DeviceBusElement>>> getNeighbors() {
        final Level level = blockEntity.getLevel();
        if (level == null || level.isClientSide()) {
            return Optional.empty();
        }

        final ArrayList<LazyOptional<DeviceBusElement>> neighbors = new ArrayList<>();
        for (final Direction neighborDirection : Constants.DIRECTIONS) {
            if (!canScanContinueTowards(neighborDirection)) {
                continue;
            }

            final BlockPos neighborPos = blockEntity.getBlockPos().relative(neighborDirection);

            final ChunkPos chunkPos = new ChunkPos(neighborPos);
            if (!level.hasChunk(chunkPos.x, chunkPos.z)) {
                return Optional.empty();
            }

            final BlockEntity blockEntity = level.getBlockEntity(neighborPos);
            if (blockEntity == null) {
                continue;
            }

            final LazyOptional<DeviceBusElement> capability = blockEntity.getCapability(Capabilities.DEVICE_BUS_ELEMENT, neighborDirection.getOpposite());
            if (capability.isPresent()) {
                neighbors.add(capability);
            }
        }

        return Optional.of(neighbors);
    }

    public void handleNeighborChanged(final BlockPos pos) {
        final Level level = blockEntity.getLevel();
        if (level == null || level.isClientSide() || !level.isLoaded(pos)) {
            return;
        }

        final BlockPos toPos = pos.subtract(blockEntity.getBlockPos());
        final Direction direction = Direction.fromNormal(toPos.getX(), toPos.getY(), toPos.getZ());
        if (direction == null) {
            return;
        }

        final BlockQueryResult queryResult = collectDevices(level, pos, direction);

        final int index = direction.get3DDataValue();
        setEntriesForGroup(index, queryResult);
    }

    public void initialize() {
        final Level level = requireNonNull(blockEntity.getLevel());
        ServerScheduler.schedule(level, () -> {
            if (blockEntity.isRemoved()) {
                return;
            }

            scanNeighborsForDevices();
            scheduleBusScanInAdjacentBusElements();
        });
    }

    ///////////////////////////////////////////////////////////////////

    protected boolean canScanContinueTowards(@Nullable final Direction direction) {
        return true;
    }

    protected boolean canDetectDevicesTowards(@Nullable final Direction direction) {
        return canScanContinueTowards(direction);
    }

    protected BlockQueryResult collectDevices(final Level level, final BlockPos pos, @Nullable final Direction direction) {
        final BlockDeviceQuery query = Devices.makeQuery(level, pos, direction != null ? direction.getOpposite() : null);
        final HashSet<BlockEntry> entries = new HashSet<>();

        if (canDetectDevicesTowards(direction)) {
            for (final Invalidatable<BlockDeviceInfo> deviceInfo : Devices.getDevices(query)) {
                if (deviceInfo.isPresent()) {
                    entries.add(new BlockEntry(deviceInfo, pos));
                }
            }

            collectSyntheticDevices(level, pos, direction, entries);
        }

        return new BlockQueryResult(query, entries);
    }

    protected void collectSyntheticDevices(final Level level, final BlockPos pos, @Nullable final Direction direction, final HashSet<BlockEntry> entries) {
        final String blockName = LevelUtils.getBlockName(level, pos);
        if (blockName != null) {
            entries.add(new BlockEntry(new BlockDeviceInfo(null, new TypeNameRPCDevice(blockName)), pos));
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
        assert query != null : "Passed null query for block device bus element.";
        super.onEntryRemoved(dataKey, tag, query);
        final IForgeRegistry<BlockDeviceProvider> registry = Providers.BLOCK_DEVICE_PROVIDER_REGISTRY.get();
        final BlockDeviceProvider provider = registry.getValue(new ResourceLocation(dataKey));
        if (provider != null) {
            provider.unmount(query, tag);
        }
    }

    ///////////////////////////////////////////////////////////////////

    private void scanNeighborsForDevices() {
        for (final Direction direction : Constants.DIRECTIONS) {
            handleNeighborChanged(blockEntity.getBlockPos().relative(direction));
        }
    }

    private void scheduleBusScanInAdjacentBusElements() {
        final Level level = requireNonNull(blockEntity.getLevel());
        final BlockPos pos = blockEntity.getBlockPos();
        for (final Direction direction : Constants.DIRECTIONS) {
            final BlockPos neighborPos = pos.relative(direction);
            final BlockEntity blockEntity = LevelUtils.getBlockEntityIfChunkExists(level, neighborPos);
            if (blockEntity == null) {
                continue;
            }

            final LazyOptional<DeviceBusElement> capability = blockEntity
                .getCapability(Capabilities.DEVICE_BUS_ELEMENT, direction.getOpposite());
            capability.ifPresent(DeviceBus::scheduleScan);
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
        private final BlockPos pos;
        private Invalidatable.ListenerToken token;

        public BlockEntry(final Invalidatable<BlockDeviceInfo> deviceInfo, final BlockPos pos) {
            this.deviceInfo = deviceInfo;
            this.pos = pos;

            // Grab these while the device info has not yet been invalidated. We still need to access
            // these even after the device has been invalidated to clean up.
            this.dataKey = optionalKey(deviceInfo.get().provider).orElse(null);
            this.device = deviceInfo.get().device;
        }

        public BlockEntry(final BlockDeviceInfo deviceInfo, final BlockPos pos) {
            this(Invalidatable.of(deviceInfo), pos);
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
            if (token == null) {
                token = deviceInfo.addListener(unused -> handleNeighborChanged(pos));
            }
        }

        public void removeListener() {
            if (token != null) {
                token.removeListener();
                token = null;
            }
        }
    }
}
