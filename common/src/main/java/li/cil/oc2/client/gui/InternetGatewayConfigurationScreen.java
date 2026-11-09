/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui;

import li.cil.oc2.client.gui.layout.*;
import li.cil.oc2.client.gui.util.GuiUtils;
import li.cil.oc2.client.gui.widget.ImageButton;
import li.cil.oc2.common.Config;
import li.cil.oc2.common.blockentity.InternetGatewayBlockEntity;
import li.cil.oc2.common.blockentity.InternetGatewayBlockEntity.InternetMode;
import li.cil.oc2.common.inet.util.AddressParseException;
import li.cil.oc2.common.inet.util.InternetUtils;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.InternetGatewayConfigurationMessage;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.Objects;
import java.util.function.Predicate;

import static li.cil.oc2.common.util.TranslationUtils.text;

public final class InternetGatewayConfigurationScreen extends AbstractBlockEntityScreen<InternetGatewayBlockEntity> {
    private static final int CONTENT_WIDTH = 160;
    private static final int BUTTON_HEIGHT = 16;
    private static final int MAX_PORT = 65535;
    private static final int MAX_VNI = 16_777_215;
    private static final int MAX_IPV4_LENGTH = 15;
    private static final String IPV4_CHARACTERS = "0123456789.";
    private static final String ADDRESS_CHARACTERS = "0123456789abcdefABCDEF.:";
    private static final int INVALID_TEXT_COLOR = 0xFF000000 | Objects.requireNonNull(ChatFormatting.RED.getColor());

    private static final Component MODE_LABEL = text("gui.{mod}.internet_gateway.mode");
    private static final Component ADDRESS_LABEL = text("gui.{mod}.internet_gateway.address");
    private static final Component PEER_LABEL = text("gui.{mod}.internet_gateway.peer");
    private static final Component PORT_LABEL = text("gui.{mod}.internet_gateway.port");
    private static final Component VNI_LABEL = text("gui.{mod}.internet_gateway.vni");
    private static final Component NAT_MODE_LABEL = text("gui.{mod}.internet_gateway.nat");
    private static final Component VXLAN_MODE_LABEL = text("gui.{mod}.internet_gateway.vxlan");

    // --------------------------------------------------------------------- //

    private final EditBoxNode addressNode;
    private final EditBoxNode peerNode;
    private final EditBoxNode portNode;
    private final EditBoxNode vniNode;
    private InternetMode mode;

    // --------------------------------------------------------------------- //

    public InternetGatewayConfigurationScreen(final InternetGatewayBlockEntity gateway) {
        super(Items.INTERNET_GATEWAY.get().getDescription(), gateway);
        final InternetGatewayBlockEntity.Settings settings = gateway.getSettings();
        mode = settings.mode();
        addressNode = createAddressNode(ADDRESS_LABEL, InternetUtils.ipv4AddressToString(settings.gatewayAddress()),
            MAX_IPV4_LENGTH,
            InternetGatewayConfigurationScreen::isIpv4Character,
            InternetGatewayConfigurationScreen::isValidIpv4Address);
        peerNode = createAddressNode(PEER_LABEL, settings.vxlanPeer(),
            InternetGatewayConfigurationMessage.MAX_PEER_LENGTH,
            InternetGatewayConfigurationScreen::isAddressCharacter,
            InternetGatewayConfigurationScreen::isValidAddress);
        portNode = createNumberNode(PORT_LABEL, settings.vxlanPort(), 1, MAX_PORT);
        vniNode = createNumberNode(VNI_LABEL, settings.vxlanVni(), 0, MAX_VNI);
    }

    // --------------------------------------------------------------------- //

    @Override
    public void removed() {
        super.removed();

        final InternetGatewayBlockEntity.Settings current = blockEntity.getSettings();
        final InternetGatewayBlockEntity.Settings settings = new InternetGatewayBlockEntity.Settings(
            mode,
            parseIpv4Address(addressNode.editBox().getValue(), current.gatewayAddress()),
            peerNode.editBox().getValue(),
            valueOf(portNode.editBox(), current.vxlanPort()),
            valueOf(vniNode.editBox(), current.vxlanVni()));
        if (!settings.equals(current)) {
            Network.sendToServer(new InternetGatewayConfigurationMessage.ToServer(blockEntity, settings));
        }
    }

    // --------------------------------------------------------------------- //

    @Override
    protected Window createWindow() {
        return new Window(
            NodeStack.vertical(
                LabelNode.truncating(title),
                Ui.field(MODE_LABEL, new WidgetNode(new ModeButton()).width(Size.GROW))
                    .visibleWhen(() -> Config.vxlanEnabled),
                Ui.field(ADDRESS_LABEL, addressNode)
                    .visibleWhen(() -> mode != InternetMode.VXLAN),
                NodeStack.vertical(
                    Ui.field(PEER_LABEL, peerNode),
                    NodeStack.horizontal(
                        Ui.field(PORT_LABEL, portNode),
                        Ui.field(VNI_LABEL, vniNode)
                    ).width(Size.GROW)
                ).width(Size.GROW).visibleWhen(() -> mode == InternetMode.VXLAN)
            ).width(Size.fixed(CONTENT_WIDTH)));
    }

    @Override
    protected void init() {
        super.init();
        setInitialFocus(mode == InternetMode.VXLAN ? peerNode.editBox() : addressNode.editBox());
    }

    // --------------------------------------------------------------------- //

    private static EditBoxNode createAddressNode(final Component label, final String value, final int maxLength,
                                                 final Predicate<String> filter, final Predicate<String> isValid) {
        final EditBoxNode node = new EditBoxNode(label);
        node.width(Size.GROW);
        final EditBox field = node.editBox();
        field.setMaxLength(maxLength);
        field.setResponder(text -> field.setTextColor(isValid.test(text) ? EditBoxNode.TEXT_COLOR : INVALID_TEXT_COLOR));
        field.setValue(value);
        field.setFilter(filter);
        return node;
    }

    private static EditBoxNode createNumberNode(final Component label, final int value, final int minimum, final int maximum) {
        final EditBoxNode node = new EditBoxNode(label);
        node.width(Size.GROW);
        final EditBox field = node.editBox();
        field.setMaxLength(String.valueOf(maximum).length());
        field.setFilter(InternetGatewayConfigurationScreen::isNumber);
        field.setValue(String.valueOf(value));
        field.setResponder(text -> GuiUtils.clampToRange(field, minimum, maximum));
        return node;
    }

    private static int valueOf(final EditBox field, final int fallback) {
        final String value = field.getValue();
        return value.isEmpty() ? fallback : Integer.parseInt(value);
    }

    private static int parseIpv4Address(final String value, final int fallback) {
        try {
            return InternetUtils.parseIpv4Address(value);
        } catch (final AddressParseException e) {
            return fallback;
        }
    }

    private static boolean isValidIpv4Address(final String value) {
        try {
            InternetUtils.parseIpv4Address(value);
            return true;
        } catch (final AddressParseException e) {
            return false;
        }
    }

    private static boolean isValidAddress(final String value) {
        try {
            InternetUtils.parseInetAddress(value);
            return true;
        } catch (final AddressParseException e) {
            return false;
        }
    }

    private static boolean isIpv4Character(final String value) {
        return value.chars().allMatch(character -> IPV4_CHARACTERS.indexOf(character) >= 0);
    }

    private static boolean isAddressCharacter(final String value) {
        return value.chars().allMatch(character -> ADDRESS_CHARACTERS.indexOf(character) >= 0);
    }

    private static boolean isNumber(final String value) {
        return value.chars().allMatch(Character::isDigit);
    }

    // --------------------------------------------------------------------- //

    private final class ModeButton extends ImageButton {
        ModeButton() {
            super(0, 0, 0, BUTTON_HEIGHT, Sprites.GENERIC_BUTTON_BASE, Sprites.GENERIC_BUTTON_PRESSED);
            updateCaption();
        }

        @Override
        public void onPress() {
            super.onPress();
            mode = switch (mode) {
                case NAT -> InternetMode.VXLAN;
                case VXLAN -> InternetMode.NAT;
            };
            updateCaption();
        }

        private void updateCaption() {
            setMessage(switch (mode) {
                case NAT -> NAT_MODE_LABEL;
                case VXLAN -> VXLAN_MODE_LABEL;
            });
        }
    }
}
