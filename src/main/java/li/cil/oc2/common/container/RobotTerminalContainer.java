package li.cil.oc2.common.container;

import li.cil.oc2.client.gui.AbstractTerminalWidget;
import li.cil.oc2.common.entity.RobotEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraftforge.items.ContainerHelper;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

public final class RobotTerminalContainer extends AbstractContainerMenu {
    private static final int ENERGY_INFO_SIZE = 3;

    ///////////////////////////////////////////////////////////////////

    @Nullable
    public static RobotTerminalContainer create(final int id, final Inventory inventory, final FriendlyByteBuf data) {
        final int entityId = data.readVarInt();
        final Entity entity = inventory.player.getCommandSenderWorld().getEntity(entityId);
        if (!(entity instanceof RobotEntity)) {
            return null;
        }
        return new RobotTerminalContainer(id, (RobotEntity) entity, new SimpleContainerData(3));
    }

    ///////////////////////////////////////////////////////////////////

    private final RobotEntity robot;
    private final ContainerData energyInfo;

    ///////////////////////////////////////////////////////////////////

    public RobotTerminalContainer(final int id, final RobotEntity robot, final ContainerData energyInfo) {
        super(Containers.ROBOT_TERMINAL_CONTAINER.get(), id);
        this.robot = robot;
        this.energyInfo = energyInfo;

        checkContainerDataCount(energyInfo, ENERGY_INFO_SIZE);
        addDataSlots(energyInfo);

        final ContainerHelper inventory = robot.getInventory();
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
    public boolean stillValid(final Player player) {
        return robot.closerThan(player, 8);
    }
}
