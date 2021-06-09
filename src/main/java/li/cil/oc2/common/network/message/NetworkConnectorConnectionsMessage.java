package li.cil.oc2.common.network.message;

import li.cil.oc2.common.network.MessageUtils;
import li.cil.oc2.common.tileentity.NetworkConnectorTileEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.ArrayList;
import java.util.function.Supplier;

public final class NetworkConnectorConnectionsMessage {
    private BlockPos pos;
    private ArrayList<BlockPos> connectedPositions;

    ///////////////////////////////////////////////////////////////////

    public NetworkConnectorConnectionsMessage(final NetworkConnectorTileEntity connector) {
        this.pos = connector.getBlockPos();
        this.connectedPositions = new ArrayList<>(connector.getConnectedPositions());
    }

    public NetworkConnectorConnectionsMessage(final PacketBuffer buffer) {
        fromBytes(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    public static boolean handleMessage(final NetworkConnectorConnectionsMessage message, final Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> MessageUtils.withClientTileEntityAt(message.pos, NetworkConnectorTileEntity.class,
                (tileEntity) -> tileEntity.setConnectedPositionsClient(message.connectedPositions)));
        return true;
    }

    public void fromBytes(final PacketBuffer buffer) {
        pos = buffer.readBlockPos();
        connectedPositions = new ArrayList<>();
        final int positionCount = buffer.readVarInt();
        for (int i = 0; i < positionCount; i++) {
            final BlockPos pos = buffer.readBlockPos();
            connectedPositions.add(pos);
        }
    }

    public static void toBytes(final NetworkConnectorConnectionsMessage message, final PacketBuffer buffer) {
        buffer.writeBlockPos(message.pos);
        buffer.writeVarInt(message.connectedPositions.size());
        for (final BlockPos pos : message.connectedPositions) {
            buffer.writeBlockPos(pos);
        }
    }
}
