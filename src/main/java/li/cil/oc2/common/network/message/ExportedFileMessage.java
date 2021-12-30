package li.cil.oc2.common.network.message;

import li.cil.oc2.client.gui.FileChooserScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;

public final class ExportedFileMessage extends AbstractMessage {
    private static final Logger LOGGER = LogManager.getLogger();

    ///////////////////////////////////////////////////////////////////

    private String name;
    private byte[] data;

    ///////////////////////////////////////////////////////////////////

    public ExportedFileMessage(final String name, final byte[] data) {
        this.name = name;
        this.data = data;
    }

    public ExportedFileMessage(final FriendlyByteBuf buffer) {
        super(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void fromBytes(final FriendlyByteBuf buffer) {
        name = buffer.readUtf();
        data = buffer.readByteArray();
    }

    @Override
    public void toBytes(final FriendlyByteBuf buffer) {
        buffer.writeUtf(name);
        buffer.writeByteArray(data);
    }

    ///////////////////////////////////////////////////////////////////

    protected void handleMessage(final NetworkEvent.Context context) {
        FileChooserScreen.openFileChooserForSave(name, path -> {
            try {
                Files.write(path, data);
            } catch (final IOException e) {
                LOGGER.error(e);
            }
        });
    }
}
