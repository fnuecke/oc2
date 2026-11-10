/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.provider.item;

import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.data.BlockDeviceData;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.common.Config;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.bus.device.item.HardDriveDevice;
import li.cil.oc2.common.bus.device.item.HardDriveDeviceWithInitialData;
import li.cil.oc2.common.bus.device.provider.util.AbstractItemDeviceProvider;
import li.cil.oc2.common.item.HardDriveItem;
import li.cil.oc2.common.util.LocationSupplierUtils;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public final class HardDriveItemDeviceProvider extends AbstractItemDeviceProvider {
    public HardDriveItemDeviceProvider() {
        super(HardDriveItem.class);
    }

    // --------------------------------------------------------------------- //

    @Override
    protected Optional<ItemDevice> getItemDevice(final ItemDeviceQuery query) {
        final ItemStack stack = query.getItemStack();
        final BlockDeviceData data = ((HardDriveItem) stack.getItem()).getData(stack);
        return Optional.of(data != null
            ? new HardDriveDeviceWithInitialData(stack, data, false, LocationSupplierUtils.of(query))
            : new HardDriveDevice(stack, false, LocationSupplierUtils.of(query)));
    }

    @Override
    protected int getItemDeviceEnergyConsumption(final ItemDeviceQuery query) {
        return Math.max(1, (int) Math.round(getCapacity(query) * Config.hardDriveEnergyPerMegabytePerTick / Constants.MEGABYTE));
    }

    // --------------------------------------------------------------------- //

    private static int getCapacity(final ItemDeviceQuery query) {
        final ItemStack stack = query.getItemStack();
        return ((HardDriveItem) stack.getItem()).getCapacity(stack);
    }
}
