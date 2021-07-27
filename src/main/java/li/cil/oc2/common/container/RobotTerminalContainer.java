package li.cil.oc2.common.container;

import li.cil.oc2.client.gui.AbstractMachineTerminalWidget;
import li.cil.oc2.common.entity.RobotEntity;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.RobotPowerMessage;
import li.cil.oc2.common.network.message.RobotTerminalInputMessage;
import li.cil.oc2.common.vm.Terminal;
import li.cil.oc2.common.vm.VirtualMachine;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.IIntArray;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;

public final class RobotTerminalContainer extends AbstractMachineTerminalContainer {
    @Nullable
    public static RobotTerminalContainer create(final int id, final PlayerInventory inventory, final PacketBuffer data) {
        final int entityId = data.readVarInt();
        final Entity entity = inventory.player.getCommandSenderWorld().getEntity(entityId);
        if (!(entity instanceof RobotEntity)) {
            return null;
        }
        return new RobotTerminalContainer(id, (RobotEntity) entity, createEnergyInfo());
    }

    ///////////////////////////////////////////////////////////////////

    private final RobotEntity robot;

    ///////////////////////////////////////////////////////////////////

    public RobotTerminalContainer(final int id, final RobotEntity robot, final IIntArray energyInfo) {
        super(Containers.ROBOT_TERMINAL.get(), id, energyInfo);
        this.robot = robot;

        final ItemStackHandler inventory = robot.getInventory();
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            final int x = (AbstractMachineTerminalWidget.WIDTH - inventory.getSlots() * SLOT_SIZE) / 2 + 1 + slot * SLOT_SIZE;
            addSlot(new SlotItemHandler(inventory, slot, x, AbstractMachineTerminalWidget.HEIGHT + 4));
        }
    }

    ///////////////////////////////////////////////////////////////////

    public RobotEntity getRobot() {
        return robot;
    }

    @Override
    public VirtualMachine getVirtualMachine() {
        return robot.getVirtualMachine();
    }

    @Override
    public void sendPowerStateToServer(final boolean value) {
        Network.INSTANCE.sendToServer(new RobotPowerMessage(robot, value));
    }

    @Override
    public Terminal getTerminal() {
        return robot.getTerminal();
    }

    @Override
    public void sendTerminalInputToServer(final ByteBuffer input) {
        Network.INSTANCE.sendToServer(new RobotTerminalInputMessage(robot, input));
    }

    @Override
    public boolean stillValid(final PlayerEntity player) {
        return robot.isAlive() && robot.closerThan(player, 8);
    }
}
