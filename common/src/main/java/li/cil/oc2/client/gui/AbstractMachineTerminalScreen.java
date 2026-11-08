/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui;

import li.cil.oc2.client.gui.layout.Node;
import li.cil.oc2.client.gui.layout.Ui;
import li.cil.oc2.client.gui.layout.UiContainerScreen;
import li.cil.oc2.client.gui.layout.WidgetNode;
import li.cil.oc2.client.gui.layout.Window;
import li.cil.oc2.client.gui.node.TerminalWidget;
import li.cil.oc2.common.container.AbstractMachineTerminalContainer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

@Environment(EnvType.CLIENT)
public abstract class AbstractMachineTerminalScreen<T extends AbstractMachineTerminalContainer> extends UiContainerScreen<T> {
    protected final TerminalWidget terminalWidget;

    // --------------------------------------------------------------------- //

    protected AbstractMachineTerminalScreen(final T container, final Inventory playerInventory, final Component title) {
        super(container, playerInventory, title, Sprites.THEME);
        this.terminalWidget = new TerminalWidget(container.getTerminal(),
            () -> container.getVirtualMachine().isRunning(),
            () -> container.getVirtualMachine().getError(),
            container::sendTerminalInputToServer);
    }

    // --------------------------------------------------------------------- //

    @Override
    public void containerTick() {
        super.containerTick();

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
        if (terminalWidget.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }

        // Don't close with inventory binding since we usually want to use that as terminal input
        // even without input capture enabled.
        if (minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void removed() {
        super.removed();
        terminalWidget.close();
    }

    // --------------------------------------------------------------------- //

    protected Node createContent() {
        return Ui.insetBlack(new WidgetNode(terminalWidget));
    }

    @Override
    protected Window createWindow() {
        return new Window(createContent(), MachineScreens.toolbar(menu,
            InputCapture.button(),
            MachineScreens.openInventoryButton(menu)));
    }

    @Override
    protected void init() {
        super.init();

        final EditBox focusIndicatorEditBox = new EditBox(font, 0, 0, 0, 0, CommonComponents.EMPTY);
        focusIndicatorEditBox.setFocused(true);
        setFocusIndicatorEditBox(focusIndicatorEditBox);
    }

    // We use this text box to indicate to Forge that we want all input, and event handlers should not be allowed
    // to steal input from us (e.g. via custom key bindings). Since Forge is lazy and just uses getDeclaredFields
    // to get private fields, which completely skips fields in base classes, we require subclasses to hold the field...
    protected abstract void setFocusIndicatorEditBox(final EditBox editBox);
}
