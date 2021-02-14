package li.cil.oc2.common.container;

import li.cil.oc2.client.gui.AbstractTerminalWidget;
import li.cil.oc2.common.entity.RobotEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.IIntArray;
import net.minecraft.util.IntArray;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nullable;

public final class RobotTerminalContainer extends AbstractContainer {
    private static final int ENERGY_INFO_SIZE = 3;

    ///////////////////////////////////////////////////////////////////

    @Nullable
    public static RobotTerminalContainer create(final int id, final PlayerInventory inventory, final PacketBuffer data) {
        final int entityId = data.readVarInt();
        final Entity entity = inventory.player.getEntityWorld().getEntityByID(entityId);
        if (!(entity instanceof RobotEntity)) {
            return null;
        }
        return new RobotTerminalContainer(id, (RobotEntity) entity, new IntArray(3));
    }

    ///////////////////////////////////////////////////////////////////

    private final RobotEntity robot;
    private final IIntArray energyInfo;

    ///////////////////////////////////////////////////////////////////

    public RobotTerminalContainer(final int id, final RobotEntity robot, final IIntArray energyInfo) {
        super(Containers.ROBOT_TERMINAL_CONTAINER.get(), id);
        this.robot = robot;
        this.energyInfo = energyInfo;

        assertIntArraySize(energyInfo, ENERGY_INFO_SIZE);
        trackIntArray(energyInfo);

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

    public int getEnergy() {
        return energyInfo.get(0);
    }

    public int getEnergyCapacity() {
        return energyInfo.get(1);
    }

    public int getEnergyConsumption() {
        return energyInfo.get(2);
    }

    @Override
    public boolean canInteractWith(final PlayerEntity player) {
        return robot.isEntityInRange(player, 8);
    }
}
