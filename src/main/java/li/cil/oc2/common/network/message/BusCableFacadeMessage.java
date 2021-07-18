package li.cil.oc2.common.network.message;

import li.cil.oc2.common.network.MessageUtils;
import li.cil.oc2.common.tileentity.BusCableTileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public final class BusCableFacadeMessage {
    private BlockPos pos;
    private ItemStack stack;

    public BusCableFacadeMessage(final BlockPos pos, final ItemStack stack) {
        this.pos = pos;
        this.stack = stack;
    }

    public BusCableFacadeMessage(final PacketBuffer buffer) {
        fromBytes(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    public static boolean handleMessage(final BusCableFacadeMessage message, final Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> MessageUtils.withClientTileEntityAt(message.pos, BusCableTileEntity.class,
                (tileEntity) -> tileEntity.setFacade(message.stack)));
        return true;
    }

    public void fromBytes(final PacketBuffer buffer) {
        pos = buffer.readBlockPos();
        stack = buffer.readItem();
    }

    public static void toBytes(final BusCableFacadeMessage message, final PacketBuffer buffer) {
        buffer.writeBlockPos(message.pos);
        buffer.writeItem(message.stack);
    }
}
