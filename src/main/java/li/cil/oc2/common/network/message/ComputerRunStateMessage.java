package li.cil.oc2.common.network.message;

import li.cil.oc2.common.block.entity.ComputerTileEntity;
import li.cil.oc2.common.network.MessageUtils;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class ComputerRunStateMessage {
    private BlockPos pos;
    private ComputerTileEntity.RunState runState;

    public ComputerRunStateMessage(final ComputerTileEntity tileEntity) {
        this.pos = tileEntity.getPos();
        this.runState = tileEntity.getRunState();
    }

    public ComputerRunStateMessage(final PacketBuffer buffer) {
        fromBytes(buffer);
    }

    public static boolean handleMessage(final ComputerRunStateMessage message, final Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> MessageUtils.withClientTileEntityAt(message.pos, ComputerTileEntity.class,
                (tileEntity) -> tileEntity.setRunStateClient(message.runState)));
        return true;
    }

    public void fromBytes(final PacketBuffer buffer) {
        pos = buffer.readBlockPos();
        runState = buffer.readEnumValue(ComputerTileEntity.RunState.class);
    }

    public static void toBytes(final ComputerRunStateMessage message, final PacketBuffer buffer) {
        buffer.writeBlockPos(message.pos);
        buffer.writeEnumValue(message.runState);
    }
}
