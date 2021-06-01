package li.cil.oc2.common.container;

import li.cil.oc2.common.block.Blocks;
import li.cil.oc2.common.tileentity.ComputerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public final class ComputerTerminalContainer extends AbstractContainer {
    private static final int ENERGY_INFO_SIZE = 3;

    ///////////////////////////////////////////////////////////////////

    @Nullable
    public static ComputerTerminalContainer create(final int id, final Inventory playerInventory, final PacketBuffer data) {
        final BlockPos pos = data.readBlockPos();
        final BlockEntity tileEntity = playerInventory.player.getCommandSenderWorld().getBlockEntity(pos);
        if (!(tileEntity instanceof ComputerBlockEntity)) {
            return null;
        }
        return new ComputerTerminalContainer(id, playerInventory.player, (ComputerBlockEntity) tileEntity, new IntArray(3));
    }

    ///////////////////////////////////////////////////////////////////

    private final ComputerBlockEntity computer;
    private final IIntArray energyInfo;

    ///////////////////////////////////////////////////////////////////

    public ComputerTerminalContainer(final int id, final Player player, final ComputerBlockEntity computer, final IIntArray energyInfo) {
        super(Containers.COMPUTER_TERMINAL_CONTAINER, id);
        this.computer = computer;
        this.energyInfo = energyInfo;

        this.computer.addTerminalUser(player);

        checkContainerDataCount(energyInfo, ENERGY_INFO_SIZE);
        addDataSlots(energyInfo);
    }

    ///////////////////////////////////////////////////////////////////

    public ComputerBlockEntity getComputer() {
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
    public boolean stillValid(final Player player) {
        return stillValid(LevelPosCallable.create(computer.getLevel(), computer.getBlockPos()), player, Blocks.COMPUTER.get());
    }

    @Override
    public void removed(final Player player) {
        super.removed(player);

        this.computer.removeTerminalUser(player);
    }
}
