package li.cil.oc2.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.ComputerPowerMessage;
import li.cil.oc2.common.network.message.ComputerTerminalInputMessage;
import li.cil.oc2.common.tileentity.ComputerTileEntity;
import li.cil.oc2.common.vm.Terminal;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;

import java.nio.ByteBuffer;

public final class ComputerTerminalScreen extends Screen {
    private final ComputerTileEntity computer;
    private final ComputerTerminalWidget terminalWidget;

    ///////////////////////////////////////////////////////////////////

    public ComputerTerminalScreen(final ComputerTileEntity computer, final ITextComponent title) {
        super(title);
        this.computer = computer;
        this.terminalWidget = new ComputerTerminalWidget(computer.getTerminal());
    }

    ///////////////////////////////////////////////////////////////////


    @Override
    public void render(final MatrixStack matrixStack, final int mouseX, final int mouseY, final float partialTicks) {
        renderBackground(matrixStack);
        terminalWidget.renderBackground(matrixStack, mouseX, mouseY);

        super.render(matrixStack, mouseX, mouseY, partialTicks);
        terminalWidget.render(matrixStack, computer.getState().getBootError());
    }

    @Override
    public void tick() {
        super.tick();

        terminalWidget.tick();

        final ClientPlayerEntity player = getMinecraft().player;
        if (!player.isAlive() || !canInteractWith(player)) {
            closeScreen();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
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

    private boolean canInteractWith(final PlayerEntity player) {
        return Vector3d.copyCentered(computer.getPos()).isWithinDistanceOf(player.getPositionVec(), 8);
    }

    ///////////////////////////////////////////////////////////////////

    private final class ComputerTerminalWidget extends AbstractTerminalWidget {
        public ComputerTerminalWidget(final Terminal terminal) {
            super(ComputerTerminalScreen.this, terminal);
        }

        @Override
        protected boolean isRunning() {
            return computer.getState().isRunning();
        }

        @Override
        protected void addButton(final Widget widget) {
            ComputerTerminalScreen.this.addButton(widget);
        }

        @Override
        protected void sendPowerStateToServer(final boolean value) {
            Network.INSTANCE.sendToServer(new ComputerPowerMessage(computer, value));
        }

        @Override
        protected void sendTerminalInputToServer(final ByteBuffer input) {
            Network.INSTANCE.sendToServer(new ComputerTerminalInputMessage(computer, input));
        }
    }
}
