package li.cil.oc2.common.bus.device.provider.block;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.api.util.Invalidatable;
import li.cil.oc2.common.block.DiskDriveBlock;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.blockentity.DiskDriveBlockEntity;
import li.cil.oc2.common.bus.device.provider.util.AbstractBlockEntityDeviceProvider;
import net.minecraft.core.Direction;

public final class DiskDriveDeviceProvider extends AbstractBlockEntityDeviceProvider<DiskDriveBlockEntity> {
    public DiskDriveDeviceProvider() {
        super(BlockEntities.DISK_DRIVE.get());
    }

    ///////////////////////////////////////////////////////////////////

    // NB: Does *not* need an unmount() implementation, because the blob UUID is stored on the item.

    ///////////////////////////////////////////////////////////////////

    @Override
    protected Invalidatable<Device> getBlockDevice(final BlockDeviceQuery query, final DiskDriveBlockEntity blockEntity) {
        // We only allow connecting to exactly one face of the disk drive to ensure only one
        // bus (and thus, one VM) will access the device at any single time.
        final Direction blockFacing = blockEntity.getBlockState().getValue(DiskDriveBlock.FACING);
        if (query.getQuerySide() == blockFacing.getOpposite()) {
            return Invalidatable.of(blockEntity.getDevice());
        } else {
            return Invalidatable.empty();
        }
    }
}
