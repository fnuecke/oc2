package li.cil.oc2.common.bus.device.item;

import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.vm.VMDevice;
import li.cil.oc2.api.bus.device.vm.VMDeviceLoadResult;
import li.cil.oc2.api.bus.device.vm.context.VMContext;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.bus.device.util.IdentityProxy;
import li.cil.oc2.common.bus.device.util.OptionalAddress;
import li.cil.oc2.common.bus.device.util.OptionalInterrupt;
import li.cil.oc2.common.serialization.BlobStorage;
import li.cil.oc2.common.serialization.NBTSerialization;
import li.cil.oc2.common.util.Event;
import li.cil.oc2.common.util.NBTTagIds;
import li.cil.sedna.api.device.BlockDevice;
import li.cil.sedna.device.virtio.VirtIOBlockDevice;
import net.minecraft.nbt.CompoundTag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public abstract class AbstractBlockDeviceVMDevice<TBlock extends BlockDevice, TIdentity> extends IdentityProxy<TIdentity> implements VMDevice, ItemDevice {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final String DEVICE_TAG_NAME = "device";
    private static final String ADDRESS_TAG_NAME = "address";
    private static final String INTERRUPT_TAG_NAME = "interrupt";
    private static final String BLOB_HANDLE_TAG_NAME = "blob";

    ///////////////////////////////////////////////////////////////

    protected VirtIOBlockDevice device;

    ///////////////////////////////////////////////////////////////

    // Online persisted data.
    private final OptionalAddress address = new OptionalAddress();
    private final OptionalInterrupt interrupt = new OptionalInterrupt();
    private CompoundTag deviceTag;

    // Offline persisted data.
    @Nullable protected UUID blobHandle;

    ///////////////////////////////////////////////////////////////

    protected AbstractBlockDeviceVMDevice(final TIdentity identity) {
        super(identity);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public VMDeviceLoadResult mount(final VMContext context) {
        if (!allocateDevice(context)) {
            return VMDeviceLoadResult.fail();
        }

        if (!address.claim(context, device)) {
            return VMDeviceLoadResult.fail();
        }

        if (interrupt.claim(context)) {
            device.getInterrupt().set(interrupt.getAsInt(), context.getInterruptController());
        } else {
            return VMDeviceLoadResult.fail();
        }

        context.getEventBus().register(this);

        if (deviceTag != null) {
            NBTSerialization.deserialize(deviceTag, device);
        }

        return VMDeviceLoadResult.success();
    }

    @Override
    public void unmount() {
        suspend();
        deviceTag = null;
        address.clear();
        interrupt.clear();
    }

    @Override
    public void suspend() {
        closeBlockDevice();

        if (blobHandle != null) {
            BlobStorage.close(blobHandle);
        }
    }

    @Override
    public void exportToItemStack(final CompoundTag nbt) {
        if (blobHandle != null) {
            nbt.putUUID(BLOB_HANDLE_TAG_NAME, blobHandle);
        }
    }

    @Override
    public void importFromItemStack(final CompoundTag nbt) {
        if (nbt.hasUUID(BLOB_HANDLE_TAG_NAME)) {
            blobHandle = nbt.getUUID(BLOB_HANDLE_TAG_NAME);
        }
    }

    @Override
    public CompoundTag serializeNBT() {
        final CompoundTag tag = new CompoundTag();

        if (blobHandle != null) {
            tag.putUUID(BLOB_HANDLE_TAG_NAME, blobHandle);
        }

        if (device != null) {
            deviceTag = NBTSerialization.serialize(device);
        }
        if (deviceTag != null) {
            tag.put(DEVICE_TAG_NAME, deviceTag);
        }
        if (address.isPresent()) {
            tag.putLong(ADDRESS_TAG_NAME, address.getAsLong());
        }
        if (interrupt.isPresent()) {
            tag.putInt(INTERRUPT_TAG_NAME, interrupt.getAsInt());
        }

        return tag;
    }

    @Override
    public void deserializeNBT(final CompoundTag tag) {
        if (tag.hasUUID(BLOB_HANDLE_TAG_NAME)) {
            blobHandle = tag.getUUID(BLOB_HANDLE_TAG_NAME);
        }

        if (tag.contains(DEVICE_TAG_NAME, NBTTagIds.TAG_COMPOUND)) {
            deviceTag = tag.getCompound(DEVICE_TAG_NAME);
        }
        if (tag.contains(ADDRESS_TAG_NAME, NBTTagIds.TAG_LONG)) {
            address.set(tag.getLong(ADDRESS_TAG_NAME));
        }
        if (tag.contains(INTERRUPT_TAG_NAME, NBTTagIds.TAG_INT)) {
            interrupt.set(tag.getInt(INTERRUPT_TAG_NAME));
        }
    }

    ///////////////////////////////////////////////////////////////

    protected abstract TBlock createBlockDevice() throws IOException;

    protected void closeBlockDevice() {
        if (device == null) {
            return;
        }

        try {
            device.close();
        } catch (final IOException e) {
            LOGGER.error(e);
        }

        device = null;
    }

    protected void handleDataAccess() {
    }

    ///////////////////////////////////////////////////////////////

    private boolean allocateDevice(final VMContext context) {
        if (!context.getMemoryAllocator().claimMemory(Constants.PAGE_SIZE)) {
            return false;
        }

        try {
            final ListenableBlockDevice listenableData = new ListenableBlockDevice(createBlockDevice());
            listenableData.onAccess.add(this::handleDataAccess);
            device = new VirtIOBlockDevice(context.getMemoryMap(), listenableData);
        } catch (final IOException e) {
            LOGGER.error(e);
            return false;
        }

        return true;
    }

    ///////////////////////////////////////////////////////////////

    private static final class ListenableBlockDevice implements BlockDevice {
        private final BlockDevice inner;

        public final Event onAccess = new Event();

        private ListenableBlockDevice(final BlockDevice inner) {
            this.inner = inner;
        }

        @Override
        public boolean isReadonly() {
            return inner.isReadonly();
        }

        @Override
        public long getCapacity() {
            return inner.getCapacity();
        }

        @Override
        public InputStream getInputStream(final long offset) {
            final ListenableInputStream stream = new ListenableInputStream(inner.getInputStream(offset));
            stream.onAccess.add(onAccess);
            return stream;
        }

        @Override
        public OutputStream getOutputStream(final long offset) {
            final ListenableOutputStream stream = new ListenableOutputStream(inner.getOutputStream(offset));
            stream.onAccess.add(onAccess);
            return stream;
        }

        @Override
        public void flush() {
            inner.flush();
        }

        @Override
        public void close() throws IOException {
            inner.close();
        }
    }

    private static final class ListenableInputStream extends InputStream {
        private final InputStream inner;

        public final Event onAccess = new Event();

        private ListenableInputStream(final InputStream inner) {
            this.inner = inner;
        }

        @Override
        public int read() throws IOException {
            onAccess();
            return inner.read();
        }

        @Override
        public int read(final byte[] b) throws IOException {
            onAccess();
            return inner.read(b);
        }

        @Override
        public int read(final byte[] b, final int off, final int len) throws IOException {
            onAccess();
            return inner.read(b, off, len);
        }

        @Override
        public long skip(final long n) throws IOException {
            onAccess();
            return inner.skip(n);
        }

        @Override
        public int available() throws IOException {
            return inner.available();
        }

        @Override
        public void close() throws IOException {
            inner.close();
        }

        @Override
        public synchronized void mark(final int limit) {
            inner.mark(limit);
        }

        @Override
        public synchronized void reset() throws IOException {
            onAccess();
            inner.reset();
        }

        @Override
        public boolean markSupported() {
            return inner.markSupported();
        }

        private void onAccess() {
            onAccess.run();
        }
    }

    private static final class ListenableOutputStream extends OutputStream {
        private final OutputStream inner;

        public final Event onAccess = new Event();

        private ListenableOutputStream(final OutputStream inner) {
            this.inner = inner;
        }

        @Override
        public void write(final int b) throws IOException {
            onAccess();
            inner.write(b);
        }

        @Override
        public void write(final byte[] b) throws IOException {
            onAccess();
            inner.write(b);
        }

        @Override
        public void write(final byte[] b, final int off, final int len) throws IOException {
            onAccess();
            inner.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            inner.flush();
        }

        @Override
        public void close() throws IOException {
            inner.close();
        }

        private void onAccess() {
            onAccess.run();
        }
    }
}
