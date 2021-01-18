package li.cil.oc2.common.container;

import li.cil.oc2.client.gui.AbstractTerminalWidget;
import li.cil.oc2.common.entity.RobotEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nullable;

public final class RobotTerminalContainer extends AbstractContainer {
    @Nullable
    public static RobotTerminalContainer create(final int id, final PlayerInventory inventory, final PacketBuffer data) {
        final int entityId = data.readVarInt();
        final Entity entity = inventory.player.getEntityWorld().getEntityByID(entityId);
        if (!(entity instanceof RobotEntity)) {
            return null;
        }
        return new RobotTerminalContainer(id, (RobotEntity) entity);
    }

    ///////////////////////////////////////////////////////////////////

    private final RobotEntity robot;

    ///////////////////////////////////////////////////////////////////

    public RobotTerminalContainer(final int id, final RobotEntity robot) {
        super(Containers.ROBOT_TERMINAL_CONTAINER.get(), id);
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
    public boolean canInteractWith(final PlayerEntity player) {
        return robot.isEntityInRange(player, 8);
    }
}
