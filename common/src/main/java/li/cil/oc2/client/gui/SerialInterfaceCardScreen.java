/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui;

import li.cil.oc2.client.gui.layout.*;
import li.cil.oc2.client.gui.util.GuiUtils;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.item.SerialInterfaceCardItem;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.SerialInterfaceCardConfigurationMessage;
import li.cil.oc2.common.serial.SerialFrame;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import static li.cil.oc2.common.util.TranslationUtils.text;

public final class SerialInterfaceCardScreen extends AbstractSideConfigurationScreen {
    private static final Component ADDRESS_TEXT = text("gui.{mod}.serial_interface_card.address");

    // --------------------------------------------------------------------- //

    public SerialInterfaceCardScreen(final Player player, final InteractionHand hand) {
        super(player, hand, Items.SERIAL_INTERFACE_CARD.get());
    }

    // --------------------------------------------------------------------- //

    @Override
    protected Node[] createControls() {
        final EditBoxNode addressNode = new EditBoxNode(ADDRESS_TEXT);
        addressNode.width(Size.GROW);
        final EditBox addressBox = addressNode.editBox();
        addressBox.setFilter(SerialInterfaceCardScreen::isNumber);
        addressBox.setValue(Integer.toString(SerialInterfaceCardItem.getAddress(getCard())));
        addressBox.setResponder(value -> sendAddress(addressBox, value));
        return new Node[]{
            Ui.field(ADDRESS_TEXT, addressNode)
        };
    }

    @Override
    protected boolean getConfiguration(final Direction side) {
        return SerialInterfaceCardItem.getSideConfiguration(getCard(), side);
    }

    @Override
    protected void sendConfiguration(final Direction side, final boolean value) {
        SerialInterfaceCardItem.setSideConfiguration(getCard(), side, value);
        Network.sendToServer(new SerialInterfaceCardConfigurationMessage(hand, getCard()));
    }

    // --------------------------------------------------------------------- //

    private static boolean isNumber(final String value) {
        return value.chars().allMatch(Character::isDigit);
    }

    private ItemStack getCard() {
        return player.getItemInHand(hand);
    }

    private void sendAddress(final EditBox addressBox, final String value) {
        if (value.isEmpty()) {
            return;
        }

        if (GuiUtils.clampToRange(addressBox, 0, SerialFrame.MAX_ADDRESS)) {
            return; // setting the value calls this again
        }

        final int address = Integer.parseInt(value);
        if (address != SerialInterfaceCardItem.getAddress(getCard())) {
            SerialInterfaceCardItem.setAddress(getCard(), address);
            Network.sendToServer(new SerialInterfaceCardConfigurationMessage(hand, getCard()));
        }
    }
}
