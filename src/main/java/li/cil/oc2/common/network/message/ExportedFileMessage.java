package li.cil.oc2.common.network.message;

import li.cil.oc2.client.gui.FileChooserScreen;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.util.function.Supplier;

public final class ExportedFileMessage {
    private static final Logger LOGGER = LogManager.getLogger();

    ///////////////////////////////////////////////////////////////////

    private String name;
    private byte[] data;

    ///////////////////////////////////////////////////////////////////

    public ExportedFileMessage(final String name, final byte[] data) {
        this.name = name;
        this.data = data;
    }

    public ExportedFileMessage(final PacketBuffer buffer) {
        fromBytes(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    public static boolean handleMessage(final ExportedFileMessage message, final Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> FileChooserScreen.openFileChooserForSave(message.name, path -> {
            try {
                Files.write(path, message.data);
            } catch (final IOException e) {
                LOGGER.error(e);
            }
        }));

        return true;
    }

    public static void toBytes(final ExportedFileMessage message, final PacketBuffer buffer) {
        buffer.writeString(message.name);
        buffer.writeByteArray(message.data);
    }

    public void fromBytes(final PacketBuffer buffer) {
        name = buffer.readString();
        data = buffer.readByteArray();
    }
}
