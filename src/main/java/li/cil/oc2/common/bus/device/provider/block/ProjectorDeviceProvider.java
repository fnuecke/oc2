package li.cil.oc2.common.bus.device.provider.block;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.api.util.Invalidatable;
import li.cil.oc2.common.block.ProjectorBlock;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.blockentity.ProjectorBlockEntity;
import li.cil.oc2.common.bus.device.provider.util.AbstractBlockEntityDeviceProvider;
import net.minecraft.core.Direction;

public final class ProjectorDeviceProvider extends AbstractBlockEntityDeviceProvider<ProjectorBlockEntity> {
    public ProjectorDeviceProvider() {
        super(BlockEntities.PROJECTOR.get());
    }

    ///////////////////////////////////////////////////////////////

    @Override
    protected Invalidatable<Device> getBlockDevice(final BlockDeviceQuery query, final ProjectorBlockEntity blockEntity) {
        final Direction blockFacing = blockEntity.getBlockState().getValue(ProjectorBlock.FACING);
        if (query.getQuerySide() != blockFacing &&
            query.getQuerySide() != blockFacing.getClockWise() &&
            query.getQuerySide() != blockFacing.getCounterClockWise()) {
            return Invalidatable.of(blockEntity.getProjectorDevice());
        } else {
            return Invalidatable.empty();
        }
    }
}
