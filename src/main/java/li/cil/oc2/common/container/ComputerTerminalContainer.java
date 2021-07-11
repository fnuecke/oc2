package li.cil.oc2.common.container;

import li.cil.oc2.common.block.Blocks;
import li.cil.oc2.common.tileentity.ComputerTileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIntArray;
import net.minecraft.util.IWorldPosCallable;
import net.minecraft.util.IntArray;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;

public final class ComputerTerminalContainer extends AbstractContainer {
    private static final int ENERGY_INFO_SIZE = 3;

    ///////////////////////////////////////////////////////////////////

    @Nullable
    public static ComputerTerminalContainer create(final int id, final PlayerInventory playerInventory, final PacketBuffer data) {
        final BlockPos pos = data.readBlockPos();
        final TileEntity tileEntity = playerInventory.player.getCommandSenderWorld().getBlockEntity(pos);
        if (!(tileEntity instanceof ComputerTileEntity)) {
            return null;
        }
        return new ComputerTerminalContainer(id, playerInventory.player, (ComputerTileEntity) tileEntity, new IntArray(3));
    }

    ///////////////////////////////////////////////////////////////////

    private final ComputerTileEntity computer;
    private final IIntArray energyInfo;

    ///////////////////////////////////////////////////////////////////

    public ComputerTerminalContainer(final int id, final PlayerEntity player, final ComputerTileEntity computer, final IIntArray energyInfo) {
        super(Containers.COMPUTER_TERMINAL.get(), id);
        this.computer = computer;
        this.energyInfo = energyInfo;

        this.computer.addTerminalUser(player);

        checkContainerDataCount(energyInfo, ENERGY_INFO_SIZE);
        addDataSlots(energyInfo);
    }

    ///////////////////////////////////////////////////////////////////

    public ComputerTileEntity getComputer() {
        return computer;
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
    public boolean stillValid(final PlayerEntity player) {
        return stillValid(IWorldPosCallable.create(computer.getLevel(), computer.getBlockPos()), player, Blocks.COMPUTER.get());
    }

    @Override
    public void removed(final PlayerEntity player) {
        super.removed(player);

        this.computer.removeTerminalUser(player);
    }
}
