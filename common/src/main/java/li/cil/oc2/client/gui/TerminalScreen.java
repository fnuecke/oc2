/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui;

import li.cil.oc2.client.gui.widget.ImageButton;
import li.cil.oc2.client.gui.widget.ToggleImageButton;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.blockentity.TerminalBlockEntity;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.TerminalInputMessage;
import li.cil.oc2.common.network.message.TerminalKeepAliveMessage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.nio.ByteBuffer;

public final class TerminalScreen extends AbstractBlockEntityScreen<TerminalBlockEntity> {
    private final TerminalWidget terminalWidget;

    private long lastKeepAliveSentAt;

    // --------------------------------------------------------------------- //

    public TerminalScreen(final TerminalBlockEntity terminal) {
        super(Items.TERMINAL.get().getDescription(), terminal);
        this.terminalWidget = new TerminalWidget(new TerminalWidget.Parent() {
            @Override
            public int getWidth() {
                return width;
            }

            @Override
            public int getHeight() {
                return height;
            }

            @Override
            public boolean shouldRenderTerminal() {
                return true;
            }

            @Override
            public void sendTerminalInputToServer(ByteBuffer input) {
                Network.sendToServer(new TerminalInputMessage(blockEntity, input));
            }
        }, terminal.getTerminal());
        imageWidth = Sprites.TERMINAL_SCREEN.width;
        imageHeight = Sprites.TERMINAL_SCREEN.height;
    }

    // --------------------------------------------------------------------- //

    @Override
    protected void safeTick() {
        super.safeTick();

        sendKeepAlive();

        terminalWidget.tick();
    }

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
        return terminalWidget.mouseScrolled(mouseX, mouseY, deltaY) ||
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
    protected void init() {
        super.init();
        terminalWidget.init();

        addRenderableWidget(new ToggleImageButton(
            leftPos - Sprites.SIDEBAR_2.width + 4, topPos + CONTROLS_TOP + 4,
            12, 12,
            Sprites.INPUT_BUTTON_BASE,
            Sprites.INPUT_BUTTON_PRESSED,
            Sprites.INPUT_BUTTON_ACTIVE
        ) {
            @Override
            public void onPress() {
                super.onPress();
                InputCapture.toggle();
            }

            @Override
            public boolean isToggled() {
                return InputCapture.isEnabled();
            }
        }).withTooltip(
            Component.translatable(Constants.TERMINAL_CAPTURE_INPUT_CAPTION),
            Component.translatable(Constants.TERMINAL_CAPTURE_INPUT_DESCRIPTION)
        );

        addRenderableWidget(new ImageButton(
            leftPos - Sprites.SIDEBAR_2.width + 4, topPos + CONTROLS_TOP + 4 + 14,
            12, 12,
            Sprites.CONFIG_BUTTON_INACTIVE,
            Sprites.CONFIG_BUTTON_ACTIVE
        ) {
            @Override
            public void onPress() {
                minecraft.setScreen(new TerminalConfigurationScreen(blockEntity));
            }
        }).withTooltip(Component.translatable(Constants.OPEN_CONFIGURATION_CAPTION));
    }

    @Override
    public void removed() {
        super.removed();
        terminalWidget.onClose();
    }

    // --------------------------------------------------------------------- //

    @Override
    public void renderFg(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTicks) {
        terminalWidget.render(graphics, mouseX, mouseY, null);
    }

    @Override
    public void renderBg(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTicks) {
        Sprites.SIDEBAR_2.draw(graphics, leftPos - Sprites.SIDEBAR_2.width, topPos + CONTROLS_TOP);

        terminalWidget.renderBackground(graphics, mouseX, mouseY);
    }

    // --------------------------------------------------------------------- //

    private void sendKeepAlive() {
        final long now = System.currentTimeMillis();
        if (now - lastKeepAliveSentAt > TerminalBlockEntity.CLIENT_KEEPALIVE_EVERY_MILLIS) {
            lastKeepAliveSentAt = now;
            Network.sendToServer(new TerminalKeepAliveMessage(blockEntity));
        }
    }
}
