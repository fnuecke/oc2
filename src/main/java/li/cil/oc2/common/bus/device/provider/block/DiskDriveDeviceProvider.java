package li.cil.oc2.common.bus.device.provider.block;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.common.bus.device.provider.util.AbstractTileEntityDeviceProvider;
import li.cil.oc2.common.tileentity.DiskDriveTileEntity;
import li.cil.oc2.common.tileentity.TileEntities;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraftforge.common.util.LazyOptional;

public final class DiskDriveDeviceProvider extends AbstractTileEntityDeviceProvider<DiskDriveTileEntity> {
    public DiskDriveDeviceProvider() {
        super(TileEntities.DISK_DRIVE_TILE_ENTITY.get());
    }

    @Override
    protected LazyOptional<Device> getBlockDevice(final BlockDeviceQuery query, final DiskDriveTileEntity tileEntity) {
        // We only allow connecting to exactly one face of the disk drive to ensure only one
        // bus (and thus, one VM) will access the device at any single time.
        if (query.getQuerySide() != tileEntity.getBlockState().getValue(HorizontalDirectionalBlock.FACING)) {
            return LazyOptional.empty();
        }

        return LazyOptional.of(tileEntity::getDevice);
    }
}
