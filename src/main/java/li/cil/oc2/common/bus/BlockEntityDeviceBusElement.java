package li.cil.oc2.common.bus;

import li.cil.oc2.api.bus.BlockDeviceBusElement;
import li.cil.oc2.api.bus.DeviceBus;
import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.bus.device.rpc.TypeNameRPCDevice;
import li.cil.oc2.common.bus.device.util.BlockDeviceInfo;
import li.cil.oc2.common.bus.device.util.Devices;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.util.LevelUtils;
import li.cil.oc2.common.util.ServerScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class BlockEntityDeviceBusElement extends AbstractGroupingBlockDeviceBusElement implements BlockDeviceBusElement {
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
        if (level == null || level.isClientSide()) {
            return;
        }

        final BlockPos toPos = pos.subtract(blockEntity.getBlockPos());
        final Direction direction = Direction.fromNormal(toPos.getX(), toPos.getY(), toPos.getZ());
        if (direction == null) {
            return;
        }

        final HashSet<BlockDeviceInfo> newDevices = collectDevices(level, pos, direction);

        final int index = direction.get3DDataValue();
        setDevicesForGroup(index, newDevices);
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

    protected HashSet<BlockDeviceInfo> collectDevices(final Level level, final BlockPos pos, @Nullable final Direction direction) {
        final HashSet<BlockDeviceInfo> newDevices = new HashSet<>();
        if (canDetectDevicesTowards(direction)) {
            final BlockDeviceQuery query = Devices.makeQuery(level, pos, direction);
            for (final LazyOptional<BlockDeviceInfo> deviceInfo : Devices.getDevices(query)) {
                deviceInfo.ifPresent(newDevices::add);
                deviceInfo.addListener(unused -> handleNeighborChanged(pos));
            }
        }

        collectSyntheticDevices(level, pos, direction, newDevices);

        return newDevices;
    }

    protected void collectSyntheticDevices(final Level level, final BlockPos pos, @Nullable final Direction direction, final HashSet<BlockDeviceInfo> devices) {
        final String blockName = LevelUtils.getBlockName(level, pos);
        if (blockName != null) {
            devices.add(new BlockDeviceInfo(null, new TypeNameRPCDevice(blockName)));
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
}
