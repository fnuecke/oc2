package li.cil.oc2.common.network.message;

import li.cil.oc2.common.network.MessageUtils;
import li.cil.oc2.common.tileentity.NetworkConnectorTileEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.ArrayList;

public final class NetworkConnectorConnectionsMessage extends AbstractMessage {
    private BlockPos pos;
    private ArrayList<BlockPos> connectedPositions;

    ///////////////////////////////////////////////////////////////////

    public NetworkConnectorConnectionsMessage(final NetworkConnectorTileEntity connector) {
        this.pos = connector.getBlockPos();
        this.connectedPositions = new ArrayList<>(connector.getConnectedPositions());
    }

    public NetworkConnectorConnectionsMessage(final PacketBuffer buffer) {
        super(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void fromBytes(final PacketBuffer buffer) {
        pos = buffer.readBlockPos();
        connectedPositions = new ArrayList<>();
        final int positionCount = buffer.readVarInt();
        for (int i = 0; i < positionCount; i++) {
            final BlockPos pos = buffer.readBlockPos();
            connectedPositions.add(pos);
        }
    }

    @Override
    public void toBytes(final PacketBuffer buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeVarInt(connectedPositions.size());
        for (final BlockPos pos : connectedPositions) {
            buffer.writeBlockPos(pos);
        }
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void handleMessage(final NetworkEvent.Context context) {
        MessageUtils.withClientTileEntityAt(pos, NetworkConnectorTileEntity.class,
                (tileEntity) -> tileEntity.setConnectedPositionsClient(connectedPositions));
    }
}
