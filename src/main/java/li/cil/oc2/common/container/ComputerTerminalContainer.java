package li.cil.oc2.common.container;

import li.cil.oc2.common.block.Blocks;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.ComputerPowerMessage;
import li.cil.oc2.common.network.message.ComputerTerminalInputMessage;
import li.cil.oc2.common.tileentity.ComputerTileEntity;
import li.cil.oc2.common.vm.Terminal;
import li.cil.oc2.common.vm.VirtualMachine;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIntArray;
import net.minecraft.util.IWorldPosCallable;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;

public final class ComputerTerminalContainer extends AbstractMachineTerminalContainer {
    @Nullable
    public static ComputerTerminalContainer create(final int id, final PlayerInventory playerInventory, final PacketBuffer data) {
        final BlockPos pos = data.readBlockPos();
        final TileEntity tileEntity = playerInventory.player.getCommandSenderWorld().getBlockEntity(pos);
        if (!(tileEntity instanceof ComputerTileEntity)) {
            return null;
        }
        return new ComputerTerminalContainer(id, playerInventory.player, (ComputerTileEntity) tileEntity, createEnergyInfo());
    }

    ///////////////////////////////////////////////////////////////////

    private final ComputerTileEntity computer;

    ///////////////////////////////////////////////////////////////////

    public ComputerTerminalContainer(final int id, final PlayerEntity player, final ComputerTileEntity computer, final IIntArray energyInfo) {
        super(Containers.COMPUTER_TERMINAL.get(), id, energyInfo);
        this.computer = computer;

        this.computer.addTerminalUser(player);
    }

    ///////////////////////////////////////////////////////////////////

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
}
