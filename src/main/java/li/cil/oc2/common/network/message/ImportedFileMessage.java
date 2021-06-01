package li.cil.oc2.common.network.message;

import li.cil.oc2.common.bus.device.item.FileImportExportCardItemDevice;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public final class ImportedFileMessage {
    private int id;
    private byte[] data;

    ///////////////////////////////////////////////////////////////////

    public ImportedFileMessage(final int id, final byte[] data) {
        this.id = id;
        this.data = data;
    }

    public ImportedFileMessage(final PacketBuffer buffer) {
        fromBytes(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    public static boolean handleMessage(final ImportedFileMessage message, final Supplier<NetworkEvent.Context> context) {
        FileImportExportCardItemDevice.setImportedFile(message.id, message.data);
        return true;
    }

    public static void toBytes(final ImportedFileMessage message, final PacketBuffer buffer) {
        buffer.writeVarInt(message.id);
        buffer.writeByteArray(message.data);
    }

    public void fromBytes(final PacketBuffer buffer) {
        id = buffer.readVarInt();
        data = buffer.readByteArray();
    }
}
