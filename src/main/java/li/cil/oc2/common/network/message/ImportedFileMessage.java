package li.cil.oc2.common.network.message;

import li.cil.oc2.common.bus.device.item.FileImportExportCardItemDevice;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class ImportedFileMessage extends AbstractMessage {
    private static final int MAX_NAME_LENGTH = 256;

    ///////////////////////////////////////////////////////////////////

    private int id;
    private String name;
    private byte[] data;

    ///////////////////////////////////////////////////////////////////

    public ImportedFileMessage(final int id, final String name, final byte[] data) {
        this.id = id;
        this.name = name;
        this.data = data;
    }

    public ImportedFileMessage(final FriendlyByteBuf buffer) {
        super(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void fromBytes(final FriendlyByteBuf buffer) {
        id = buffer.readVarInt();
        name = buffer.readUtf(MAX_NAME_LENGTH);
        data = buffer.readByteArray();
    }

    @Override
    public void toBytes(final FriendlyByteBuf buffer) {
        buffer.writeVarInt(id);
        buffer.writeUtf(name, MAX_NAME_LENGTH);
        buffer.writeByteArray(data);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void handleMessage(final Supplier<NetworkEvent.Context> context) {
        FileImportExportCardItemDevice.setImportedFile(id, name, data);
    }
}
