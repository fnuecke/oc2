package li.cil.oc2.common.bus;

import li.cil.oc2.api.bus.DeviceBus;
import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.common.bus.device.DeviceInfo;
import li.cil.oc2.common.bus.device.Devices;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.util.ServerScheduler;
import li.cil.oc2.common.util.WorldUtils;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.LazyOptional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class TileEntityDeviceBusElement extends AbstractGroupingDeviceBusElement {
    private static final int NEIGHBOR_COUNT = 6;
    final Direction[] NEIGHBOR_DIRECTIONS = Direction.values();

    ///////////////////////////////////////////////////////////////////

    private final TileEntity tileEntity;

    ///////////////////////////////////////////////////////////////////

    public TileEntityDeviceBusElement(final TileEntity tileEntity) {
        super(NEIGHBOR_COUNT);
        this.tileEntity = tileEntity;
    }

    @Override
    public Optional<Collection<LazyOptional<DeviceBusElement>>> getNeighbors() {
        final World world = tileEntity.getWorld();
        if (world == null || world.isRemote()) {
            return Optional.empty();
        }

        final ArrayList<LazyOptional<DeviceBusElement>> neighbors = new ArrayList<>();
        for (final Direction neighborDirection : NEIGHBOR_DIRECTIONS) {
            final BlockPos neighborPos = tileEntity.getPos().offset(neighborDirection);

            final ChunkPos chunkPos = new ChunkPos(neighborPos);
            if (!world.chunkExists(chunkPos.x, chunkPos.z)) {
                return Optional.empty();
            }

            final TileEntity tileEntity = world.getTileEntity(neighborPos);
            if (tileEntity == null) {
                continue;
            }

            final LazyOptional<DeviceBusElement> capability = tileEntity.getCapability(Capabilities.DEVICE_BUS_ELEMENT_CAPABILITY, neighborDirection.getOpposite());
            if (capability.isPresent()) {
                neighbors.add(capability);
            }
        }

        return Optional.of(neighbors);
    }

    public void handleNeighborChanged(final BlockPos pos) {
        final World world = tileEntity.getWorld();
        if (world == null || world.isRemote()) {
            return;
        }

        final BlockPos toPos = pos.subtract(tileEntity.getPos());
        final Direction direction = Direction.byLong(toPos.getX(), toPos.getY(), toPos.getZ());
        if (direction == null) {
            return;
        }

        final int index = direction.getIndex();

        final HashSet<DeviceInfo> newDevices = new HashSet<>();
        if (canConnectToSide(direction)) {
            for (final LazyOptional<DeviceInfo> deviceInfo : Devices.getDevices(world, pos, direction)) {
                deviceInfo.ifPresent(newDevices::add);
                deviceInfo.addListener(unused -> handleNeighborChanged(pos));
            }
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
            final TileEntity tileEntity = WorldUtils.getTileEntityIfChunkExists(world, neighborPos);
            if (tileEntity == null) {
                continue;
            }

            final LazyOptional<DeviceBusElement> capability = tileEntity
                    .getCapability(Capabilities.DEVICE_BUS_ELEMENT_CAPABILITY, direction.getOpposite());
            capability.ifPresent(DeviceBus::scheduleScan);
        }
    }
}
