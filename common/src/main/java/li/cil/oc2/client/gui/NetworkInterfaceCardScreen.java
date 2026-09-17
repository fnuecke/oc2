/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui;

import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.item.NetworkInterfaceCardItem;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.NetworkInterfaceCardConfigurationMessage;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

public final class NetworkInterfaceCardScreen extends AbstractSideConfigurationScreen {
    public NetworkInterfaceCardScreen(final Player player, final InteractionHand hand) {
        super(player, hand, Items.NETWORK_INTERFACE_CARD.get());
    }

    // --------------------------------------------------------------------- //

    @Override
    protected boolean getConfiguration(@Nullable final Direction side) {
        return side != null && NetworkInterfaceCardItem.getSideConfiguration(player.getItemInHand(hand), side);
    }

    @Override
    protected void sendConfiguration(final Direction side, final boolean value) {
        Network.sendToServer(new NetworkInterfaceCardConfigurationMessage(hand, side, value));
    }
}
