package li.cil.oc2.common.bus.device.provider.item;

import li.cil.oc2.api.bus.device.DeviceType;
import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.data.BlockDeviceData;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.common.Config;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.bus.device.item.HardDriveVMDeviceWithInitialData;
import li.cil.oc2.common.bus.device.provider.util.AbstractItemDeviceProvider;
import li.cil.oc2.common.item.HardDriveWithExternalDataItem;
import li.cil.oc2.common.util.LocationSupplierUtils;
import net.minecraft.item.ItemStack;

import java.util.Optional;

public final class HardDriveWithExternalDataItemDeviceProvider extends AbstractItemDeviceProvider {
    public HardDriveWithExternalDataItemDeviceProvider() {
        super(HardDriveWithExternalDataItem.class);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected Optional<ItemDevice> getItemDevice(final ItemDeviceQuery query) {
        final ItemStack stack = query.getItemStack();
        final HardDriveWithExternalDataItem item = (HardDriveWithExternalDataItem) stack.getItem();
        final BlockDeviceData data = item.getData(stack);
        if (data == null) {
            return Optional.empty();
        }

        return Optional.of(new HardDriveVMDeviceWithInitialData(stack, data.getBlockDevice(), false, LocationSupplierUtils.of(query)));
    }

    @Override
    protected Optional<DeviceType> getItemDeviceType(final ItemDeviceQuery query) {
        return Optional.of(DeviceTypes.HARD_DRIVE);
    }

    @Override
    protected int getItemDeviceEnergyConsumption(final ItemDeviceQuery query) {
        final ItemStack stack = query.getItemStack();
        final HardDriveWithExternalDataItem item = (HardDriveWithExternalDataItem) stack.getItem();
        final BlockDeviceData data = item.getData(stack);
        if (data == null) {
            return 0;
        }

        final long capacity = Math.min(data.getBlockDevice().getCapacity(), Math.max(0, Config.maxHardDriveSize));
        return Math.max(1, (int) Math.round(capacity * Config.hardDriveEnergyPerMegabytePerTick / Constants.MEGABYTE));
    }
}
