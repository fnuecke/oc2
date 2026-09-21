/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.rpc.item;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.Parameter;
import li.cil.oc2.api.bus.device.object.RPCDeviceDescription;
import li.cil.oc2.api.bus.device.rpc.RPCBusContext;
import li.cil.oc2.api.capabilities.TerminalUserProvider;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.ExportedFileMessage;
import li.cil.oc2.common.network.message.RequestImportedFileMessage;
import li.cil.oc2.common.network.message.ServerCanceledImportFileMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

@RPCDeviceDescription(typeNames = {"file_import_export"}, description = """
    Provided by the [file import/export card](../item/file_import_export_card.md).

    ### Exporting
    Call `beginExportFile()` with a name, append the contents with `writeExportFile()`, then call `finishExportFile()`. Every user at the terminal is offered to save the file. Files are limited to 512 KiB.

    ### Importing
    Call `requestImportFile()` to prompt every user at the terminal for a file. Poll `beginImportFile()` until it returns the file's name and size; the first file a user picks wins, and the prompts on other clients are canceled. Then call `readImportFile()` until it returns nothing. `reset()` cancels either operation.

    Methods fail with an error when called in the wrong order, or when the users canceled.""")
public final class FileImportExportCardItemDevice extends AbstractItemRPCDevice {
    public static final int MAX_TRANSFERRED_FILE_SIZE = 512 * Constants.KILOBYTE;
    private static final int IMPORT_CHUNK_SIZE = 4 * Constants.KILOBYTE;

    // --------------------------------------------------------------------- //

    private enum State {
        IDLE,
        EXPORTING,
        IMPORT_REQUESTED,
        IMPORTING,
        IMPORT_CANCELED,
    }

    private static final class ExportedFile {
        public final String name;
        public final ByteArrayOutputStream data = new ByteArrayOutputStream();

        private ExportedFile(final String name) {
            this.name = name;
        }
    }

    private static final class ImportedFile {
        public final String name;
        public final int size;
        public final ByteArrayInputStream data;

        private ImportedFile(final String name, final byte[] data) {
            this.name = name;
            this.size = data.length;
            this.data = new ByteArrayInputStream(data);
        }
    }

    private record ImportedFileInfo(String name, int size) {
    }

    private static final class ImportFileRequest {
        public final Set<ServerPlayer> pendingPlayers = Collections.newSetFromMap(new WeakHashMap<>());
        public final WeakReference<FileImportExportCardItemDevice> device;

        private ImportFileRequest(final FileImportExportCardItemDevice device, final ArrayList<ServerPlayer> players) {
            this.device = new WeakReference<>(device);
            this.pendingPlayers.addAll(players);
        }
    }

    // --------------------------------------------------------------------- //

    private static final Int2ObjectArrayMap<ImportFileRequest> importingDevices = new Int2ObjectArrayMap<>();
    private static int nextImportId = 1;

    private final TerminalUserProvider userProvider;

    // Written from the VM worker thread (the non-synchronized export callbacks) and from
    // the server thread (reset/unmount and network messages), so these need to be volatile
    // for proper propagation/visibility. For now we don't need a lock:
    //   - RPCDeviceBusAdapter runs at most one callback at a time, so reset() and
    //     writeExportFile() are exclusive.
    //   - unmount() only runs after the worker thread has been joined.
    //   - device rebuilds are guarded by pause().
    //   - network handlers are queued onto the server thread.
    private volatile State state;
    private volatile ExportedFile exportedFile;
    private volatile int importingId;
    private volatile ImportedFile importedFile;

    // --------------------------------------------------------------------- //

    public FileImportExportCardItemDevice(final ItemStack identity, final TerminalUserProvider userProvider) {
        super(identity);
        this.userProvider = userProvider;
    }

    // --------------------------------------------------------------------- //

    public static void setImportedFile(final ServerPlayer sender, final int id, final String name, final byte[] data) {
        synchronized (importingDevices) {
            final ImportFileRequest request = importingDevices.get(id);
            if (request != null && request.pendingPlayers.contains(sender)) {
                importingDevices.remove(id);
                final FileImportExportCardItemDevice device = request.device.get();
                if (device != null) {
                    device.importedFile = new ImportedFile(name, data);
                    final ServerCanceledImportFileMessage message = new ServerCanceledImportFileMessage(id);
                    for (final ServerPlayer serverPlayer : request.pendingPlayers) {
                        if (serverPlayer != sender) {
                            Network.sendToClient(message, serverPlayer);
                        }
                    }
                }
            }
        }
    }

    public static void cancelImport(final ServerPlayer player, final int id) {
        synchronized (importingDevices) {
            final ImportFileRequest request = importingDevices.get(id);
            if (request != null) {
                request.pendingPlayers.remove(player);
                if (request.pendingPlayers.isEmpty()) {
                    importingDevices.remove(id);
                    final FileImportExportCardItemDevice device = request.device.get();
                    if (device != null) {
                        device.state = State.IMPORT_CANCELED;
                    }
                }
            }
        }
    }

    // --------------------------------------------------------------------- //

    @Override
    public void unmount(final RPCBusContext context) {
        reset();
    }

    @Callback(synchronize = false,
        description = "Begins exporting a file. Provide its contents with writeExportFile() and complete the export with finishExportFile(). " +
            "Fails if the device is currently exporting or importing.")
    public void beginExportFile(@Parameter(value = "name", description = "the name of the file being exported.") final String name) {
        if (state != State.IDLE) {
            throw new IllegalStateException("invalid state");
        }

        if (StringUtil.isNullOrEmpty(name)) {
            throw new IllegalArgumentException("name must not be empty");
        }

        exportedFile = new ExportedFile(name);
        state = State.EXPORTING;
    }

    @Callback(synchronize = false,
        description = "Appends data to the file being exported. Fails if the device is not currently exporting, or the file grows past 512 KiB.")
    public void writeExportFile(@Parameter(value = "data", description = "the data to append to the file being exported.") @Nullable final byte[] data) throws IOException {
        if (state != State.EXPORTING) {
            throw new IllegalStateException("invalid state");
        }

        if (data == null) {
            throw new IllegalArgumentException("data is required");
        }

        exportedFile.data.write(data);

        if (exportedFile.data.size() > MAX_TRANSFERRED_FILE_SIZE) {
            reset();
            throw new IllegalArgumentException("exported file too large");
        }
    }

    @Callback(description = "Finishes the export and offers every user at the terminal to save the file. Fails if the device is not currently exporting.")
    public void finishExportFile() {
        if (state != State.EXPORTING) {
            throw new IllegalStateException("invalid state");
        }

        try {
            for (final Player player : userProvider.getTerminalUsers()) {
                if (player instanceof final ServerPlayer serverPlayer) {
                    final ExportedFileMessage message = new ExportedFileMessage(exportedFile.name, exportedFile.data.toByteArray());
                    Network.sendToClient(message, serverPlayer);
                }
            }
        } finally {
            reset();
        }
    }

    @Callback(description = "Begins an import by prompting every user at the terminal to pick a file. Fails if the device is currently exporting or importing.",
        returnValueDescription = "whether anyone was prompted; `false` when nobody is using the terminal.")
    public boolean requestImportFile() {
        if (state != State.IDLE) {
            throw new IllegalStateException("invalid state");
        }

        final ArrayList<ServerPlayer> players = new ArrayList<>();
        for (final Player player : userProvider.getTerminalUsers()) {
            if (player instanceof final ServerPlayer serverPlayer) {
                players.add(serverPlayer);
            }
        }

        if (players.isEmpty()) {
            return false;
        }

        importingId = nextImportId++;
        state = State.IMPORT_REQUESTED;
        synchronized (importingDevices) {
            importingDevices.put(importingId, new ImportFileRequest(this, players));
        }

        final RequestImportedFileMessage message = new RequestImportedFileMessage(importingId);
        for (final ServerPlayer serverPlayer : players) {
            Network.sendToClient(message, serverPlayer);
        }

        return true;
    }

    @Nullable
    @Callback(description = "Checks whether a requested file has arrived and, if so, starts reading it. Poll this after requestImportFile(). " +
            "Fails if no import was requested, or every user canceled.",
        returnValueDescription = "a table with the file's `name` and `size`, or nothing while no file was picked yet.")
    public ImportedFileInfo beginImportFile() {
        if (state == State.IMPORT_CANCELED) {
            reset();
            throw new IllegalStateException("import was canceled");
        }

        if (state != State.IMPORT_REQUESTED) {
            throw new IllegalStateException("invalid state");
        }

        if (importedFile == null) {
            return null;
        }

        state = State.IMPORTING;
        return new ImportedFileInfo(importedFile.name, importedFile.size);
    }

    @Nullable
    @Callback(description = "Reads the next chunk of the file being imported. Fails if beginImportFile() did not succeed yet.",
        returnValueDescription = "up to 4 KiB of data, or nothing once the whole file was read.")
    public byte[] readImportFile() throws IOException {
        if (state == State.IMPORT_CANCELED) {
            reset();
            throw new IllegalStateException("import was canceled");
        }

        if (state != State.IMPORTING) {
            throw new IllegalStateException("invalid state");
        }

        if (importedFile == null) {
            return new byte[0];
        }

        final byte[] buffer = new byte[IMPORT_CHUNK_SIZE];
        final int count = importedFile.data.read(buffer);
        if (count <= 0) {
            reset();
            return null;
        }
        if (count < buffer.length) {
            final byte[] data = new byte[count];
            System.arraycopy(buffer, 0, data, 0, count);
            return data;
        } else {
            return buffer;
        }
    }

    @Callback(description = "Cancels any export or import in progress and returns the device to its idle state.")
    public void reset() {
        state = State.IDLE;
        exportedFile = null;
        importedFile = null;
        synchronized (importingDevices) {
            importingDevices.remove(importingId);
        }
    }
}
