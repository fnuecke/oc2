package li.cil.oc2.client.gui;

import li.cil.oc2.common.entity.RobotEntity;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.RobotPowerMessage;
import li.cil.oc2.common.network.message.RobotTerminalInputMessage;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.ITextComponent;

import java.nio.ByteBuffer;

public final class RobotTerminalScreen extends AbstractTerminalScreen {
    private final RobotEntity robot;

    ///////////////////////////////////////////////////////////////////

    public RobotTerminalScreen(final RobotEntity robot, final ITextComponent title) {
        super(robot.getState(), robot.getTerminal(), title);
        this.robot = robot;
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void sendPowerStateToServer(final boolean value) {
        Network.INSTANCE.sendToServer(new RobotPowerMessage(robot, value));
    }

    @Override
    protected void sendTerminalInputToServer(final ByteBuffer input) {
        Network.INSTANCE.sendToServer(new RobotTerminalInputMessage(robot, input));
    }

    @Override
    protected boolean canInteractWith(final PlayerEntity player) {
        return robot.isEntityInRange(player, 8);
    }
}
