package li.cil.oc2.common.bus.device.item;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.DocumentedDevice;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.object.Parameter;
import li.cil.oc2.api.bus.device.rpc.RPCDevice;
import li.cil.oc2.api.bus.device.rpc.RPCMethod;
import li.cil.oc2.api.capabilities.TerminalUserProvider;
import li.cil.oc2.common.bus.device.util.IdentityProxy;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.ExportedFileMessage;
import li.cil.oc2.common.network.message.RequestImportedFileMessage;
import li.cil.oc2.common.network.message.ServerCanceledImportFileMessage;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StringUtils;
import net.minecraftforge.fml.network.PacketDistributor;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

public final class CloudInterfaceCardItemDevice extends IdentityProxy<ItemStack> implements RPCDevice, DocumentedDevice, ItemDevice {
    public static final int MAX_TRANSFERRED_FILE_SIZE = 512 * 1024;

    private static final String BEGIN_EXPORT_FILE = "beginExportFile";
    private static final String WRITE_EXPORT_FILE = "writeExportFile";
    private static final String FINISH_EXPORT_FILE = "finishExportFile";
    private static final String BEGIN_IMPORT_FILE = "beginImportFile";
    private static final String READ_IMPORT_FILE = "readImportFile";
    private static final String RESET = "reset";
    private static final String NAME = "name";
    private static final String DATA = "data";

    ///////////////////////////////////////////////////////////////////

    private enum State {
        IDLE,
        EXPORTING,
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
        public final ByteArrayInputStream data;

        private ImportedFile(final byte[] data) {
            this.data = new ByteArrayInputStream(data);
        }
    }

    private static final class ImportFileRequest {
        public final Set<ServerPlayerEntity> PendingPlayers = Collections.newSetFromMap(new WeakHashMap<>());
        public final WeakReference<CloudInterfaceCardItemDevice> Device;

        private ImportFileRequest(final CloudInterfaceCardItemDevice device) {
            Device = new WeakReference<>(device);
        }
    }

    ///////////////////////////////////////////////////////////////////

    private static final Int2ObjectArrayMap<ImportFileRequest> importingDevices = new Int2ObjectArrayMap<>();
    private static int nextImportId = 1;

    private final TerminalUserProvider userProvider;
    private final ObjectDevice device;
    private State state;
    private ExportedFile exportedFile;
    private int importingId;
    private ImportedFile importedFile;

    ///////////////////////////////////////////////////////////////////

    public CloudInterfaceCardItemDevice(final ItemStack identity, final TerminalUserProvider userProvider) {
        super(identity);
        this.userProvider = userProvider;
        this.device = new ObjectDevice(this, "cloud");
    }

    ///////////////////////////////////////////////////////////////////

    public static void setImportedFile(final int id, final byte[] data) {
        final ImportFileRequest request = importingDevices.remove(id);
        if (request != null) {
            final CloudInterfaceCardItemDevice device = request.Device.get();
            if (device != null) {
                device.importedFile = new ImportedFile(data);
                final ServerCanceledImportFileMessage message = new ServerCanceledImportFileMessage(id);
                for (final ServerPlayerEntity player : request.PendingPlayers) {
                    Network.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
                }
            }
        }
    }

    public static void cancelImport(final ServerPlayerEntity player, final int id) {
        final ImportFileRequest request = importingDevices.get(id);
        if (request != null) {
            request.PendingPlayers.remove(player);
            if (request.PendingPlayers.isEmpty()) {
                importingDevices.remove(id);
                final CloudInterfaceCardItemDevice device = request.Device.get();
                if (device != null) {
                    device.state = State.IMPORT_CANCELED;
                }
            }
        }
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public List<String> getTypeNames() {
        return device.getTypeNames();
    }

    @Override
    public List<RPCMethod> getMethods() {
        return device.getMethods();
    }

    @Callback(name = BEGIN_EXPORT_FILE, synchronize = false)
    public void beginExportFile(@Parameter(NAME) final String name) {
        if (state != State.IDLE) {
            throw new IllegalStateException("invalid state");
        }

        if (StringUtils.isNullOrEmpty(name)) {
            throw new IllegalArgumentException("name must not be empty");
        }

        state = State.EXPORTING;
        exportedFile = new ExportedFile(name);
    }

    @Callback(name = WRITE_EXPORT_FILE, synchronize = false)
    public void writeExportFile(@Parameter(DATA) final byte[] data) throws IOException {
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

    @Callback(name = FINISH_EXPORT_FILE)
    public void finishExportFile() {
        if (state != State.EXPORTING) {
            throw new IllegalStateException("invalid state");
        }

        try {
            for (final PlayerEntity player : userProvider.getTerminalUsers()) {
                if (player instanceof ServerPlayerEntity) {
                    final ExportedFileMessage message = new ExportedFileMessage(exportedFile.name, exportedFile.data.toByteArray());
                    Network.INSTANCE.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) player), message);
                }
            }
        } finally {
            reset();
        }
    }

    @Callback(name = BEGIN_IMPORT_FILE)
    public void beginImportFile() {
        if (state != State.IDLE) {
            throw new IllegalStateException("invalid state");
        }

        state = State.IMPORTING;

        importingId = nextImportId++;
        importingDevices.put(importingId, new ImportFileRequest(this));

        boolean hasAnyUsers = false;
        for (final PlayerEntity player : userProvider.getTerminalUsers()) {
            if (player instanceof ServerPlayerEntity) {
                final RequestImportedFileMessage message = new RequestImportedFileMessage(importingId);
                Network.INSTANCE.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) player), message);
                hasAnyUsers = true;
            }
        }

        if (!hasAnyUsers) {
            importingDevices.remove(importingId);
            importedFile = new ImportedFile(new byte[0]);
        }
    }

    @Nullable
    @Callback(name = READ_IMPORT_FILE)
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

        final byte[] buffer = new byte[512];
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

    @Callback(name = RESET)
    public void reset() {
        state = State.IDLE;
        exportedFile = null;
        importedFile = null;
        importingDevices.remove(importingId);
    }

    @Override
    public void getDeviceDocumentation(final DeviceVisitor visitor) {
        visitor.visitCallback(BEGIN_EXPORT_FILE)
                .description("Begins exporting a file to external data storage. Requires calls to " +
                             WRITE_EXPORT_FILE + "() to provide data of the exported file and a call " +
                             "to " + FINISH_EXPORT_FILE + "() to complete the export.\n" +
                             "This method may error if the device is currently exporting or importing.")
                .parameterDescription(NAME, "the name of the file being exported.");
        visitor.visitCallback(WRITE_EXPORT_FILE)
                .description("Appends more data to the currently being exported file.\n" +
                             "This method may error if the device is not currently exporting or the " +
                             "export was interrupted.\n")
                .parameterDescription(DATA, "the contents of the file being exported.");
        visitor.visitCallback(FINISH_EXPORT_FILE)
                .description("Finishes an export. This will prompt present users to select an external " +
                             "file location for the file being exported. If multiple users are present, " +
                             "the file is provided to all users.\n" +
                             "This method may error if the device is not currently exporting or the " +
                             "export was interrupted.");
        visitor.visitCallback(BEGIN_IMPORT_FILE)
                .description("Begins a file import operation. This will prompt present users to select " +
                             "an externally stored file for import. If multiple users are present, the " +
                             "first user to select a file will have their file uploaded. Use the " +
                             READ_IMPORT_FILE + "() method to read the contents of the file being imported.\n" +
                             "This method may error if the device is currently exporting or importing.");
        visitor.visitCallback(READ_IMPORT_FILE)
                .description("Tries to read some data from a file being imported. Returns zero length " +
                             "data if no data is available yet. Returns null when no more data is " +
                             "available.\n" +
                             "This method may error if the device is not currently importing or the " +
                             "import was interrupted.")
                .returnValueDescription("data from the file being imported.");
        visitor.visitCallback(RESET)
                .description("Resets the device and cancels any currently running export or import operation.");
    }
}
