package li.cil.oc2.common.container;

import li.cil.oc2.common.block.Blocks;
import li.cil.oc2.common.blockentity.ComputerBlockEntity;
import li.cil.oc2.common.bus.CommonDeviceBusController;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.ComputerPowerMessage;
import li.cil.oc2.common.network.message.ComputerTerminalInputMessage;
import li.cil.oc2.common.network.message.OpenComputerInventoryMessage;
import li.cil.oc2.common.network.message.OpenComputerTerminalMessage;
import li.cil.oc2.common.vm.Terminal;
import li.cil.oc2.common.vm.VirtualMachine;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.energy.IEnergyStorage;

import java.nio.ByteBuffer;

public abstract class AbstractComputerContainer extends AbstractMachineTerminalContainer {
    private final ComputerBlockEntity computer;

    ///////////////////////////////////////////////////////////////////

    protected AbstractComputerContainer(final MenuType<?> type, final int id, final Player player, final ComputerBlockEntity computer, final ContainerData energyInfo) {
        super(type, id, energyInfo);
        this.computer = computer;

        this.computer.addTerminalUser(player);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void switchToInventory() {
        Network.INSTANCE.sendToServer(new OpenComputerInventoryMessage(computer));
    }

    @Override
    public void switchToTerminal() {
        Network.INSTANCE.sendToServer(new OpenComputerTerminalMessage(computer));
    }

    @Override
    public VirtualMachine getVirtualMachine() {
        return computer.getVirtualMachine();
    }

    @Override
    public void sendPowerStateToServer(final boolean value) {
        Network.INSTANCE.sendToServer(new ComputerPowerMessage(computer, value));
    }

    @Override
    public Terminal getTerminal() {
        return computer.getTerminal();
    }

    @Override
    public void sendTerminalInputToServer(final ByteBuffer input) {
        Network.INSTANCE.sendToServer(new ComputerTerminalInputMessage(computer, input));
    }

    @Override
    public boolean stillValid(final Player player) {
        return !computer.isRemoved() && stillValid(ContainerLevelAccess.create(computer.getLevel(), computer.getBlockPos()), player, Blocks.COMPUTER.get());
    }

    @Override
    public void removed(final Player player) {
        super.removed(player);

        this.computer.removeTerminalUser(player);
    }

    ///////////////////////////////////////////////////////////////////

    protected static ContainerData createEnergyInfo(final IEnergyStorage energy, final CommonDeviceBusController busController) {
        return new ContainerData() {
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
