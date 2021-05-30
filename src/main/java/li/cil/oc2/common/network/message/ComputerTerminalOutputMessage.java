package li.cil.oc2.common.network.message;

import li.cil.oc2.common.network.MessageUtils;
import li.cil.oc2.common.tileentity.ComputerBlockEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.nio.ByteBuffer;
import java.util.function.Supplier;

public final class ComputerTerminalOutputMessage extends AbstractTerminalBlockMessage {
    public ComputerTerminalOutputMessage(final ComputerBlockEntity tileEntity, final ByteBuffer data) {
        super(tileEntity, data);
    }

    public ComputerTerminalOutputMessage(final PacketBuffer buffer) {
        super(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    public static boolean handleMessage(final AbstractTerminalBlockMessage message, final Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> MessageUtils.withClientBlockEntityAt(message.pos, ComputerBlockEntity.class,
                tileEntity -> tileEntity.getTerminal().putOutput(ByteBuffer.wrap(message.data))));
        return true;
    }
}
