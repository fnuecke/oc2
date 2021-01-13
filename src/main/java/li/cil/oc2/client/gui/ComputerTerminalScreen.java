package li.cil.oc2.client.gui;

import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.ComputerPowerMessage;
import li.cil.oc2.common.network.message.ComputerTerminalInputMessage;
import li.cil.oc2.common.tileentity.ComputerTileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.ITextComponent;

import java.nio.ByteBuffer;

public final class ComputerTerminalScreen extends AbstractTerminalScreen {
    private final ComputerTileEntity computer;

    ///////////////////////////////////////////////////////////////////

    public ComputerTerminalScreen(final ComputerTileEntity computer, final ITextComponent title) {
        super(computer.getState(), computer.getTerminal(), title);
        this.computer = computer;
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void sendPowerStateToServer(final boolean value) {
        Network.INSTANCE.sendToServer(new ComputerPowerMessage(computer, value));
    }

    @Override
    protected void sendTerminalInputToServer(final ByteBuffer input) {
        Network.INSTANCE.sendToServer(new ComputerTerminalInputMessage(computer, input));
    }

    @Override
    protected boolean canInteractWith(final PlayerEntity player) {
        return computer.getPos().withinDistance(player.getPositionVec(), 8);
    }
}
