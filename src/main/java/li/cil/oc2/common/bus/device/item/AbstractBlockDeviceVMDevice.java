package li.cil.oc2.common.bus.device.item;

import com.google.common.eventbus.Subscribe;
import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.vm.VMDevice;
import li.cil.oc2.api.bus.device.vm.VMDeviceLoadResult;
import li.cil.oc2.api.bus.device.vm.context.VMContext;
import li.cil.oc2.api.bus.device.vm.event.VMResumingRunningEvent;
import li.cil.oc2.common.bus.device.util.IdentityProxy;
import li.cil.oc2.common.bus.device.util.OptionalAddress;
import li.cil.oc2.common.bus.device.util.OptionalInterrupt;
import li.cil.oc2.common.serialization.BlobStorage;
import li.cil.oc2.common.serialization.NBTSerialization;
import li.cil.oc2.common.util.Event;
import li.cil.oc2.common.util.NBTTagIds;
import li.cil.sedna.api.device.BlockDevice;
import li.cil.sedna.device.virtio.VirtIOBlockDevice;
import net.minecraft.nbt.CompoundNBT;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("UnstableApiUsage")
public abstract class AbstractBlockDeviceVMDevice<TBlock extends BlockDevice, TIdentity> extends IdentityProxy<TIdentity> implements VMDevice, ItemDevice {
    private static final String DEVICE_TAG_NAME = "device";
    private static final String ADDRESS_TAG_NAME = "address";
    private static final String INTERRUPT_TAG_NAME = "interrupt";
    private static final String BLOB_HANDLE_TAG_NAME = "blob";

    ///////////////////////////////////////////////////////////////

    private BlobStorage.JobHandle jobHandle;
    protected TBlock data;
    protected VirtIOBlockDevice device;

    ///////////////////////////////////////////////////////////////

    // Online persisted data.
    private final OptionalAddress address = new OptionalAddress();
    private final OptionalInterrupt interrupt = new OptionalInterrupt();
    private CompoundNBT deviceTag;

    // Offline persisted data.
    protected UUID blobHandle;

    ///////////////////////////////////////////////////////////////

    protected AbstractBlockDeviceVMDevice(final TIdentity identity) {
        super(identity);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public VMDeviceLoadResult mount(final VMContext context) {
        data = createBlockDevice();

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

        deserializeData();

        if (deviceTag != null) {
            NBTSerialization.deserialize(deviceTag, device);
        }

        return VMDeviceLoadResult.success();
    }

    @Override
    public void unmount() {
        // Since we cannot serialize the data in a regular serialize call due to the
        // actual data being unloaded at that point, but want to permanently persist
        // it (it's the contents of the block device) we need to serialize it in the
        // unload, too. Don't need to wait for the job, though.
        serializeData();

        suspend();
        deviceTag = null;
        address.clear();
        interrupt.clear();
    }

    @Override
    public void suspend() {
        data = null;
        jobHandle = null;

        device = null;
    }

    @Subscribe
    public void handleResumingRunningEvent(final VMResumingRunningEvent event) {
        awaitStorageOperation();
    }

    @Override
    public void exportToItemStack(final CompoundNBT nbt) {
        if (blobHandle == null && data != null) {
            getSerializationStream(data).ifPresent(stream -> blobHandle = BlobStorage.validateHandle(blobHandle));
        }
        if (blobHandle != null) {
            nbt.putUUID(BLOB_HANDLE_TAG_NAME, blobHandle);
        }
    }

    @Override
    public void importFromItemStack(final CompoundNBT nbt) {
        if (nbt.hasUUID(BLOB_HANDLE_TAG_NAME)) {
            blobHandle = nbt.getUUID(BLOB_HANDLE_TAG_NAME);
        }
    }

    @Override
    public CompoundNBT serializeNBT() {
        final CompoundNBT tag = new CompoundNBT();

        serializeData();
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

        if (blobHandle != null) {
            tag.putUUID(BLOB_HANDLE_TAG_NAME, blobHandle);
        }

        return tag;
    }

    @Override
    public void deserializeNBT(final CompoundNBT tag) {
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

    public void serializeData() {
        if (data != null) {
            final Optional<InputStream> optional = getSerializationStream(data);
            optional.ifPresent(stream -> {
                blobHandle = BlobStorage.validateHandle(blobHandle);
                jobHandle = BlobStorage.submitSave(blobHandle, stream);
            });
            if (!optional.isPresent()) {
                BlobStorage.freeHandle(blobHandle);
                blobHandle = null;
            }
        }
    }

    public void deserializeData() {
        if (data != null && blobHandle != null) {
            try {
                jobHandle = BlobStorage.submitLoad(blobHandle, getDeserializationStream(data));
            } catch (final UnsupportedOperationException ignored) {
                // If logic producing the block data implementation changed between saves we
                // can potentially end up in a state where we now have read only block data
                // with some previously persisted data. A call to getDeserializationStream
                // will, indirectly, lead to this exception. So we just ignore it.
            }
        }
    }

    ///////////////////////////////////////////////////////////////

    protected abstract int getSize();

    protected abstract TBlock createBlockDevice();

    protected abstract Optional<InputStream> getSerializationStream(TBlock device);

    protected abstract OutputStream getDeserializationStream(TBlock device);

    protected void handleDataAccess() {
    }

    ///////////////////////////////////////////////////////////////

    private boolean allocateDevice(final VMContext context) {
        if (!context.getMemoryAllocator().claimMemory(getSize())) {
            return false;
        }

        final ListenableBlockDevice listenableData = new ListenableBlockDevice(data);
        listenableData.onAccess.add(this::handleDataAccess);
        device = new VirtIOBlockDevice(context.getMemoryMap(), listenableData);

        return true;
    }

    private void awaitStorageOperation() {
        if (jobHandle != null) {
            jobHandle.await();
            jobHandle = null;
        }
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
