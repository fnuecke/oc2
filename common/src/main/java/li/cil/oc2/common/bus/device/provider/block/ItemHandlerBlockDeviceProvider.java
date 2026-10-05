/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.provider.block;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.common.bus.device.ItemHandlerDevice;
import li.cil.oc2.common.bus.device.provider.util.AbstractBlockEntityCapabilityDeviceProvider;
import li.cil.oc2.common.capabilities.CachedItemHandler;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.capabilities.CapabilityCache;
import li.cil.oc2.common.inventory.ItemHandler;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Optional;

public final class ItemHandlerBlockDeviceProvider extends AbstractBlockEntityCapabilityDeviceProvider<ItemHandler, BlockEntity> {
    public ItemHandlerBlockDeviceProvider() {
        super(Capabilities.ITEM_HANDLER);
    }

    // --------------------------------------------------------------------- //

    @Override
    protected Optional<Device> getBlockDevice(final BlockDeviceQuery query, final CapabilityCache<ItemHandler> value) {
        return Optional.of(new ObjectDevice(new ItemHandlerDevice(new CachedItemHandler(value))));
    }
}
