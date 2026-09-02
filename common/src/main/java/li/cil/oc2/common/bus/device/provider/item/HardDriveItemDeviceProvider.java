/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.provider.item;

import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.data.BlockDeviceData;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.common.Config;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.bus.device.provider.util.AbstractItemDeviceProvider;
import li.cil.oc2.common.bus.device.vm.item.HardDriveDevice;
import li.cil.oc2.common.bus.device.vm.item.HardDriveDeviceWithInitialData;
import li.cil.oc2.common.item.HardDriveItem;
import li.cil.oc2.common.util.LocationSupplierUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Optional;

public final class HardDriveItemDeviceProvider extends AbstractItemDeviceProvider {
    public HardDriveItemDeviceProvider() {
        super(HardDriveItem.class);
    }

    // --------------------------------------------------------------------- //

    @Override
    public void disposeMissing(@Nullable final ItemDeviceQuery query, final CompoundTag tag) {
        super.disposeMissing(query, tag);
        HardDriveDevice.unmount(tag);
    }

    // --------------------------------------------------------------------- //

    @Override
    protected Optional<ItemDevice> getItemDevice(final ItemDeviceQuery query) {
        final ItemStack stack = query.getItemStack();
        final BlockDeviceData data = getData(query);
        return Optional.of(data != null
            ? new HardDriveDeviceWithInitialData(stack, data.getBlockDevice(), false, LocationSupplierUtils.of(query))
            : new HardDriveDevice(stack, getCapacity(query), false, LocationSupplierUtils.of(query)));
    }

    @Override
    protected int getItemDeviceEnergyConsumption(final ItemDeviceQuery query) {
        return Math.max(1, (int) Math.round(getCapacity(query) * Config.hardDriveEnergyPerMegabytePerTick / Constants.MEGABYTE));
    }

    // --------------------------------------------------------------------- //

    @Nullable
    private static BlockDeviceData getData(final ItemDeviceQuery query) {
        final ItemStack stack = query.getItemStack();
        return ((HardDriveItem) stack.getItem()).getData(stack);
    }

    private static int getCapacity(final ItemDeviceQuery query) {
        final ItemStack stack = query.getItemStack();
        final BlockDeviceData data = getData(query);
        if (data != null) {
            return (int) Math.max(data.getBlockDevice().getCapacity(), 0);
        }

        final HardDriveItem item = (HardDriveItem) stack.getItem();
        return Math.max(item.getCapacity(stack), 0);
    }
}
