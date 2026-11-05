/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui;

import li.cil.oc2.client.gui.util.GuiUtils;
import li.cil.oc2.client.gui.widget.ImageButton;
import li.cil.oc2.common.blockentity.InternetGatewayBlockEntity;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.InternetGatewayConfigurationMessage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import static li.cil.oc2.common.util.TranslationUtils.text;

public final class InternetGatewayConfigurationScreen extends AbstractBlockEntityScreen<InternetGatewayBlockEntity> {
    private static final int MARGIN = 8;
    private static final int FIELD_HEIGHT = 12;
    private static final int BUTTON_HEIGHT = 16;
    private static final int LABEL_OFFSET = 10;
    private static final int TITLE_COLOR = 0x404040;
    private static final int LABEL_COLOR = 0xFFAAAAAA;

    private static final int MODE_TOP = 34;
    private static final int PEER_TOP = 76;
    private static final int NETWORK_TOP = 118;
    private static final int HEIGHT = NETWORK_TOP + FIELD_HEIGHT + MARGIN;

    private static final int COLUMN_GAP = 4;
    private static final int COLUMN_WIDTH = (176 - MARGIN * 2 - COLUMN_GAP) / 2;

    private static final Component MODE_LABEL = text("gui.{mod}.internet_gateway.mode");
    private static final Component PEER_LABEL = text("gui.{mod}.internet_gateway.peer");
    private static final Component PORT_LABEL = text("gui.{mod}.internet_gateway.port");
    private static final Component VNI_LABEL = text("gui.{mod}.internet_gateway.vni");
    private static final Component NAT_MODE_LABEL = text("gui.{mod}.internet_gateway.nat");
    private static final Component VXLAN_MODE_LABEL = text("gui.{mod}.internet_gateway.vxlan");

    private EditBox peerField;
    private EditBox portField;
    private EditBox vniField;
    private ModeButton modeButton;

    // --------------------------------------------------------------------- //

    public InternetGatewayConfigurationScreen(final InternetGatewayBlockEntity gateway) {
        super(Items.INTERNET_GATEWAY.get().getDescription(), gateway);
        imageHeight = HEIGHT;
    }

    // --------------------------------------------------------------------- //

    @Override
    public void renderBg(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTicks) {
        Sprites.GENERIC_SCREEN.draw(graphics, leftPos, topPos, imageWidth, imageHeight);
    }

    @Override
    public void renderFg(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTicks) {
        graphics.drawString(font, title, leftPos + MARGIN, topPos + MARGIN, TITLE_COLOR, false);
        graphics.drawString(font, MODE_LABEL, leftPos + MARGIN, topPos + MODE_TOP - LABEL_OFFSET, LABEL_COLOR);

        if (modeButton.getMode() == InternetGatewayBlockEntity.InternetMode.VXLAN) {
            graphics.drawString(font, PEER_LABEL, leftPos + MARGIN, topPos + PEER_TOP - LABEL_OFFSET, LABEL_COLOR);
            graphics.drawString(font, PORT_LABEL, leftPos + MARGIN, topPos + NETWORK_TOP - LABEL_OFFSET, LABEL_COLOR);
            graphics.drawString(font, VNI_LABEL,
                leftPos + MARGIN + COLUMN_WIDTH + COLUMN_GAP,
                topPos + NETWORK_TOP - LABEL_OFFSET, LABEL_COLOR);
        }
    }

    @Override
    protected void init() {
        super.init();

        modeButton = addRenderableWidget(new ModeButton(
            leftPos + MARGIN, topPos + MODE_TOP,
            imageWidth - MARGIN * 2, BUTTON_HEIGHT
        ));

        peerField = addPeerField(topPos + PEER_TOP, blockEntity.getSettings().vxlanPeer());

        portField = addNumberField(
            leftPos + MARGIN, topPos + NETWORK_TOP,
            COLUMN_WIDTH, blockEntity.getSettings().vxlanPort(), 1, 65535
        );

        vniField = addNumberField(
            leftPos + MARGIN + COLUMN_WIDTH + COLUMN_GAP, topPos + NETWORK_TOP,
            COLUMN_WIDTH, blockEntity.getSettings().vxlanVni(), 0, 16_777_215
        );

        updateFieldsEnabled();
        setInitialFocus(peerField);
    }

    @Override
    public void removed() {
        super.removed();

        final InternetGatewayBlockEntity.InternetMode mode = modeButton.getMode();
        final String peer = peerField.getValue().trim();
        final int port = valueOf(portField, blockEntity.getSettings().vxlanPort());
        final int vni = valueOf(vniField, blockEntity.getSettings().vxlanVni());

        if (mode != blockEntity.getSettings().mode()
            || !peer.equals(blockEntity.getSettings().vxlanPeer())
            || port != blockEntity.getSettings().vxlanPort()
            || vni != blockEntity.getSettings().vxlanVni()) {
            Network.sendToServer(new InternetGatewayConfigurationMessage.ToServer(blockEntity, new InternetGatewayBlockEntity.Settings(
                peer, port, vni, mode
            )));
        }
    }

    // --------------------------------------------------------------------- //

    private EditBox addPeerField(final int y, final String value) {
        final EditBox field = new EditBox(
            font, leftPos + MARGIN, y,
            imageWidth - MARGIN * 2, FIELD_HEIGHT, PEER_LABEL
        );

        field.setMaxLength(InternetGatewayConfigurationMessage.MAX_PEER_LENGTH);
        field.setValue(value);
        field.setFilter(InternetGatewayConfigurationScreen::isPeerCharacter);

        addRenderableWidget(field);
        return field;
    }

    private EditBox addNumberField(final int x, final int y, final int width,
                                   final int value, final int minimum, final int maximum) {
        final EditBox field = new EditBox(
            font, x, y,
            width, FIELD_HEIGHT, Component.empty()
        );

        field.setMaxLength(String.valueOf(maximum).length());
        field.setValue(String.valueOf(value));
        field.setFilter(InternetGatewayConfigurationScreen::isNumber);
        field.setResponder(ignored -> GuiUtils.clampToRange(field, minimum, maximum));

        addRenderableWidget(field);
        return field;
    }

    private void updateFieldsEnabled() {
        final boolean vxlan = modeButton.getMode() == InternetGatewayBlockEntity.InternetMode.VXLAN;

        peerField.setVisible(vxlan);
        portField.setVisible(vxlan);
        vniField.setVisible(vxlan);

        peerField.setEditable(vxlan);
        portField.setEditable(vxlan);
        vniField.setEditable(vxlan);

        if (!vxlan) {
            peerField.setFocused(false);
            portField.setFocused(false);
            vniField.setFocused(false);
        }
    }

    private static int valueOf(final EditBox field, final int fallback) {
        final String value = field.getValue();

        if (value.isEmpty()) {
            return fallback;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static boolean isNumber(final String value) {
        return value.chars().allMatch(Character::isDigit);
    }

    private static boolean isPeerCharacter(final String value) {
        return value.chars().allMatch(character ->
            Character.isLetterOrDigit(character)
                || character == '.'
                || character == ':'
                || character == '-'
                || character == '_'
                || character == '['
                || character == ']'
        );
    }

    // --------------------------------------------------------------------- //

    private final class ModeButton extends ImageButton {
        private InternetGatewayBlockEntity.InternetMode mode;

        ModeButton(final int x, final int y, final int width, final int height) {
            super(
                x, y, width, height,
                Sprites.GENERIC_BUTTON_BASE, Sprites.GENERIC_BUTTON_PRESSED
            );

            mode = blockEntity.getSettings().mode();
            updateCaption();
        }

        @Override
        public void onPress() {
            super.onPress();

            mode = switch (mode) {
                case NAT -> InternetGatewayBlockEntity.InternetMode.VXLAN;
                case VXLAN -> InternetGatewayBlockEntity.InternetMode.NAT;
            };

            updateCaption();
            updateFieldsEnabled();
        }

        InternetGatewayBlockEntity.InternetMode getMode() {
            return mode;
        }

        private void updateCaption() {
            switch (mode) {
                case NAT -> setMessage(NAT_MODE_LABEL);
                case VXLAN -> setMessage(VXLAN_MODE_LABEL);
            }
        }
    }
}
