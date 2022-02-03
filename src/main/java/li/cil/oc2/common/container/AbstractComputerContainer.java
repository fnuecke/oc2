/* SPDX-License-Identifier: MIT */

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
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;
import net.minecraftforge.energy.IEnergyStorage;

import java.nio.ByteBuffer;

public abstract class AbstractComputerContainer extends AbstractMachineTerminalContainer {
    private final ComputerBlockEntity computer;

    ///////////////////////////////////////////////////////////////////

    protected AbstractComputerContainer(final MenuType<?> type, final int id, final Player player, final ComputerBlockEntity computer, final IntPrecisionContainerData energyInfo) {
        super(type, id, energyInfo);
        this.computer = computer;

        this.computer.addTerminalUser(player);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void switchToInventory() {
        Network.sendToServer(new OpenComputerInventoryMessage(computer));
    }

    @Override
    public void switchToTerminal() {
        Network.sendToServer(new OpenComputerTerminalMessage(computer));
    }

    @Override
    public VirtualMachine getVirtualMachine() {
        return computer.getVirtualMachine();
    }

    @Override
    public void sendPowerStateToServer(final boolean value) {
        Network.sendToServer(new ComputerPowerMessage(computer, value));
    }

    @Override
    public Terminal getTerminal() {
        return computer.getTerminal();
    }

    @Override
    public void sendTerminalInputToServer(final ByteBuffer input) {
        Network.sendToServer(new ComputerTerminalInputMessage(computer, input));
    }

    @Override
    public boolean stillValid(final Player player) {
        if (computer.isRemoved()) { return false; }
        final Level level = computer.getLevel();
        return level != null && stillValid(ContainerLevelAccess.create(level, computer.getBlockPos()), player, Blocks.COMPUTER.get());
    }

    @Override
    public void removed(final Player player) {
        super.removed(player);

        this.computer.removeTerminalUser(player);
    }

    ///////////////////////////////////////////////////////////////////

    protected static IntPrecisionContainerData createEnergyInfo(final IEnergyStorage energy, final CommonDeviceBusController busController) {
        return new IntPrecisionContainerData.Server() {
            @Override
            public int getInt(final int index) {
                return switch (index) {
                    case AbstractMachineContainer.ENERGY_STORED_INDEX -> energy.getEnergyStored();
                    case AbstractMachineContainer.ENERGY_CAPACITY_INDEX -> energy.getMaxEnergyStored();
                    case AbstractMachineContainer.ENERGY_CONSUMPTION_INDEX -> busController.getEnergyConsumption();
                    default -> 0;
                };
            }

            @Override
            public int getIntCount() {
                return ENERGY_INFO_SIZE;
            }
        };
    }
}
