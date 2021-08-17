package li.cil.oc2.common.network.message;

import li.cil.oc2.common.network.MessageUtils;
import li.cil.oc2.common.tileentity.BusCableTileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.NetworkEvent;

public final class BusCableFacadeMessage extends AbstractMessage {
    private BlockPos pos;
    private ItemStack stack;

    ///////////////////////////////////////////////////////////////////

    public BusCableFacadeMessage(final BlockPos pos, final ItemStack stack) {
        this.pos = pos;
        this.stack = stack;
    }

    public BusCableFacadeMessage(final PacketBuffer buffer) {
        super(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void fromBytes(final PacketBuffer buffer) {
        pos = buffer.readBlockPos();
        stack = buffer.readItem();
    }

    @Override
    public void toBytes(final PacketBuffer buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeItem(stack);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void handleMessage(final NetworkEvent.Context context) {
        MessageUtils.withClientTileEntityAt(pos, BusCableTileEntity.class,
                (tileEntity) -> tileEntity.setFacade(stack));
    }
}
