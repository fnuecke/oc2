package li.cil.oc2.common.network.message;

import li.cil.oc2.common.network.MessageUtils;
import li.cil.oc2.common.blockentity.ComputerBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.nio.ByteBuffer;

public final class ComputerTerminalInputMessage extends AbstractTerminalBlockMessage {
    public ComputerTerminalInputMessage(final ComputerBlockEntity computer, final ByteBuffer data) {
        super(computer, data);
    }

    public ComputerTerminalInputMessage(final FriendlyByteBuf buffer) {
        super(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void handleMessage(final NetworkEvent.Context context) {
        MessageUtils.withNearbyServerBlockEntityAt(context, pos, ComputerBlockEntity.class,
            computer -> computer.getTerminal().putInput(ByteBuffer.wrap(data)));
    }
}
