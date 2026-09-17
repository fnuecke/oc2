/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui;

import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.item.SerialInterfaceCardItem;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.SerialInterfaceCardConfigurationMessage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

import static li.cil.oc2.common.util.TranslationUtils.text;

public final class SerialInterfaceCardScreen extends AbstractSideConfigurationScreen {
    private static final Component ADDRESS_TEXT = text("gui.{mod}.serial_interface_card.address");

    private static final int CONTROLS_TOP = 121;
    private static final int CONTROLS_HEIGHT = 16;
    private static final int LABEL_GAP = 5;
    private static final int LABEL_COLOR = 0xAAAAAA;
    private static final int ADDRESS_WIDTH = 38;

    // --------------------------------------------------------------------- //

    public SerialInterfaceCardScreen(final Player player, final InteractionHand hand) {
        super(player, hand, Items.SERIAL_INTERFACE_CARD.get());
    }

    // --------------------------------------------------------------------- //

    @Override
    protected void init() {
        super.init();

        final var addressBox = addRenderableWidget(new EditBox(font,
            left + addressLeft(), top + CONTROLS_TOP, ADDRESS_WIDTH, CONTROLS_HEIGHT, ADDRESS_TEXT));
        addressBox.setMaxLength(3);
        addressBox.setFilter(SerialInterfaceCardScreen::isAddress);
        addressBox.setValue(Integer.toString(SerialInterfaceCardItem.getAddress(getCard())));
        addressBox.setResponder(this::sendAddress);
    }

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);

        graphics.drawString(font, ADDRESS_TEXT, left + labelLeft(),
            top + CONTROLS_TOP + (CONTROLS_HEIGHT - font.lineHeight) / 2, LABEL_COLOR, false);
    }

    @Override
    protected boolean getConfiguration(@Nullable final Direction side) {
        return side != null && SerialInterfaceCardItem.getSideConfiguration(getCard(), side);
    }

    @Override
    protected void sendConfiguration(final Direction side, final boolean value) {
        SerialInterfaceCardItem.setSideConfiguration(getCard(), side, value);
        Network.sendToServer(new SerialInterfaceCardConfigurationMessage(hand, getCard()));
    }

    // --------------------------------------------------------------------- //

    private static boolean isAddress(final String value) {
        if (!value.chars().allMatch(Character::isDigit)) {
            return false;
        }
        return value.isEmpty() || Integer.parseInt(value) <= SerialInterfaceCardItem.MAX_ADDRESS;
    }

    private int labelLeft() {
        return (UI_WIDTH - font.width(ADDRESS_TEXT) - LABEL_GAP - ADDRESS_WIDTH) / 2;
    }

    private int addressLeft() {
        return labelLeft() + font.width(ADDRESS_TEXT) + LABEL_GAP;
    }

    private ItemStack getCard() {
        return player.getItemInHand(hand);
    }

    private void sendAddress(final String value) {
        if (value.isEmpty()) {
            return;
        }

        final int address = Integer.parseInt(value);
        if (address != SerialInterfaceCardItem.getAddress(getCard())) {
            SerialInterfaceCardItem.setAddress(getCard(), address);
            Network.sendToServer(new SerialInterfaceCardConfigurationMessage(hand, getCard()));
        }
    }
}
