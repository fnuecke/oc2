package li.cil.oc2.common.network.message;

import li.cil.oc2.common.blockentity.NetworkConnectorBlockEntity;
import li.cil.oc2.common.network.MessageUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;

public final class NetworkConnectorConnectionsMessage extends AbstractMessage {
    private BlockPos pos;
    private ArrayList<BlockPos> connectedPositions;

    ///////////////////////////////////////////////////////////////////

    public NetworkConnectorConnectionsMessage(final NetworkConnectorBlockEntity networkConnector) {
        this.pos = networkConnector.getBlockPos();
        this.connectedPositions = new ArrayList<>(networkConnector.getConnectedPositions());
    }

    public NetworkConnectorConnectionsMessage(final FriendlyByteBuf buffer) {
        super(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void fromBytes(final FriendlyByteBuf buffer) {
        pos = buffer.readBlockPos();
        connectedPositions = new ArrayList<>();
        final int positionCount = buffer.readVarInt();
        for (int i = 0; i < positionCount; i++) {
            final BlockPos pos = buffer.readBlockPos();
            connectedPositions.add(pos);
        }
    }

    @Override
    public void toBytes(final FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeVarInt(connectedPositions.size());
        for (final BlockPos pos : connectedPositions) {
            buffer.writeBlockPos(pos);
        }
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void handleMessage(final NetworkEvent.Context context) {
        MessageUtils.withClientBlockEntityAt(pos, NetworkConnectorBlockEntity.class,
            networkConnector -> networkConnector.setConnectedPositionsClient(connectedPositions));
    }
}
