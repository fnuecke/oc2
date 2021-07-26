package li.cil.oc2.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import li.cil.oc2.common.container.RobotTerminalContainer;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.RobotPowerMessage;
import li.cil.oc2.common.network.message.RobotTerminalInputMessage;
import li.cil.oc2.common.vm.Terminal;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.util.InputMappings;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;

import java.nio.ByteBuffer;

public final class RobotTerminalScreen extends ContainerScreen<RobotTerminalContainer> {
    private static final int SLOTS_X = (AbstractTerminalWidget.WIDTH - Sprites.HOTBAR.width) / 2;
    private static final int SLOTS_Y = AbstractTerminalWidget.HEIGHT - 1;

    private final RobotTerminalWidget terminalWidget;

    ///////////////////////////////////////////////////////////////////

    public RobotTerminalScreen(final RobotTerminalContainer container, final PlayerInventory playerInventory, final ITextComponent title) {
        super(container, playerInventory, title);
        this.terminalWidget = new RobotTerminalWidget(container.getRobot().getTerminal());
        imageWidth = AbstractTerminalWidget.WIDTH;
        imageHeight = AbstractTerminalWidget.HEIGHT;
    }

    @Override
    protected void renderBg(final MatrixStack matrixStack, final float partialTicks, final int mouseX, final int mouseY) {
        Sprites.HOTBAR.draw(matrixStack, leftPos + SLOTS_X, topPos + SLOTS_Y);
        terminalWidget.renderBackground(matrixStack, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(final MatrixStack matrixStack, final int mouseX, final int mouseY) {

    }

    @Override
    public void render(final MatrixStack matrixStack, final int mouseX, final int mouseY, final float partialTicks) {
        terminalWidget.setEnergyInfo(menu.getEnergy(), menu.getEnergyCapacity(), menu.getEnergyConsumption());

        renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        terminalWidget.render(matrixStack, mouseX, mouseY, menu.getRobot().getVirtualMachine().getBootError());
        RobotContainerScreen.renderSelection(matrixStack, menu.getRobot().getSelectedSlot(), leftPos + SLOTS_X + 4, topPos + SLOTS_Y + 4, 12);
        renderTooltip(matrixStack, mouseX, mouseY);
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

    private final class RobotTerminalWidget extends AbstractTerminalWidget {
        public RobotTerminalWidget(final Terminal terminal) {
            super(RobotTerminalScreen.this, terminal);
        }

        @Override
        protected boolean isRunning() {
            return menu.getRobot().getVirtualMachine().isRunning();
        }

        @Override
        protected void addButton(final Widget widget) {
            RobotTerminalScreen.this.addButton(widget);
        }

        @Override
        protected void sendPowerStateToServer(final boolean value) {
            Network.INSTANCE.sendToServer(new RobotPowerMessage(menu.getRobot(), value));
        }

        @Override
        protected void sendTerminalInputToServer(final ByteBuffer input) {
            Network.INSTANCE.sendToServer(new RobotTerminalInputMessage(menu.getRobot(), input));
        }
    }
}
