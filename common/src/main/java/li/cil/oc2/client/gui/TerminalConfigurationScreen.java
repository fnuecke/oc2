/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui;

import li.cil.oc2.client.gui.util.GuiUtils;
import li.cil.oc2.client.gui.widget.ImageButton;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.blockentity.TerminalBlockEntity;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.TerminalConfigurationMessage;
import li.cil.oc2.common.network.message.TerminalKeepAliveMessage;
import li.cil.oc2.common.serial.SerialFrame;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import static li.cil.oc2.common.util.TranslationUtils.text;

public final class TerminalConfigurationScreen extends AbstractBlockEntityScreen<TerminalBlockEntity> {
    private static final int MARGIN = 8;
    private static final int FIELD_HEIGHT = 12;
    private static final int LINE_HEIGHT = 26;
    private static final int BUTTON_HEIGHT = 16;
    private static final int ADDRESS_TOP = MARGIN + LINE_HEIGHT;
    private static final int BAUD_RATE_TOP = ADDRESS_TOP + LINE_HEIGHT + 2;
    private static final int HEIGHT = BAUD_RATE_TOP + BUTTON_HEIGHT + MARGIN;
    private static final int LABEL_OFFSET = 10;
    private static final int TITLE_COLOR = 0x404040;
    private static final int LABEL_COLOR = 0xFFAAAAAA;

    private static final Component ADDRESS_LABEL = text("gui.{mod}.terminal.address");
    private static final Component BAUD_RATE_LABEL = text("gui.{mod}.terminal.baud_rate");

    private static final int[] BAUD_RATES = {300, 1200, 2400, 4800, 9600, 19200, 38400, 57600, 115200};

    // --------------------------------------------------------------------- //

    private long lastKeepAliveSentAt;

    private EditBox addressField;
    private int baudRateIndex;

    // --------------------------------------------------------------------- //

    public TerminalConfigurationScreen(final TerminalBlockEntity terminal) {
        super(Items.TERMINAL.get().getDescription(), terminal);
        imageHeight = HEIGHT;
    }

    // --------------------------------------------------------------------- //

    @Override
    protected void safeTick() {
        sendKeepAlive();
    }

    @Override
    public void renderBg(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTicks) {
        Sprites.SIDEBAR_1.draw(graphics, leftPos - Sprites.SIDEBAR_1.width, topPos + CONTROLS_TOP);
        Sprites.GENERIC_SCREEN.draw(graphics, leftPos, topPos, imageWidth, imageHeight);
    }

    @Override
    public void renderFg(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTicks) {
        graphics.drawString(font, title, leftPos + MARGIN, topPos + MARGIN, TITLE_COLOR, false);
        graphics.drawString(font, ADDRESS_LABEL, leftPos + MARGIN, topPos + ADDRESS_TOP - LABEL_OFFSET, LABEL_COLOR);
        graphics.drawString(font, BAUD_RATE_LABEL, leftPos + MARGIN, topPos + BAUD_RATE_TOP - LABEL_OFFSET, LABEL_COLOR);
    }

    @Override
    protected void init() {
        super.init();

        baudRateIndex = indexOfNearestBaudRate(blockEntity.getBaudRate());

        addressField = addAddressField(topPos + ADDRESS_TOP, blockEntity.getAddress());
        addRenderableWidget(new BaudRateButton(leftPos + MARGIN, topPos + BAUD_RATE_TOP, imageWidth - MARGIN * 2, BUTTON_HEIGHT));

        setInitialFocus(addressField);

        addRenderableWidget(new ImageButton(
            leftPos - Sprites.SIDEBAR_1.width + 4, topPos + CONTROLS_TOP + 4,
            12, 12,
            Sprites.CONFIG_BUTTON_ACTIVE,
            Sprites.CONFIG_BUTTON_INACTIVE
        ) {
            @Override
            public void onPress() {
                minecraft.setScreen(new TerminalScreen(blockEntity));
            }
        }).withTooltip(Component.translatable(Constants.MACHINE_OPEN_TERMINAL_CAPTION));
    }

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

    private void sendKeepAlive() {
        final long now = System.currentTimeMillis();
        if (now - lastKeepAliveSentAt > TerminalBlockEntity.CLIENT_KEEPALIVE_EVERY_MILLIS) {
            lastKeepAliveSentAt = now;
            Network.sendToServer(new TerminalKeepAliveMessage(blockEntity));
        }
    }

    private EditBox addAddressField(final int y, final int value) {
        final EditBox field = new EditBox(font, leftPos + MARGIN, y, imageWidth - MARGIN * 2, FIELD_HEIGHT, ADDRESS_LABEL);
        field.setFilter(TerminalConfigurationScreen::isNumber);
        field.setValue(String.valueOf(value));
        field.setResponder(text -> GuiUtils.clampToRange(field, 0, SerialFrame.MAX_ADDRESS));
        addRenderableWidget(field);
        return field;
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
        BaudRateButton(final int x, final int y, final int width, final int height) {
            super(x, y, width, height, Sprites.GENERIC_BUTTON_BASE, Sprites.GENERIC_BUTTON_PRESSED);
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
