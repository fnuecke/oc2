package li.cil.oc2.common.bus.device.provider.block;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.common.bus.device.provider.util.AbstractBlockEntityDeviceProvider;
import li.cil.oc2.common.tileentity.DiskDriveBlockEntity;
import li.cil.oc2.common.tileentity.TileEntities;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import java.util.Optional;

public final class DiskDriveDeviceProvider extends AbstractBlockEntityDeviceProvider<DiskDriveBlockEntity> {
    public DiskDriveDeviceProvider() {
        super(TileEntities.DISK_DRIVE_TILE_ENTITY);
    }

    @Override
    protected Optional<Device> getBlockDevice(final BlockDeviceQuery query, final DiskDriveBlockEntity tileEntity) {
        // We only allow connecting to exactly one face of the disk drive to ensure only one
        // bus (and thus, one VM) will access the device at any single time.
        if (query.getQuerySide() != tileEntity.getBlockState().getValue(HorizontalDirectionalBlock.FACING)) {
            return Optional.empty();
        }

        return Optional.of(tileEntity.getDevice());
    }
}
