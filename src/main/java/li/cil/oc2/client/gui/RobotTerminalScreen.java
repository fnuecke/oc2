package li.cil.oc2.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import li.cil.oc2.client.gui.widget.Sprite;
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
    private static final Sprite INVENTORY_BACKGROUND = new Sprite(AbstractTerminalWidget.BACKGROUND_LOCATION, AbstractTerminalWidget.TEXTURE_SIZE, 224, 26, 80, 300);

    private static final int SLOTS_X = (AbstractTerminalWidget.WIDTH - INVENTORY_BACKGROUND.width) / 2;
    private static final int SLOTS_Y = AbstractTerminalWidget.HEIGHT - 1;

    private final RobotTerminalWidget terminalWidget;

    ///////////////////////////////////////////////////////////////////

    public RobotTerminalScreen(final RobotTerminalContainer container, final PlayerInventory playerInventory, final ITextComponent title) {
        super(container, playerInventory, title);
        this.terminalWidget = new RobotTerminalWidget(container.getRobot().getTerminal());
        xSize = AbstractTerminalWidget.WIDTH;
        ySize = AbstractTerminalWidget.HEIGHT;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(final MatrixStack matrixStack, final float partialTicks, final int mouseX, final int mouseY) {
        INVENTORY_BACKGROUND.draw(matrixStack, guiLeft + SLOTS_X, guiTop + SLOTS_Y);
        terminalWidget.renderBackground(matrixStack, mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(final MatrixStack matrixStack, final int mouseX, final int mouseY) {
    }

    @Override
    public void render(final MatrixStack matrixStack, final int mouseX, final int mouseY, final float partialTicks) {
        terminalWidget.setEnergyInfo(container.getEnergy(), container.getEnergyCapacity(), container.getEnergyConsumption());

        renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        terminalWidget.render(matrixStack, mouseX, mouseY, container.getRobot().getVirtualMachine().getBootError());
        RobotContainerScreen.renderSelection(matrixStack, container.getRobot().getSelectedSlot(), guiLeft + SLOTS_X + 4, guiTop + SLOTS_Y + 4, 12);
        renderHoveredTooltip(matrixStack, mouseX, mouseY);
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
        final InputMappings.Input input = InputMappings.getInputByCode(keyCode, scanCode);
        if (this.minecraft.gameSettings.keyBindInventory.isActiveAndMatches(input)) {
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
            return container.getRobot().getVirtualMachine().isRunning();
        }

        @Override
        protected void addButton(final Widget widget) {
            RobotTerminalScreen.this.addButton(widget);
        }

        @Override
        protected void sendPowerStateToServer(final boolean value) {
            Network.INSTANCE.sendToServer(new RobotPowerMessage(container.getRobot(), value));
        }

        @Override
        protected void sendTerminalInputToServer(final ByteBuffer input) {
            Network.INSTANCE.sendToServer(new RobotTerminalInputMessage(container.getRobot(), input));
        }
    }
}
