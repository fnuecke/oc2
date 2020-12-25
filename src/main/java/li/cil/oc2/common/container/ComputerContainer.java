package li.cil.oc2.common.container;

import li.cil.oc2.common.block.entity.ComputerTileEntity;
import li.cil.oc2.common.init.Blocks;
import li.cil.oc2.common.init.Containers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IWorldPosCallable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nullable;

public final class ComputerContainer extends AbstractContainer {
    @Nullable
    public static ComputerContainer create(final int id, final PlayerInventory inventory, final PacketBuffer data) {
        final BlockPos pos = data.readBlockPos();
        final TileEntity tileEntity = inventory.player.getEntityWorld().getTileEntity(pos);
        if (!(tileEntity instanceof ComputerTileEntity)) {
            return null;
        }
        return new ComputerContainer(id, (ComputerTileEntity) tileEntity, inventory);
    }

    ///////////////////////////////////////////////////////////////////

    private final World world;
    private final BlockPos pos;

    ///////////////////////////////////////////////////////////////////

    public ComputerContainer(final int id, final ComputerTileEntity tileEntity, final PlayerInventory inventory) {
        super(Containers.COMPUTER_CONTAINER.get(), id);
        this.world = inventory.player.getEntityWorld();
        this.pos = tileEntity.getPos();

        final IItemHandler itemHandler = tileEntity.getItemHandler();

        for (int i = 0; i < itemHandler.getSlots(); ++i) {
            this.addSlot(new SlotItemHandler(itemHandler, i, 8 + i * SLOT_SIZE, 20));
        }

        createPlayerInventoryAndHotbarSlots(inventory, 8, 51);
    }

    @Override
    public boolean canInteractWith(final PlayerEntity player) {
        return isWithinUsableDistance(IWorldPosCallable.of(world, pos), player, Blocks.COMPUTER_BLOCK.get());
    }
}
