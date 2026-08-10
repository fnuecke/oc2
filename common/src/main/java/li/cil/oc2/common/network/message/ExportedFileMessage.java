/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network.message;

import li.cil.oc2.client.gui.FileChooserScreen;
import net.minecraft.network.RegistryFriendlyByteBuf;
import dev.architectury.networking.NetworkManager;
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

    public ExportedFileMessage(final RegistryFriendlyByteBuf buffer) {
        super(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void fromBytes(final RegistryFriendlyByteBuf buffer) {
        name = buffer.readUtf();
        data = buffer.readByteArray();
    }

    @Override
    public void toBytes(final RegistryFriendlyByteBuf buffer) {
        buffer.writeUtf(name);
        buffer.writeByteArray(data);
    }

    ///////////////////////////////////////////////////////////////////

    protected void handleMessage(final NetworkManager.PacketContext context) {
        FileChooserScreen.openFileChooserForSave(name, path -> {
            try {
                Files.write(path, data);
            } catch (final IOException e) {
                LOGGER.error(e);
            }
        });
    }
}
