/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui;

import li.cil.oc2.client.gui.layout.Ui;
import li.cil.oc2.client.gui.layout.WidgetNode;
import li.cil.oc2.client.gui.layout.Window;
import li.cil.oc2.client.gui.node.TerminalWidget;
import li.cil.oc2.client.gui.widget.ImageButton;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.blockentity.TerminalBlockEntity;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.TerminalInputMessage;
import li.cil.oc2.common.network.message.TerminalKeepAliveMessage;
import net.minecraft.network.chat.Component;

public final class TerminalScreen extends AbstractBlockEntityScreen<TerminalBlockEntity> {
    private final TerminalWidget terminalWidget;

    private int keepAliveCountdown;

    // --------------------------------------------------------------------- //

    public TerminalScreen(final TerminalBlockEntity terminal) {
        super(Items.TERMINAL.get().getDescription(), terminal);
        this.terminalWidget = new TerminalWidget(terminal.getTerminal(), () -> true, () -> null,
            input -> Network.sendToServer(new TerminalInputMessage(blockEntity, input)));
    }

    // --------------------------------------------------------------------- //

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        return terminalWidget.mouseClicked(mouseX, mouseY, button) ||
            super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(final double mouseX, final double mouseY, final int button) {
        return terminalWidget.mouseReleased(mouseX, mouseY, button) ||
            super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double deltaX, final double deltaY) {
        return terminalWidget.mouseScrolled(mouseX, mouseY, deltaX, deltaY) ||
            super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public boolean charTyped(final char ch, final int modifiers) {
        return terminalWidget.charTyped(ch, modifiers) ||
            super.charTyped(ch, modifiers);
    }

    @Override
    public boolean keyPressed(final int keyCode, final int scanCode, final int modifiers) {
        return terminalWidget.keyPressed(keyCode, scanCode, modifiers) ||
            super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void removed() {
        super.removed();
        terminalWidget.close();
    }

    // --------------------------------------------------------------------- //

    @Override
    protected Window createWindow() {
        final ImageButton configButton = new ImageButton(0, 0, 12, 12,
            Sprites.CONFIG_BUTTON_INACTIVE,
            Sprites.CONFIG_BUTTON_ACTIVE
        ) {
            @Override
            public void onPress() {
                minecraft.setScreen(new TerminalConfigurationScreen(blockEntity));
            }
        };
        configButton.withTooltip(Component.translatable(Constants.OPEN_CONFIGURATION_CAPTION));

        return new Window(Ui.insetBlack(new WidgetNode(terminalWidget)),
            Ui.toolbar(new WidgetNode(InputCapture.button()), new WidgetNode(configButton)));
    }

    @Override
    protected void safeTick() {
        super.safeTick();

        sendKeepAlive();

        terminalWidget.tick();
    }

    // --------------------------------------------------------------------- //

    private void sendKeepAlive() {
        if (--keepAliveCountdown <= 0) {
            keepAliveCountdown = TerminalBlockEntity.CLIENT_KEEPALIVE_EVERY;
            Network.sendToServer(new TerminalKeepAliveMessage(blockEntity));
        }
    }
}
