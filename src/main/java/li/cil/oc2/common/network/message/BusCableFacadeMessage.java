package li.cil.oc2.common.network.message;

import li.cil.oc2.common.network.MessageUtils;
import li.cil.oc2.common.blockentity.BusCableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

public final class BusCableFacadeMessage extends AbstractMessage {
    private BlockPos pos;
    private ItemStack stack;

    ///////////////////////////////////////////////////////////////////

    public BusCableFacadeMessage(final BlockPos pos, final ItemStack stack) {
        this.pos = pos;
        this.stack = stack;
    }

    public BusCableFacadeMessage(final FriendlyByteBuf buffer) {
        super(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void fromBytes(final FriendlyByteBuf buffer) {
        pos = buffer.readBlockPos();
        stack = buffer.readItem();
    }

    @Override
    public void toBytes(final FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeItem(stack);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void handleMessage(final NetworkEvent.Context context) {
        MessageUtils.withClientBlockEntityAt(pos, BusCableBlockEntity.class,
            busCable -> busCable.setFacade(stack));
    }
}
