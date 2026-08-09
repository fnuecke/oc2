/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.vm.item;

import li.cil.oc2.common.item.NetworkInterfaceCardItem;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class NetworkInterfaceCardDevice extends AbstractNetworkInterfaceDevice {
    public NetworkInterfaceCardDevice(final ItemStack identity) {
        super(identity);
    }

    ///////////////////////////////////////////////////////////////

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(final Capability<T> cap, @Nullable final Direction side) {
        if (NetworkInterfaceCardItem.getSideConfiguration(identity, side)) {
            return super.getCapability(cap, side);
        }

        return LazyOptional.empty();
    }
}
