package li.cil.oc2.common.container;

import li.cil.oc2.client.gui.AbstractTerminalWidget;
import li.cil.oc2.common.entity.RobotEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.IIntArray;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nullable;

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
            final int x = (AbstractTerminalWidget.WIDTH - inventory.getSlots() * SLOT_SIZE) / 2 + 1 + slot * SLOT_SIZE;
            addSlot(new SlotItemHandler(inventory, slot, x, AbstractTerminalWidget.HEIGHT + 4));
        }
    }

    ///////////////////////////////////////////////////////////////////

    public RobotEntity getRobot() {
        return robot;
    }

    @Override
    public boolean stillValid(final PlayerEntity player) {
        return robot.isAlive() && robot.closerThan(player, 8);
    }
}
