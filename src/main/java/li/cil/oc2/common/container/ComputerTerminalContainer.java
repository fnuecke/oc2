package li.cil.oc2.common.container;

import li.cil.oc2.common.bus.CommonDeviceBusController;
import li.cil.oc2.common.tileentity.ComputerTileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIntArray;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.network.NetworkHooks;

public final class ComputerTerminalContainer extends AbstractComputerContainer {
    public static void createServer(final ComputerTileEntity computer, final IEnergyStorage energy, final CommonDeviceBusController busController, final ServerPlayerEntity player) {
        NetworkHooks.openGui(player, new INamedContainerProvider() {
            @Override
            public ITextComponent getDisplayName() {
                return new TranslationTextComponent(computer.getBlockState().getBlock().getDescriptionId());
            }

            @Override
            public Container createMenu(final int id, final PlayerInventory inventory, final PlayerEntity player) {
                return new ComputerTerminalContainer(id, player, computer, createEnergyInfo(energy, busController));
            }
        }, computer.getBlockPos());
    }

    public static ComputerTerminalContainer createClient(final int id, final PlayerInventory playerInventory, final PacketBuffer data) {
        final BlockPos pos = data.readBlockPos();
        final TileEntity tileEntity = playerInventory.player.level.getBlockEntity(pos);
        if (!(tileEntity instanceof ComputerTileEntity)) {
            throw new IllegalArgumentException();
        }
        return new ComputerTerminalContainer(id, playerInventory.player, (ComputerTileEntity) tileEntity, createEnergyInfo());
    }

    ///////////////////////////////////////////////////////////////////

    private ComputerTerminalContainer(final int id, final PlayerEntity player, final ComputerTileEntity computer, final IIntArray energyInfo) {
        super(Containers.COMPUTER_TERMINAL.get(), id, player, computer, energyInfo);
    }
}
