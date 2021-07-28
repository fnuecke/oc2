package li.cil.oc2.common.container;

import li.cil.oc2.common.block.Blocks;
import li.cil.oc2.common.bus.CommonDeviceBusController;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.ComputerPowerMessage;
import li.cil.oc2.common.network.message.ComputerTerminalInputMessage;
import li.cil.oc2.common.network.message.OpenComputerInventoryMessage;
import li.cil.oc2.common.network.message.OpenComputerTerminalMessage;
import li.cil.oc2.common.tileentity.ComputerTileEntity;
import li.cil.oc2.common.vm.Terminal;
import li.cil.oc2.common.vm.VirtualMachine;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.util.IIntArray;
import net.minecraft.util.IWorldPosCallable;
import net.minecraftforge.energy.IEnergyStorage;

import java.nio.ByteBuffer;

public abstract class AbstractComputerContainer extends AbstractMachineTerminalContainer {
    private final ComputerTileEntity computer;

    ///////////////////////////////////////////////////////////////////

    protected AbstractComputerContainer(final ContainerType<?> type, final int id, final PlayerEntity player, final ComputerTileEntity computer, final IIntArray energyInfo) {
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
    public boolean stillValid(final PlayerEntity player) {
        return !computer.isRemoved() && stillValid(IWorldPosCallable.create(computer.getLevel(), computer.getBlockPos()), player, Blocks.COMPUTER.get());
    }

    @Override
    public void removed(final PlayerEntity player) {
        super.removed(player);

        this.computer.removeTerminalUser(player);
    }

    ///////////////////////////////////////////////////////////////////

    protected static IIntArray createEnergyInfo(final IEnergyStorage energy, final CommonDeviceBusController busController) {
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
