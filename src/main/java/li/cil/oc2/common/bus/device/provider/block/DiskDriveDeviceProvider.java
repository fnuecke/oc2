package li.cil.oc2.common.bus.device.provider.block;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.common.bus.device.provider.util.AbstractBlockEntityDeviceProvider;
import li.cil.oc2.common.blockentity.DiskDriveBlockEntity;
import li.cil.oc2.common.blockentity.BlockEntities;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraftforge.common.util.LazyOptional;

public final class DiskDriveDeviceProvider extends AbstractBlockEntityDeviceProvider<DiskDriveBlockEntity> {
    public DiskDriveDeviceProvider() {
        super(BlockEntities.DISK_DRIVE.get());
    }

    @Override
    protected LazyOptional<Device> getBlockDevice(final BlockDeviceQuery query, final DiskDriveBlockEntity blockEntity) {
        // We only allow connecting to exactly one face of the disk drive to ensure only one
        // bus (and thus, one VM) will access the device at any single time.
        if (query.getQuerySide() != blockEntity.getBlockState().getValue(HorizontalDirectionalBlock.FACING)) {
            return LazyOptional.empty();
        }

        return LazyOptional.of(blockEntity::getDevice);
    }
}
