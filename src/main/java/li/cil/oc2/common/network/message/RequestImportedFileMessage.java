package li.cil.oc2.common.network.message;

import li.cil.oc2.client.gui.FileChooserScreen;
import li.cil.oc2.common.bus.device.item.FileImportExportCardItemDevice;
import li.cil.oc2.common.network.Network;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static li.cil.oc2.common.util.TranslationUtils.text;

public final class RequestImportedFileMessage extends AbstractMessage {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final TranslatableComponent FILE_TOO_LARGE_TEXT = text("message.{mod}.import_file.file_too_large");

    ///////////////////////////////////////////////////////////////////

    private int id;

    ///////////////////////////////////////////////////////////////////

    public RequestImportedFileMessage(final int id) {
        this.id = id;
    }

    public RequestImportedFileMessage(final FriendlyByteBuf buffer) {
        super(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void fromBytes(final FriendlyByteBuf buffer) {
        id = buffer.readVarInt();
    }

    @Override
    public void toBytes(final FriendlyByteBuf buffer) {
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
                        Minecraft.getInstance().gui.getChat().addMessage(FILE_TOO_LARGE_TEXT
                            .withStyle(s -> s.withColor(TextColor.fromRgb(0xFFA0A0))));
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
