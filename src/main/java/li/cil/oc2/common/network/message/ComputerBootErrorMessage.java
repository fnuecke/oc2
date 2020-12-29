package li.cil.oc2.common.network.message;

import li.cil.oc2.common.network.MessageUtils;
import li.cil.oc2.common.tileentity.ComputerTileEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public final class ComputerBootErrorMessage {
    private BlockPos pos;
    private ITextComponent value;

    ///////////////////////////////////////////////////////////////////

    public ComputerBootErrorMessage(final ComputerTileEntity tileEntity) {
        this.pos = tileEntity.getPos();
        this.value = tileEntity.getBootError();
    }

    public ComputerBootErrorMessage(final PacketBuffer buffer) {
        fromBytes(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    public static boolean handleMessage(final ComputerBootErrorMessage message, final Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> MessageUtils.withClientTileEntityAt(message.pos, ComputerTileEntity.class,
                (tileEntity) -> tileEntity.setBootErrorClient(message.value)));
        return true;
    }

    public void fromBytes(final PacketBuffer buffer) {
        pos = buffer.readBlockPos();
        value = buffer.readTextComponent();
    }

    public static void toBytes(final ComputerBootErrorMessage message, final PacketBuffer buffer) {
        buffer.writeBlockPos(message.pos);
        buffer.writeTextComponent(message.value);
    }
}
