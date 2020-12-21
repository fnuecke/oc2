package li.cil.oc2.common.container;

import li.cil.oc2.common.block.entity.ComputerTileEntity;
import li.cil.oc2.common.init.Blocks;
import li.cil.oc2.common.init.Containers;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public final class ComputerContainer extends ScreenHandler {
    @Nullable
    public static ComputerContainer create(final int id, final PlayerInventory inventory, final PacketByteBuf data) {
        final BlockPos pos = data.readBlockPos();
        final BlockEntity tileEntity = inventory.player.getEntityWorld().getBlockEntity(pos);
        if (!(tileEntity instanceof ComputerTileEntity)) {
            return null;
        }
        return new ComputerContainer(id, (ComputerTileEntity) tileEntity);
    }

    ///////////////////////////////////////////////////////////////////

    private final ComputerTileEntity tileEntity;

    ///////////////////////////////////////////////////////////////////

    public ComputerContainer(final int id, @Nullable final ComputerTileEntity tileEntity) {
        super(Containers.COMPUTER_CONTAINER, id);
        this.tileEntity = tileEntity;
    }

    @Override
    public boolean canUse(final PlayerEntity player) {
        if (tileEntity == null) return false;
        final World world = tileEntity.getWorld();
        if (world == null) return false;
        return canUse(ScreenHandlerContext.create(world, tileEntity.getPos()), player, Blocks.COMPUTER_BLOCK);
    }
}
