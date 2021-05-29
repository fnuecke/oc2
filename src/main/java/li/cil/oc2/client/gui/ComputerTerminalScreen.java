package li.cil.oc2.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import li.cil.oc2.common.container.ComputerTerminalContainer;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.ComputerPowerMessage;
import li.cil.oc2.common.network.message.ComputerTerminalInputMessage;
import li.cil.oc2.common.vm.Terminal;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.util.InputMappings;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;

import java.nio.ByteBuffer;

public final class ComputerTerminalScreen extends ContainerScreen<ComputerTerminalContainer> {
    private final ComputerTerminalWidget terminalWidget;

    ///////////////////////////////////////////////////////////////////

    public ComputerTerminalScreen(final ComputerTerminalContainer container, final PlayerInventory playerInventory, final ITextComponent title) {
        super(container, playerInventory, title);
        this.terminalWidget = new ComputerTerminalWidget(container.getComputer().getTerminal());
        width = AbstractTerminalWidget.WIDTH;
        height = AbstractTerminalWidget.HEIGHT;
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void renderBg(final MatrixStack matrixStack, final float partialTicks, final int mouseX, final int mouseY) {
        terminalWidget.renderBackground(matrixStack, mouseX, mouseY);
    }

    @Override
    public void render(final MatrixStack matrixStack, final int mouseX, final int mouseY, final float partialTicks) {
        terminalWidget.setEnergyInfo(menu.getEnergy(), menu.getEnergyCapacity(), menu.getEnergyConsumption());

        renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        terminalWidget.render(matrixStack, mouseX, mouseY, menu.getComputer().getVirtualMachine().getBootError());
    }

    @Override
    public void tick() {
        super.tick();

        terminalWidget.tick();
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
        final InputMappings.Input input = InputMappings.getKey(keyCode, scanCode);
        if (this.minecraft.options.keyInventory.isActiveAndMatches(input)) {
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void init() {
        super.init();
        terminalWidget.init();
    }

    @Override
    public void onClose() {
        super.onClose();
        terminalWidget.onClose();
    }

    ///////////////////////////////////////////////////////////////////

    private final class ComputerTerminalWidget extends AbstractTerminalWidget {
        public ComputerTerminalWidget(final Terminal terminal) {
            super(ComputerTerminalScreen.this, terminal);
        }

        @Override
        protected boolean isRunning() {
            return menu.getComputer().getVirtualMachine().isRunning();
        }

        @Override
        protected void addButton(final Widget widget) {
            ComputerTerminalScreen.this.addButton(widget);
        }

        @Override
        protected void sendPowerStateToServer(final boolean value) {
            Network.INSTANCE.sendToServer(new ComputerPowerMessage(menu.getComputer(), value));
        }

        @Override
        protected void sendTerminalInputToServer(final ByteBuffer input) {
            Network.INSTANCE.sendToServer(new ComputerTerminalInputMessage(menu.getComputer(), input));
        }
    }
}
