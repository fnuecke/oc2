package li.cil.oc2.common.network.message;

import li.cil.oc2.common.network.MessageUtils;
import li.cil.oc2.common.tileentity.BusCableBlockEntity;
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

    protected BusInterfaceNameMessage(final BusCableBlockEntity tileEntity, final Direction side, final String value) {
        this.pos = tileEntity.getBlockPos();
        this.side = side;
        this.value = value;
    }

    protected BusInterfaceNameMessage(final PacketBuffer buffer) {
        fromBytes(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    public static boolean handleMessageClient(final BusInterfaceNameMessage message, final Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> MessageUtils.withClientBlockEntityAt(message.pos, BusCableBlockEntity.class,
                (tileEntity) -> tileEntity.setInterfaceName(message.side, message.value)));
        return true;
    }

    public static boolean handleMessageServer(final BusInterfaceNameMessage message, final Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> MessageUtils.withServerBlockEntityAt(context, message.pos, BusCableBlockEntity.class,
                (tileEntity) -> {
                    final Vector3d busCableCenter = Vector3d.atCenterOf(tileEntity.getBlockPos());
                    if (context.get().getSender().distanceToSqr(busCableCenter) <= 8 * 8) {
                        tileEntity.setInterfaceName(message.side, message.value);
                    }
                }));
        return true;
    }

    public void fromBytes(final PacketBuffer buffer) {
        pos = buffer.readBlockPos();
        side = buffer.readEnum(Direction.class);
        value = buffer.readUtf(32);
    }

    public static void toBytes(final BusInterfaceNameMessage message, final PacketBuffer buffer) {
        buffer.writeBlockPos(message.pos);
        buffer.writeEnum(message.side);
        buffer.writeUtf(message.value, 32);
    }

    ///////////////////////////////////////////////////////////////////

    public static final class ToClient extends BusInterfaceNameMessage {
        public ToClient(final BusCableBlockEntity tileEntity, final Direction side, final String value) {
            super(tileEntity, side, value);
        }

        public ToClient(final PacketBuffer buffer) {
            super(buffer);
        }
    }

    public static final class ToServer extends BusInterfaceNameMessage {
        public ToServer(final BusCableBlockEntity tileEntity, final Direction side, final String value) {
            super(tileEntity, side, value);
        }

        public ToServer(final PacketBuffer buffer) {
            super(buffer);
        }
    }
}
