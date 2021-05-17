package li.cil.oc2.common.network.message;

import li.cil.oc2.common.bus.device.item.FileImportExportCardItemDevice;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public final class ServerCanceledImportFileMessage {
    private int id;

    ///////////////////////////////////////////////////////////////////

    public ServerCanceledImportFileMessage(final int id) {
        this.id = id;
    }

    public ServerCanceledImportFileMessage(final PacketBuffer buffer) {
        fromBytes(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    public static boolean handleMessage(final ServerCanceledImportFileMessage message, final Supplier<NetworkEvent.Context> context) {
        FileImportExportCardItemDevice.cancelImport(context.get().getSender(), message.id);
        return true;
    }

    public static void toBytes(final ServerCanceledImportFileMessage message, final PacketBuffer buffer) {
        buffer.writeVarInt(message.id);
    }

    public void fromBytes(final PacketBuffer buffer) {
        id = buffer.readVarInt();
    }
}
