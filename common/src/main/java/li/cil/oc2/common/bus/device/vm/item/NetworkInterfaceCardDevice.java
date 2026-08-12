/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.vm.item;

import li.cil.oc2.common.capabilities.CapabilityType;
import li.cil.oc2.common.item.NetworkInterfaceCardItem;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public final class NetworkInterfaceCardDevice extends AbstractNetworkInterfaceDevice {
    public NetworkInterfaceCardDevice(final ItemStack identity) {
        super(identity);
    }

    // ------------------------------------------------------------- //

    @Nullable
    @Override
    public <T> T getCapability(final CapabilityType<T> capability, @Nullable final Direction side) {
        if (NetworkInterfaceCardItem.getSideConfiguration(identity, side)) {
            return super.getCapability(capability, side);
        }

        return null;
    }
}
