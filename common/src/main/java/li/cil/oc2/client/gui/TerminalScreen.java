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
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class TerminalScreen extends AbstractBlockEntityScreen<TerminalBlockEntity> {
    private final TerminalWidget terminalWidget;

    private int keepAliveCountdown;

    // --------------------------------------------------------------------- //

    public static void open(final TerminalBlockEntity terminal) {
        Minecraft.getInstance().setScreen(new TerminalScreen(terminal));
    }

    // --------------------------------------------------------------------- //

    public TerminalScreen(final TerminalBlockEntity terminal) {
        super(Items.TERMINAL.get().getName(), terminal);
        this.terminalWidget = new TerminalWidget(terminal.getTerminal(), () -> true, () -> null,
            input -> Network.sendToServer(new TerminalInputMessage(blockEntity, input)));
    }

    // --------------------------------------------------------------------- //

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean isDoubleClick) {
        return terminalWidget.mouseClicked(event, isDoubleClick) ||
            super.mouseClicked(event, isDoubleClick);
    }

    @Override
    public boolean mouseReleased(final MouseButtonEvent event) {
        return terminalWidget.mouseReleased(event) ||
            super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double deltaX, final double deltaY) {
        return terminalWidget.mouseScrolled(mouseX, mouseY, deltaX, deltaY) ||
            super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public boolean charTyped(final CharacterEvent event) {
        return terminalWidget.charTyped(event) ||
            super.charTyped(event);
    }

    @Override
    public boolean keyPressed(final KeyEvent event) {
        return terminalWidget.keyPressed(event) ||
            super.keyPressed(event);
    }

    // --------------------------------------------------------------------- //

    @Override
    protected Window createWindow() {
        final ImageButton configButton = new ImageButton(0, 0, 12, 12,
            Sprites.CONFIG_BUTTON_INACTIVE,
            Sprites.CONFIG_BUTTON_ACTIVE
        ) {
            @Override
            public void onPress(final InputWithModifiers input) {
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
