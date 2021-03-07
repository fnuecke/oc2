package li.cil.oc2.common.network.message;

import li.cil.oc2.common.network.MessageUtils;
import li.cil.oc2.common.tileentity.BusCableTileEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public abstract class BusInterfaceNameMessage {
    private BlockPos pos;
    private Direction side;
    private String value;

    ///////////////////////////////////////////////////////////////////

    protected BusInterfaceNameMessage(final BusCableTileEntity tileEntity, final Direction side, final String value) {
        this.pos = tileEntity.getPos();
        this.side = side;
        this.value = value;
    }

    protected BusInterfaceNameMessage(final PacketBuffer buffer) {
        fromBytes(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    public static boolean handleMessageClient(final BusInterfaceNameMessage message, final Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> MessageUtils.withClientTileEntityAt(message.pos, BusCableTileEntity.class,
                (tileEntity) -> tileEntity.setInterfaceName(message.side, message.value)));
        return true;
    }

    public static boolean handleMessageServer(final BusInterfaceNameMessage message, final Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> MessageUtils.withServerTileEntityAt(context, message.pos, BusCableTileEntity.class,
                (tileEntity) -> {
                    final Vector3d busCableCenter = Vector3d.copyCentered(tileEntity.getPos());
                    if (context.get().getSender().getDistanceSq(busCableCenter) <= 8 * 8) {
                        tileEntity.setInterfaceName(message.side, message.value);
                    }
                }));
        return true;
    }

    public void fromBytes(final PacketBuffer buffer) {
        pos = buffer.readBlockPos();
        side = buffer.readEnumValue(Direction.class);
        value = buffer.readString(32);
    }

    public static void toBytes(final BusInterfaceNameMessage message, final PacketBuffer buffer) {
        buffer.writeBlockPos(message.pos);
        buffer.writeEnumValue(message.side);
        buffer.writeString(message.value, 32);
    }

    ///////////////////////////////////////////////////////////////////

    public static final class ToClient extends BusInterfaceNameMessage {
        public ToClient(final BusCableTileEntity tileEntity, final Direction side, final String value) {
            super(tileEntity, side, value);
        }

        public ToClient(final PacketBuffer buffer) {
            super(buffer);
        }
    }

    public static final class ToServer extends BusInterfaceNameMessage {
        public ToServer(final BusCableTileEntity tileEntity, final Direction side, final String value) {
            super(tileEntity, side, value);
        }

        public ToServer(final PacketBuffer buffer) {
            super(buffer);
        }
    }
}
