/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.provider.item;

import li.cil.oc2.api.API;
import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.api.bus.device.vm.ArchitectureType;
import li.cil.oc2.common.Config;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.bus.device.item.FloppyDevice;
import li.cil.oc2.common.bus.device.provider.util.AbstractItemDeviceProvider;
import li.cil.oc2.common.item.FloppyItem;
import li.cil.oc2.common.util.LocationSupplierUtils;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public final class FloppyItemDeviceProvider extends AbstractItemDeviceProvider {
    public static final String DEVICE_DATA_KEY = API.MOD_ID + ":floppy";

    // --------------------------------------------------------------------- //

    public FloppyItemDeviceProvider() {
        super(FloppyItem.class);
    }

    // --------------------------------------------------------------------- //

    @Override
    protected Optional<ItemDevice> getItemDevice(final ItemDeviceQuery query) {
        final Optional<ArchitectureType> architectureType = query.getArchitectureType();
        if (architectureType.isEmpty()) {
            return Optional.empty();
        }

        final ItemStack stack = query.getItemStack();
        final FloppyItem item = (FloppyItem) stack.getItem();
        return Optional.of(new FloppyDevice(stack, architectureType.get(),
            item.getData(stack), LocationSupplierUtils.of(query)));
    }

    @Override
    protected int getItemDeviceEnergyConsumption(final ItemDeviceQuery query) {
        return Math.max(1, (int) Math.round(getCapacity(query) * Config.hardDriveEnergyPerMegabytePerTick / Constants.MEGABYTE));
    }

    // --------------------------------------------------------------------- //

    private static int getCapacity(final ItemDeviceQuery query) {
        final ItemStack stack = query.getItemStack();
        return ((FloppyItem) stack.getItem()).getCapacity(stack);
    }
}
