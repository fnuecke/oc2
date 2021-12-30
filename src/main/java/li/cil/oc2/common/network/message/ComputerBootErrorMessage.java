package li.cil.oc2.common.network.message;

import li.cil.oc2.common.network.MessageUtils;
import li.cil.oc2.common.tileentity.ComputerTileEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;

public final class ComputerBootErrorMessage extends AbstractMessage {
    private BlockPos pos;
    private Component value;

    ///////////////////////////////////////////////////////////////////

    public ComputerBootErrorMessage(final ComputerTileEntity tileEntity) {
        this.pos = tileEntity.getBlockPos();
        this.value = tileEntity.getVirtualMachine().getBootError();
    }

    public ComputerBootErrorMessage(final FriendlyByteBuf buffer) {
        super(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void fromBytes(final FriendlyByteBuf buffer) {
        pos = buffer.readBlockPos();
        value = buffer.readComponent();
    }

    @Override
    public void toBytes(final FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeComponent(value);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void handleMessage(final NetworkEvent.Context context) {
        MessageUtils.withClientTileEntityAt(pos, ComputerTileEntity.class,
                (tileEntity) -> tileEntity.getVirtualMachine().setBootErrorClient(value));
    }
}
