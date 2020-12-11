package li.cil.oc2.common.container;

import li.cil.oc2.OpenComputers;
import li.cil.oc2.common.block.entity.ComputerTileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.util.IWorldPosCallable;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public final class ComputerContainer extends Container {
    private final ComputerTileEntity tileEntity;

    public ComputerContainer(final int id, @Nullable final ComputerTileEntity tileEntity) {
        super(OpenComputers.COMPUTER_CONTAINER.get(), id);
        this.tileEntity = tileEntity;
    }

    @Override
    public boolean canInteractWith(final PlayerEntity player) {
        if (tileEntity == null) return false;
        final World world = tileEntity.getWorld();
        if (world == null) return false;
        return isWithinUsableDistance(IWorldPosCallable.of(world, tileEntity.getPos()), player, OpenComputers.COMPUTER_BLOCK.get());
    }
}
