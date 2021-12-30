package li.cil.oc2.common.container;

import li.cil.oc2.common.bus.CommonDeviceBusController;
import li.cil.oc2.common.tileentity.ComputerTileEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.MenuProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.network.NetworkHooks;

public final class ComputerTerminalContainer extends AbstractComputerContainer {
    public static void createServer(final ComputerTileEntity computer, final IEnergyStorage energy, final CommonDeviceBusController busController, final ServerPlayer player) {
        NetworkHooks.openGui(player, new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return new TranslatableComponent(computer.getBlockState().getBlock().getDescriptionId());
            }

            @Override
            public AbstractContainerMenu createMenu(final int id, final Inventory inventory, final Player player) {
                return new ComputerTerminalContainer(id, player, computer, createEnergyInfo(energy, busController));
            }
        }, computer.getBlockPos());
    }

    public static ComputerTerminalContainer createClient(final int id, final Inventory playerInventory, final FriendlyByteBuf data) {
        final BlockPos pos = data.readBlockPos();
        final BlockEntity tileEntity = playerInventory.player.level.getBlockEntity(pos);
        if (!(tileEntity instanceof ComputerTileEntity)) {
            throw new IllegalArgumentException();
        }
        return new ComputerTerminalContainer(id, playerInventory.player, (ComputerTileEntity) tileEntity, createEnergyInfo());
    }

    ///////////////////////////////////////////////////////////////////

    private ComputerTerminalContainer(final int id, final Player player, final ComputerTileEntity computer, final ContainerData energyInfo) {
        super(Containers.COMPUTER_TERMINAL.get(), id, player, computer, energyInfo);
    }
}
