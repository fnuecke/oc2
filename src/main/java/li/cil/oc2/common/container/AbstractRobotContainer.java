package li.cil.oc2.common.container;

import li.cil.oc2.common.bus.CommonDeviceBusController;
import li.cil.oc2.common.energy.FixedEnergyStorage;
import li.cil.oc2.common.entity.RobotEntity;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.OpenRobotInventoryMessage;
import li.cil.oc2.common.network.message.OpenRobotTerminalMessage;
import li.cil.oc2.common.network.message.RobotPowerMessage;
import li.cil.oc2.common.network.message.RobotTerminalInputMessage;
import li.cil.oc2.common.vm.Terminal;
import li.cil.oc2.common.vm.VirtualMachine;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.util.IIntArray;

import java.nio.ByteBuffer;

public abstract class AbstractRobotContainer extends AbstractMachineTerminalContainer {
    private final RobotEntity robot;

    ///////////////////////////////////////////////////////////////////

    public AbstractRobotContainer(final ContainerType<?> type, final int id, final RobotEntity robot, final IIntArray energyInfo) {
        super(type, id, energyInfo);
        this.robot = robot;
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void switchToInventory() {
        Network.INSTANCE.sendToServer(new OpenRobotInventoryMessage(robot));
    }

    @Override
    public void switchToTerminal() {
        Network.INSTANCE.sendToServer(new OpenRobotTerminalMessage(robot));
    }

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

    ///////////////////////////////////////////////////////////////////

    protected static IIntArray createEnergyInfo(final FixedEnergyStorage energy, final CommonDeviceBusController busController) {
        return new IIntArray() {
            @Override
            public int get(final int index) {
                switch (index) {
                    case AbstractMachineContainer.ENERGY_STORED_INDEX:
                        return energy.getEnergyStored();
                    case AbstractMachineContainer.ENERGY_CAPACITY_INDEX:
                        return energy.getMaxEnergyStored();
                    case AbstractMachineContainer.ENERGY_CONSUMPTION_INDEX:
                        return busController.getEnergyConsumption();
                    default:
                        return 0;
                }
            }

            @Override
            public void set(final int index, final int value) {
            }

            @Override
            public int getCount() {
                return ENERGY_INFO_SIZE;
            }
        };
    }
}
