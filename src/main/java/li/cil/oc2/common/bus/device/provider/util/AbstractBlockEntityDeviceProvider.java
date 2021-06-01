package li.cil.oc2.common.bus.device.provider.util;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.common.util.WorldUtils;
import net.minecraft.tileentity.BlockEntity;
import net.minecraft.tileentity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.util.Optional;

import java.util.Optional;

public abstract class AbstractBlockEntityDeviceProvider<T extends BlockEntity> extends AbstractBlockDeviceProvider {
    private final BlockEntityType<T> tileEntityType;

    ///////////////////////////////////////////////////////////////////

    protected AbstractBlockEntityDeviceProvider(final BlockEntityType<T> tileEntityType) {
        this.tileEntityType = tileEntityType;
    }

    protected AbstractBlockEntityDeviceProvider() {
        this.tileEntityType = null;
    }

    ///////////////////////////////////////////////////////////////////

    @SuppressWarnings("unchecked")
    @Override
    public final Optional<Device> getDevice(final BlockDeviceQuery query) {
        final BlockEntity tileEntity = WorldUtils.getBlockEntityIfChunkExists(query.getLevel(), query.getQueryPosition());
        if (tileEntity == null) {
            return Optional.empty();
        }

        if (tileEntityType != null && tileEntity.getType() != tileEntityType) {
            return Optional.empty();
        }

        return getBlockDevice(query, (T) tileEntity);
    }

    ///////////////////////////////////////////////////////////////////

    protected abstract Optional<Device> getBlockDevice(final BlockDeviceQuery query, final T tileEntity);
}
