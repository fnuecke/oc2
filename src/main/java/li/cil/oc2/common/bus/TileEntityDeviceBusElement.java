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
import li.cil.oc2.common.util.ServerScheduler;
import li.cil.oc2.common.util.WorldUtils;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class TileEntityDeviceBusElement extends AbstractGroupingBlockDeviceBusElement implements BlockDeviceBusElement {
    private final TileEntity tileEntity;

    ///////////////////////////////////////////////////////////////////

    public TileEntityDeviceBusElement(final TileEntity tileEntity) {
        super(Constants.BLOCK_FACE_COUNT);
        this.tileEntity = tileEntity;
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public IWorld getLevel() {
        return tileEntity.getLevel();
    }

    @Override
    public BlockPos getPosition() {
        return tileEntity.getBlockPos();
    }

    @Override
    public Optional<Collection<LazyOptional<DeviceBusElement>>> getNeighbors() {
        final World world = tileEntity.getLevel();
        if (world == null || world.isClientSide) {
            return Optional.empty();
        }

        final ArrayList<LazyOptional<DeviceBusElement>> neighbors = new ArrayList<>();
        for (final Direction neighborDirection : Constants.DIRECTIONS) {
            if (!canScanContinueTowards(neighborDirection)) {
                continue;
            }

            final BlockPos neighborPos = tileEntity.getBlockPos().relative(neighborDirection);

            final ChunkPos chunkPos = new ChunkPos(neighborPos);
            if (!world.hasChunk(chunkPos.x, chunkPos.z)) {
                return Optional.empty();
            }

            final TileEntity tileEntity = world.getBlockEntity(neighborPos);
            if (tileEntity == null) {
                continue;
            }

            final LazyOptional<DeviceBusElement> capability = tileEntity.getCapability(Capabilities.DEVICE_BUS_ELEMENT, neighborDirection.getOpposite());
            if (capability.isPresent()) {
                neighbors.add(capability);
            }
        }

        return Optional.of(neighbors);
    }

    public void handleNeighborChanged(final BlockPos pos) {
        final World world = tileEntity.getLevel();
        if (world == null || world.isClientSide) {
            return;
        }

        final BlockPos toPos = pos.subtract(tileEntity.getBlockPos());
        final Direction direction = Direction.fromNormal(toPos.getX(), toPos.getY(), toPos.getZ());
        if (direction == null) {
            return;
        }

        final int index = direction.get3DDataValue();

        final HashSet<BlockDeviceInfo> newDevices = new HashSet<>();
        if (canDetectDevicesTowards(direction)) {
            final BlockDeviceQuery query = Devices.makeQuery(world, pos, direction);
            for (final LazyOptional<BlockDeviceInfo> deviceInfo : Devices.getDevices(query)) {
                deviceInfo.ifPresent(newDevices::add);
                deviceInfo.addListener(unused -> handleNeighborChanged(pos));
            }
        }

        collectSyntheticDevices(world, pos, direction, newDevices);

        setDevicesForGroup(index, newDevices);
    }

    public void initialize() {
        final World world = requireNonNull(tileEntity.getLevel());
        ServerScheduler.schedule(world, () -> {
            if (tileEntity.isRemoved()) {
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

    protected void collectSyntheticDevices(final World world, final BlockPos pos, final Direction direction, final HashSet<BlockDeviceInfo> devices) {
        final String blockName = WorldUtils.getBlockName(world, pos);
        if (blockName != null) {
            devices.add(new BlockDeviceInfo(null, new TypeNameRPCDevice(blockName)));
        }
    }

    ///////////////////////////////////////////////////////////////////

    private void scanNeighborsForDevices() {
        for (final Direction direction : Constants.DIRECTIONS) {
            handleNeighborChanged(tileEntity.getBlockPos().relative(direction));
        }
    }

    private void scheduleBusScanInAdjacentBusElements() {
        final World world = requireNonNull(tileEntity.getLevel());
        final BlockPos pos = tileEntity.getBlockPos();
        for (final Direction direction : Constants.DIRECTIONS) {
            final BlockPos neighborPos = pos.relative(direction);
            final TileEntity tileEntity = WorldUtils.getBlockEntityIfChunkExists(world, neighborPos);
            if (tileEntity == null) {
                continue;
            }

            final LazyOptional<DeviceBusElement> capability = tileEntity
                    .getCapability(Capabilities.DEVICE_BUS_ELEMENT, direction.getOpposite());
            capability.ifPresent(DeviceBus::scheduleScan);
        }
    }
}
