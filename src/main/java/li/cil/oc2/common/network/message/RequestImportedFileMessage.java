package li.cil.oc2.common.network.message;

import li.cil.oc2.client.gui.FileChooserScreen;
import li.cil.oc2.common.bus.device.item.FileImportExportCardItemDevice;
import li.cil.oc2.common.network.Network;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.Color;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.fml.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static li.cil.oc2.common.util.TranslationUtils.text;

public final class RequestImportedFileMessage extends AbstractMessage {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final TranslationTextComponent FILE_TOO_LARGE_TEXT = text("message.{mod}.import_file.file_too_large");

    ///////////////////////////////////////////////////////////////////

    private int id;

    ///////////////////////////////////////////////////////////////////

    public RequestImportedFileMessage(final int id) {
        this.id = id;
    }

    public RequestImportedFileMessage(final PacketBuffer buffer) {
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
    protected void handleMessage(final NetworkEvent.Context context) {
        FileChooserScreen.openFileChooserForLoad(new FileChooserScreen.FileChooserCallback() {
            @Override
            public void onFileSelected(final Path path) {
                try {
                    final String fileName = path.getFileName().toString();
                    final byte[] data = Files.readAllBytes(path);
                    if (data.length > FileImportExportCardItemDevice.MAX_TRANSFERRED_FILE_SIZE) {
                        Network.INSTANCE.sendToServer(new ClientCanceledImportFileMessage(id));
                        Minecraft.getInstance().player.displayClientMessage(FILE_TOO_LARGE_TEXT
                                .withStyle(s -> s.withColor(Color.fromRgb(0xFFA0A0))), false);
                    } else {
                        Network.INSTANCE.sendToServer(new ImportedFileMessage(id, fileName, data));
                    }
                } catch (final IOException e) {
                    LOGGER.error(e);
                }
            }

            @Override
            public void onCanceled() {
                Network.INSTANCE.sendToServer(new ClientCanceledImportFileMessage(id));
            }
        });
    }
}
