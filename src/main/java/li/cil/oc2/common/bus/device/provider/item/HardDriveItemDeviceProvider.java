package li.cil.oc2.common.bus.device.provider.item;

import li.cil.oc2.Config;
import li.cil.oc2.api.bus.device.DeviceType;
import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.data.BaseBlockDevice;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.common.bus.device.item.HardDriveVMDevice;
import li.cil.oc2.common.bus.device.item.SparseHardDriveVMDevice;
import li.cil.oc2.common.bus.device.provider.util.AbstractItemDeviceProvider;
import li.cil.oc2.common.init.Items;
import li.cil.oc2.common.item.HardDriveItem;
import li.cil.sedna.api.device.BlockDevice;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;

import java.util.Optional;

public final class HardDriveItemDeviceProvider extends AbstractItemDeviceProvider {
    public HardDriveItemDeviceProvider() {
        super(Items.HARD_DRIVE_ITEM);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected Optional<ItemDevice> getItemDevice(final ItemDeviceQuery query) {
        final ItemStack stack = query.getItemStack();

        final boolean readonly = HardDriveItem.isReadonly(stack);
        final BaseBlockDevice baseBlockDevice = HardDriveItem.getBaseBlockDevice(stack);
        if (baseBlockDevice != null) {
            final BlockDevice base = baseBlockDevice.get();
            return Optional.of(new SparseHardDriveVMDevice(stack, base, readonly));
        }

        final int size = MathHelper.clamp(HardDriveItem.getCapacity(stack), 0, Config.maxHardDriveSize);
        return Optional.of(new HardDriveVMDevice(stack, size, readonly));
    }

    @Override
    protected Optional<DeviceType> getItemDeviceType(final ItemDeviceQuery query) {
        return Optional.of(DeviceTypes.HARD_DRIVE);
    }
}
