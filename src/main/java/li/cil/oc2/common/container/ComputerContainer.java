package li.cil.oc2.common.container;

import li.cil.oc2.common.block.entity.ComputerTileEntity;
import li.cil.oc2.common.init.Blocks;
import li.cil.oc2.common.init.Containers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IWorldPosCallable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public final class ComputerContainer extends Container {
    @Nullable
    public static ComputerContainer create(final int id, final PlayerInventory inventory, final PacketBuffer data) {
        final BlockPos pos = data.readBlockPos();
        final TileEntity tileEntity = inventory.player.getEntityWorld().getTileEntity(pos);
        if (!(tileEntity instanceof ComputerTileEntity)) {
            return null;
        }
        return new ComputerContainer(id, (ComputerTileEntity) tileEntity);
    }

    ///////////////////////////////////////////////////////////////////

    private final ComputerTileEntity tileEntity;

    ///////////////////////////////////////////////////////////////////

    public ComputerContainer(final int id, @Nullable final ComputerTileEntity tileEntity) {
        super(Containers.COMPUTER_CONTAINER.get(), id);
        this.tileEntity = tileEntity;
    }

    @Override
    public boolean canInteractWith(final PlayerEntity player) {
        if (tileEntity == null) return false;
        final World world = tileEntity.getWorld();
        if (world == null) return false;
        return isWithinUsableDistance(IWorldPosCallable.of(world, tileEntity.getPos()), player, Blocks.COMPUTER_BLOCK.get());
    }
}
