/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.provider.item;

import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.common.Config;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.bus.device.provider.util.AbstractItemDeviceProvider;
import li.cil.oc2.common.bus.device.vm.item.MemoryDevice;
import li.cil.oc2.common.item.MemoryItem;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public final class MemoryItemDeviceProvider extends AbstractItemDeviceProvider {
    public MemoryItemDeviceProvider() {
        super(MemoryItem.class);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected Optional<ItemDevice> getItemDevice(final ItemDeviceQuery query) {
        final ItemStack stack = query.getItemStack();
        final MemoryItem item = (MemoryItem) stack.getItem();
        final int capacity = Math.max(item.getCapacity(stack), 0);
        return Optional.of(new MemoryDevice(query.getItemStack(), capacity));
    }

    @Override
    protected int getItemDeviceEnergyConsumption(final ItemDeviceQuery query) {
        final ItemStack stack = query.getItemStack();
        final MemoryItem item = (MemoryItem) stack.getItem();
        final int capacity = Math.max(item.getCapacity(stack), 0);
        return Math.max(1, (int) Math.round(capacity * Config.memoryEnergyPerMegabytePerTick / Constants.MEGABYTE));
    }
}
