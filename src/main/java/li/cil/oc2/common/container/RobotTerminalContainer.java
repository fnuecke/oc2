package li.cil.oc2.common.container;

import li.cil.oc2.client.gui.MachineTerminalWidget;
import li.cil.oc2.common.bus.CommonDeviceBusController;
import li.cil.oc2.common.energy.FixedEnergyStorage;
import li.cil.oc2.common.entity.RobotEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.IIntArray;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

public final class RobotTerminalContainer extends AbstractRobotContainer {
    public static void createServer(final RobotEntity robot, final FixedEnergyStorage energy, final CommonDeviceBusController busController, final ServerPlayerEntity player) {
        NetworkHooks.openGui(player, new INamedContainerProvider() {
            @Override
            public ITextComponent getDisplayName() {
                return robot.getName();
            }

            @Override
            public Container createMenu(final int id, final PlayerInventory inventory, final PlayerEntity player) {
                return new RobotTerminalContainer(id, robot, createEnergyInfo(energy, busController));
            }
        }, b -> b.writeVarInt(robot.getId()));
    }

    public static RobotTerminalContainer createClient(final int id, final PlayerInventory inventory, final PacketBuffer data) {
        final int entityId = data.readVarInt();
        final Entity entity = inventory.player.level.getEntity(entityId);
        if (!(entity instanceof RobotEntity)) {
            throw new IllegalArgumentException();
        }
        return new RobotTerminalContainer(id, (RobotEntity) entity, createEnergyInfo());
    }

    ///////////////////////////////////////////////////////////////////

    private RobotTerminalContainer(final int id, final RobotEntity robot, final IIntArray energyInfo) {
        super(Containers.ROBOT_TERMINAL.get(), id, robot, energyInfo);

        final ItemStackHandler inventory = robot.getInventory();
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            final int x = (MachineTerminalWidget.WIDTH - inventory.getSlots() * SLOT_SIZE) / 2 + 1 + slot * SLOT_SIZE;
            addSlot(new SlotItemHandler(inventory, slot, x, MachineTerminalWidget.HEIGHT + 4));
        }
    }
}
