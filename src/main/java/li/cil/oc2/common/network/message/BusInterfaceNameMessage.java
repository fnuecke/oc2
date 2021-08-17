package li.cil.oc2.common.network.message;

import li.cil.oc2.common.network.MessageUtils;
import li.cil.oc2.common.tileentity.BusCableTileEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.NetworkEvent;

public abstract class BusInterfaceNameMessage extends AbstractMessage {
    protected BlockPos pos;
    protected Direction side;
    protected String value;

    ///////////////////////////////////////////////////////////////////

    protected BusInterfaceNameMessage(final BusCableTileEntity tileEntity, final Direction side, final String value) {
        this.pos = tileEntity.getBlockPos();
        this.side = side;
        this.value = value;
    }

    protected BusInterfaceNameMessage(final PacketBuffer buffer) {
        super(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void fromBytes(final PacketBuffer buffer) {
        pos = buffer.readBlockPos();
        side = buffer.readEnum(Direction.class);
        value = buffer.readUtf(32);
    }

    @Override
    public void toBytes(final PacketBuffer buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeEnum(side);
        buffer.writeUtf(value, 32);
    }

    ///////////////////////////////////////////////////////////////////

    public static final class ToClient extends BusInterfaceNameMessage {
        public ToClient(final BusCableTileEntity tileEntity, final Direction side, final String value) {
            super(tileEntity, side, value);
        }

        public ToClient(final PacketBuffer buffer) {
            super(buffer);
        }

        @Override
        protected void handleMessage(final NetworkEvent.Context context) {
            MessageUtils.withClientTileEntityAt(pos, BusCableTileEntity.class,
                    (tileEntity) -> tileEntity.setInterfaceName(side, value));
        }
    }

    public static final class ToServer extends BusInterfaceNameMessage {
        public ToServer(final BusCableTileEntity tileEntity, final Direction side, final String value) {
            super(tileEntity, side, value);
        }

        public ToServer(final PacketBuffer buffer) {
            super(buffer);
        }

        @Override
        protected void handleMessage(final NetworkEvent.Context context) {
            MessageUtils.withNearbyServerTileEntityAt(context, pos, BusCableTileEntity.class,
                    (tileEntity) -> tileEntity.setInterfaceName(side, value));
        }
    }
}
