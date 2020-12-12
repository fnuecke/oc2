package li.cil.oc2.common.bus;

import li.cil.ceres.api.Serialized;
import li.cil.oc2.api.bus.Device;
import li.cil.oc2.api.bus.DeviceBus;
import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.common.bus.device.provider.Providers;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.util.ServerScheduler;
import li.cil.oc2.common.util.WorldUtils;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.LazyOptional;

import java.util.*;

import static java.util.Objects.requireNonNull;

public class TileEntityDeviceBusElement extends AbstractDeviceBusElement {
    private static final int NEIGHBOR_COUNT = 6;

    ///////////////////////////////////////////////////////////////////

    private final TileEntity tileEntity;
    private final ArrayList<HashSet<Device>> sidedDevices = new ArrayList<>(6);

    ///////////////////////////////////////////////////////////////////

    @Serialized private final UUID[] sidedDeviceIds = new UUID[NEIGHBOR_COUNT];

    ///////////////////////////////////////////////////////////////////

    public TileEntityDeviceBusElement(final TileEntity tileEntity) {
        this.tileEntity = tileEntity;

        for (int i = 0; i < NEIGHBOR_COUNT; i++) {
            sidedDevices.add(new HashSet<>());
            sidedDeviceIds[i] = UUID.randomUUID();
        }
    }

    @Override
    public Optional<UUID> getDeviceIdentifier(final Device device) {
        for (int i = 0; i < NEIGHBOR_COUNT; i++) {
            if (sidedDevices.get(i).contains(device)) {
                return Optional.of(sidedDeviceIds[i]);
            }
        }
        return super.getDeviceIdentifier(device);
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

        final HashSet<Device> newDevices = new HashSet<>();
        if (canConnectToSide(direction)) {
            for (final LazyOptional<Device> device : Providers.getDevices(world, pos, direction)) {
                device.ifPresent(newDevices::add);
                device.addListener(unused -> handleNeighborChanged(pos));
            }
        }

        final HashSet<Device> devicesOnSide = sidedDevices.get(index);
        if (Objects.equals(newDevices, devicesOnSide)) {
            return;
        }

        devices.removeAll(devicesOnSide);
        devicesOnSide.clear();
        devicesOnSide.addAll(newDevices);
        devices.addAll(devicesOnSide);
        scanDevices();
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
