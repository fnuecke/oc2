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
import java.util.function.Supplier;

public final class RequestImportedFileMessage {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final TranslationTextComponent FILE_TOO_LARGE_TEXT = new TranslationTextComponent("message.oc2.import_file.file_too_large");

    ///////////////////////////////////////////////////////////////////

    private int id;

    ///////////////////////////////////////////////////////////////////

    public RequestImportedFileMessage(final int id) {
        this.id = id;
    }

    public RequestImportedFileMessage(final PacketBuffer buffer) {
        fromBytes(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    public static boolean handleMessage(final RequestImportedFileMessage message, final Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> FileChooserScreen.openFileChooserForLoad(new FileChooserScreen.FileChooserCallback() {
            @Override
            public void onFileSelected(final Path path) {
                try {
                    final byte[] data = Files.readAllBytes(path);
                    if (data.length > FileImportExportCardItemDevice.MAX_TRANSFERRED_FILE_SIZE) {
                        Network.INSTANCE.sendToServer(new ClientCanceledImportFileMessage(message.id));
                        Minecraft.getInstance().player.sendStatusMessage(FILE_TOO_LARGE_TEXT
                                .modifyStyle(s -> s.setColor(Color.fromInt(0xFFA0A0))), false);
                    } else {
                        Network.INSTANCE.sendToServer(new ImportedFileMessage(message.id, data));
                    }
                } catch (final IOException e) {
                    LOGGER.error(e);
                }
            }

            @Override
            public void onCanceled() {
                Network.INSTANCE.sendToServer(new ClientCanceledImportFileMessage(message.id));
            }
        }));

        return true;
    }

    public static void toBytes(final RequestImportedFileMessage message, final PacketBuffer buffer) {
        buffer.writeVarInt(message.id);
    }

    public void fromBytes(final PacketBuffer buffer) {
        id = buffer.readVarInt();
    }
}
