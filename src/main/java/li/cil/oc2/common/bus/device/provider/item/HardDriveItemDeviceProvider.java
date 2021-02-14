package li.cil.oc2.common.bus.device.provider.item;

import li.cil.oc2.api.bus.device.DeviceType;
import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.data.BlockDeviceData;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.common.Config;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.bus.device.item.HardDriveVMDevice;
import li.cil.oc2.common.bus.device.item.SparseHardDriveVMDevice;
import li.cil.oc2.common.bus.device.provider.util.AbstractItemDeviceProvider;
import li.cil.oc2.common.item.AbstractBlockDeviceItem;
import li.cil.oc2.common.item.HardDriveItem;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.util.LocationSupplierUtils;
import li.cil.sedna.api.device.BlockDevice;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;

import java.util.Optional;

public final class HardDriveItemDeviceProvider extends AbstractItemDeviceProvider {
    public HardDriveItemDeviceProvider() {
        super(Items.HARD_DRIVE);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected Optional<ItemDevice> getItemDevice(final ItemDeviceQuery query) {
        final ItemStack stack = query.getItemStack();

        final boolean readonly = AbstractBlockDeviceItem.isReadonly(stack);
        final BlockDeviceData handler = AbstractBlockDeviceItem.getData(stack);
        if (handler != null) {
            final BlockDevice base = handler.getBlockDevice();
            return Optional.of(new SparseHardDriveVMDevice(stack, base, readonly, LocationSupplierUtils.of(query)));
        }

        final int size = MathHelper.clamp(AbstractBlockDeviceItem.getCapacity(stack), 0, Config.maxHardDriveSize);
        return Optional.of(new HardDriveVMDevice(stack, size, readonly, LocationSupplierUtils.of(query)));
    }

    @Override
    protected Optional<DeviceType> getItemDeviceType(final ItemDeviceQuery query) {
        return Optional.of(DeviceTypes.HARD_DRIVE);
    }

    @Override
    protected int getItemDeviceEnergyConsumption(final ItemDeviceQuery query) {
        final BlockDeviceData data = HardDriveItem.getData(query.getItemStack());
        final long capacity;
        if (data != null) {
            capacity = data.getBlockDevice().getCapacity();
        } else {
            capacity = HardDriveItem.getCapacity(query.getItemStack());
        }
        return Math.max(1, (int) Math.round(capacity * Config.hardDriveEnergyPerMegabytePerTick / Constants.MEGABYTE));
    }
}
