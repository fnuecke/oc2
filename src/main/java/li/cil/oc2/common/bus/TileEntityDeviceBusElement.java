package li.cil.oc2.common.bus;

import li.cil.oc2.api.bus.DeviceBus;
import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.common.ServerScheduler;
import li.cil.oc2.common.device.CompoundDevice;
import li.cil.oc2.common.device.IdentifiableDeviceImpl;
import li.cil.oc2.common.device.Providers;
import li.cil.oc2.common.util.NBTTagIds;
import li.cil.oc2.common.util.TileEntityUtils;
import li.cil.oc2.common.util.WorldUtils;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class TileEntityDeviceBusElement implements INBTSerializable<CompoundNBT> {
    private static final String DEVICE_IDS_NBT_TAG_NAME = "deviceIds";
    private static final String DEVICE_ID_NBT_TAG_NAME = "deviceId";

    private static final int NEIGHBOR_COUNT = 6;

    private final TileEntity tileEntity;

    private final DeviceBusElementImpl busElement = new DeviceBusElementImpl();
    private final UUID[] deviceIds = new UUID[NEIGHBOR_COUNT];
    private final IdentifiableDeviceImpl[] devices = new IdentifiableDeviceImpl[NEIGHBOR_COUNT];

    public TileEntityDeviceBusElement(final TileEntity tileEntity) {
        this.tileEntity = tileEntity;

        for (int i = 0; i < NEIGHBOR_COUNT; i++) {
            deviceIds[i] = UUID.randomUUID();
        }
    }

    public DeviceBusElementImpl getBusElement() {
        return busElement;
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

        final LazyOptional<CompoundDevice> device = Providers.getDevice(world, pos, direction);
        final IdentifiableDeviceImpl identifiableDevice;

        if (device.isPresent()) {
            final String typeName = WorldUtils.getBlockName(world, pos);
            identifiableDevice = new IdentifiableDeviceImpl(device, deviceIds[index], typeName);
            device.addListener((ignored) -> handleNeighborChanged(pos));
        } else {
            identifiableDevice = null;
        }

        if (Objects.equals(devices[index], identifiableDevice)) {
            return;
        }

        if (devices[index] != null) {
            busElement.removeDevice(devices[index]);
        }

        devices[index] = identifiableDevice;

        if (devices[index] != null) {
            busElement.addDevice(devices[index]);
        }
    }

    public void initialize() {
        final World world = tileEntity.getWorld();
        if (world == null || world.isRemote()) {
            return;
        }

        ServerScheduler.schedule(world, () -> {
            if (tileEntity.isRemoved()) return;
            scanNeighborsForBusElements(tileEntity.getWorld());
            scanNeighborsForDevices();
        });
    }

    public void dispose() {
        busElement.scheduleScan();
    }

    @Override
    public CompoundNBT serializeNBT() {
        final ListNBT deviceIdsNbt = new ListNBT();
        for (int i = 0; i < NEIGHBOR_COUNT; i++) {
            final CompoundNBT deviceIdNbt = new CompoundNBT();
            deviceIdNbt.putUniqueId(DEVICE_ID_NBT_TAG_NAME, deviceIds[i]);
            deviceIdsNbt.add(deviceIdNbt);
        }

        final CompoundNBT compound = new CompoundNBT();
        compound.put(DEVICE_IDS_NBT_TAG_NAME, deviceIdsNbt);
        return compound;
    }

    @Override
    public void deserializeNBT(final CompoundNBT compound) {
        final ListNBT deviceIdsNbt = compound.getList(DEVICE_IDS_NBT_TAG_NAME, NBTTagIds.TAG_COMPOUND);
        for (int i = 0; i < Math.min(deviceIdsNbt.size(), NEIGHBOR_COUNT); i++) {
            final CompoundNBT deviceIdNbt = deviceIdsNbt.getCompound(i);
            if (deviceIdNbt.hasUniqueId(DEVICE_ID_NBT_TAG_NAME)) {
                deviceIds[i] = deviceIdNbt.getUniqueId(DEVICE_ID_NBT_TAG_NAME);
            }
        }
    }

    public CompoundNBT write(final CompoundNBT compound) {
        final ListNBT deviceIdsNbt = new ListNBT();
        for (int i = 0; i < NEIGHBOR_COUNT; i++) {
            final CompoundNBT deviceIdNbt = new CompoundNBT();
            deviceIdNbt.putUniqueId(DEVICE_ID_NBT_TAG_NAME, deviceIds[i]);
            deviceIdsNbt.add(deviceIdNbt);
        }
        compound.put(DEVICE_IDS_NBT_TAG_NAME, deviceIdsNbt);

        return compound;
    }

    private void scanNeighborsForDevices() {
        for (final Direction direction : Direction.values()) {
            handleNeighborChanged(tileEntity.getPos().offset(direction));
        }
    }

    private void scanNeighborsForBusElements(final World world) {
        final BlockPos pos = tileEntity.getPos();
        for (final Direction direction : Direction.values()) {
            final BlockPos neighborPos = pos.offset(direction);
            final TileEntity tileEntity = WorldUtils.getTileEntityIfChunkExists(world, neighborPos);
            if (tileEntity == null) {
                continue;
            }

            final Optional<DeviceBusElement> capability = TileEntityUtils.getInterfaceForSide(tileEntity, DeviceBusElement.class, direction.getOpposite());
            capability.ifPresent(DeviceBus::scheduleScan);
        }
    }
}
