/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.provider.item;

import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.data.BlockDeviceData;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.common.bus.device.provider.util.AbstractItemDeviceProvider;
import li.cil.oc2.common.bus.device.vm.item.FlashStorageDevice;
import li.cil.oc2.common.bus.device.vm.item.FlashStorageDeviceWithInitialData;
import li.cil.oc2.common.item.FlashMemoryItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Optional;

public final class FlashMemoryItemDeviceProvider extends AbstractItemDeviceProvider {
    public FlashMemoryItemDeviceProvider() {
        super(FlashMemoryItem.class);
    }

    // --------------------------------------------------------------------- //

    @Override
    public void disposeMissing(@Nullable final ItemDeviceQuery query, final CompoundTag tag) {
        super.disposeMissing(query, tag);
        FlashStorageDevice.unmount(tag);
    }

    // --------------------------------------------------------------------- //

    @Override
    protected Optional<ItemDevice> getItemDevice(final ItemDeviceQuery query) {
        final ItemStack stack = query.getItemStack();
        final FlashMemoryItem item = (FlashMemoryItem) stack.getItem();

        final int capacity = Math.max(item.getCapacity(stack), 0);
        final BlockDeviceData data = item.getData(stack);
        return Optional.of(data != null
            ? new FlashStorageDeviceWithInitialData(stack, capacity, data.getBlockDevice())
            : new FlashStorageDevice(stack, capacity));
    }
}
