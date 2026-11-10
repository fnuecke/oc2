/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.provider.item;

import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.bus.device.item.MemoryDevice;
import li.cil.oc2.common.bus.device.provider.util.AbstractItemDeviceProvider;
import li.cil.oc2.common.item.Items;

import java.util.Optional;

public final class CpuZ80ItemDeviceProvider extends AbstractItemDeviceProvider {
    private static final int MEMORY_SIZE = 64 * Constants.KILOBYTE;

    // --------------------------------------------------------------------- //

    public CpuZ80ItemDeviceProvider() {
        super(Items.CPU_Z80);
    }

    // --------------------------------------------------------------------- //

    @Override
    protected Optional<ItemDevice> getItemDevice(final ItemDeviceQuery query) {
        return Optional.of(new MemoryDevice(query.getItemStack(), MEMORY_SIZE));
    }
}
