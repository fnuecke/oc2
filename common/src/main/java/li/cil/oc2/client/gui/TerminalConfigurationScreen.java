/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui;

import li.cil.oc2.client.gui.layout.*;
import li.cil.oc2.client.gui.util.GuiUtils;
import li.cil.oc2.client.gui.widget.ImageButton;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.blockentity.TerminalBlockEntity;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.TerminalConfigurationMessage;
import li.cil.oc2.common.network.message.TerminalKeepAliveMessage;
import li.cil.oc2.common.serial.SerialFrame;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import static li.cil.oc2.common.util.TranslationUtils.text;

public final class TerminalConfigurationScreen extends AbstractBlockEntityScreen<TerminalBlockEntity> {
    private static final int CONTENT_WIDTH = 80;
    private static final int BUTTON_HEIGHT = 16;

    private static final Component ADDRESS_LABEL = text("gui.{mod}.terminal.address");
    private static final Component BAUD_RATE_LABEL = text("gui.{mod}.terminal.baud_rate");

    private static final int[] BAUD_RATES = {300, 1200, 2400, 4800, 9600, 19200, 38400, 57600, 115200};

    // --------------------------------------------------------------------- //

    private int keepAliveCountdown;

    private final EditBoxNode addressNode;
    private final EditBox addressField;
    private int baudRateIndex;

    // --------------------------------------------------------------------- //

    public TerminalConfigurationScreen(final TerminalBlockEntity terminal) {
        super(Items.TERMINAL.get().getDescription(), terminal);
        baudRateIndex = indexOfNearestBaudRate(terminal.getBaudRate());
        addressNode = createAddressNode(terminal.getAddress());
        addressField = addressNode.editBox();
    }

    // --------------------------------------------------------------------- //

    @Override
    public void removed() {
        super.removed();

        final int address = Math.clamp(valueOf(addressField, blockEntity.getAddress()), 0, SerialFrame.MAX_ADDRESS);
        final int baudRate = BAUD_RATES[baudRateIndex];
        if (address != blockEntity.getAddress() || baudRate != blockEntity.getBaudRate()) {
            Network.sendToServer(new TerminalConfigurationMessage.ToServer(blockEntity, address, baudRate));
        }
    }

    // --------------------------------------------------------------------- //

    @Override
    protected Window createWindow() {
        final ImageButton backButton = new ImageButton(0, 0, 12, 12,
            Sprites.CONFIG_BUTTON_ACTIVE,
            Sprites.CONFIG_BUTTON_INACTIVE
        ) {
            @Override
            public void onPress() {
                minecraft.setScreen(new TerminalScreen(blockEntity));
            }
        };
        backButton.withTooltip(Component.translatable(Constants.MACHINE_OPEN_TERMINAL_CAPTION));

        return new Window(
            NodeStack.vertical(
                LabelNode.truncating(title),
                Ui.section(ADDRESS_LABEL, addressNode),
                Ui.section(BAUD_RATE_LABEL, new WidgetNode(new BaudRateButton()).width(Size.GROW))
            ).width(Size.fixed(CONTENT_WIDTH)),
            Ui.toolbar(new WidgetNode(backButton)));
    }

    @Override
    protected void init() {
        super.init();
        setInitialFocus(addressField);
    }

    @Override
    protected void safeTick() {
        sendKeepAlive();
    }

    // --------------------------------------------------------------------- //

    private void sendKeepAlive() {
        if (--keepAliveCountdown <= 0) {
            keepAliveCountdown = TerminalBlockEntity.CLIENT_KEEPALIVE_EVERY;
            Network.sendToServer(new TerminalKeepAliveMessage(blockEntity));
        }
    }

    private static EditBoxNode createAddressNode(final int value) {
        final EditBoxNode node = new EditBoxNode(ADDRESS_LABEL);
        node.width(Size.GROW);
        final EditBox field = node.editBox();
        field.setFilter(TerminalConfigurationScreen::isNumber);
        field.setValue(String.valueOf(value));
        field.setResponder(text -> GuiUtils.clampToRange(field, 0, SerialFrame.MAX_ADDRESS));
        return node;
    }

    private static int indexOfNearestBaudRate(final int baudRate) {
        int nearest = 0;
        for (int i = 1; i < BAUD_RATES.length; i++) {
            if (Math.abs(BAUD_RATES[i] - baudRate) < Math.abs(BAUD_RATES[nearest] - baudRate)) {
                nearest = i;
            }
        }

        return nearest;
    }

    private static int valueOf(final EditBox field, final int fallback) {
        final String value = field.getValue();
        return value.isEmpty() ? fallback : Integer.parseInt(value);
    }

    private static boolean isNumber(final String value) {
        return value.chars().allMatch(Character::isDigit);
    }

    // --------------------------------------------------------------------- //

    private final class BaudRateButton extends ImageButton {
        BaudRateButton() {
            super(0, 0, 0, BUTTON_HEIGHT, Sprites.GENERIC_BUTTON_BASE, Sprites.GENERIC_BUTTON_PRESSED);
            cycle(0);
        }

        @Override
        public void onPress() {
            super.onPress();
            cycle(1);
        }

        private void cycle(final int direction) {
            baudRateIndex = Math.floorMod(baudRateIndex + direction, BAUD_RATES.length);
            setMessage(Component.literal(String.valueOf(BAUD_RATES[baudRateIndex])));
        }
    }
}
