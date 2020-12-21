package li.cil.oc2.common.bus;

import alexiil.mc.lib.attributes.SearchOptions;
import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.common.bus.device.BlockDeviceInfo;
import li.cil.oc2.common.bus.device.Devices;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.util.ServerScheduler;
import li.cil.oc2.common.util.WorldUtils;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class TileEntityDeviceBusElement extends AbstractGroupingBlockDeviceBusElement {
    private static final int NEIGHBOR_COUNT = 6;
    final Direction[] NEIGHBOR_DIRECTIONS = Direction.values();

    ///////////////////////////////////////////////////////////////////

    private final BlockEntity tileEntity;

    ///////////////////////////////////////////////////////////////////

    public TileEntityDeviceBusElement(final BlockEntity tileEntity) {
        super(NEIGHBOR_COUNT);
        this.tileEntity = tileEntity;
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public Optional<Collection<DeviceBusElement>> getNeighbors() {
        final World world = tileEntity.getWorld();
        if (world == null || world.isClient()) {
            return Optional.empty();
        }

        final BlockPos pos = tileEntity.getPos();

        final ArrayList<DeviceBusElement> neighbors = new ArrayList<>();
        for (final Direction neighborDirection : NEIGHBOR_DIRECTIONS) {
            if (!canConnectToSide(neighborDirection)) {
                continue;
            }

            final BlockPos neighborPos = pos.offset(neighborDirection);

            final ChunkPos chunkPos = new ChunkPos(neighborPos);
            if (!world.isChunkLoaded(chunkPos.x, chunkPos.z)) {
                return Optional.empty();
            }

            final DeviceBusElement capability = Capabilities.DEVICE_BUS_ELEMENT_CAPABILITY
                    .getFirstOrNull(world, pos, SearchOptions.inDirection(neighborDirection));
            if (capability != null) {
                neighbors.add(capability);
            }
        }

        return Optional.of(neighbors);
    }

    public void handleNeighborChanged(final BlockPos pos) {
        final World world = tileEntity.getWorld();
        if (world == null || world.isClient()) {
            return;
        }

        final BlockPos toPos = pos.subtract(tileEntity.getPos());
        final Direction direction = Direction.fromVector(toPos.getX(), toPos.getY(), toPos.getZ());
        if (direction == null) {
            return;
        }

        final int index = direction.getId();

        final HashSet<BlockDeviceInfo> newDevices = new HashSet<>();
        if (canConnectToSide(direction)) {
            newDevices.addAll(Devices.getDevices(world, pos, direction));
        }

        setDevicesForGroup(index, newDevices);
    }

    public void initialize() {
        final World world = requireNonNull(tileEntity.getWorld());
        ServerScheduler.schedule(world, () -> {
            if (tileEntity.isRemoved()) {
                return;
            }

            scanNeighborsForDevices();
            scheduleBusScanInAdjacentBusElements();
        });
    }

    public void dispose() {
        scheduleScan();
    }

    ///////////////////////////////////////////////////////////////////

    protected boolean canConnectToSide(final Direction direction) {
        return true;
    }

    ///////////////////////////////////////////////////////////////////

    private void scanNeighborsForDevices() {
        for (final Direction direction : Direction.values()) {
            handleNeighborChanged(tileEntity.getPos().offset(direction));
        }
    }

    private void scheduleBusScanInAdjacentBusElements() {
        final World world = requireNonNull(tileEntity.getWorld());
        final BlockPos pos = tileEntity.getPos();
        for (final Direction direction : Direction.values()) {
            final BlockPos neighborPos = pos.offset(direction);
            final BlockEntity tileEntity = WorldUtils.getTileEntityIfChunkExists(world, neighborPos);
            if (tileEntity == null) {
                continue;
            }

            final DeviceBusElement capability = Capabilities.DEVICE_BUS_ELEMENT_CAPABILITY
                    .getFirstOrNull(world, pos, SearchOptions.inDirection(direction));
            if (capability != null) {
                capability.scheduleScan();
            }
        }
    }
}
