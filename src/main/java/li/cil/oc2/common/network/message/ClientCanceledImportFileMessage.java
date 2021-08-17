package li.cil.oc2.common.network.message;

import li.cil.oc2.common.bus.device.item.FileImportExportCardItemDevice;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public final class ClientCanceledImportFileMessage extends AbstractMessage {
    private int id;

    ///////////////////////////////////////////////////////////////////

    public ClientCanceledImportFileMessage(final int id) {
        this.id = id;
    }

    public ClientCanceledImportFileMessage(final PacketBuffer buffer) {
        super(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void fromBytes(final PacketBuffer buffer) {
        id = buffer.readVarInt();
    }

    @Override
    public void toBytes(final PacketBuffer buffer) {
        buffer.writeVarInt(id);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void handleMessage(final Supplier<NetworkEvent.Context> context) {
        FileImportExportCardItemDevice.cancelImport(context.get().getSender(), id);
    }
}
