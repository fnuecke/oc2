package li.cil.oc2.common.bus.device.item;

import com.google.common.eventbus.Subscribe;
import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.vm.VMContext;
import li.cil.oc2.api.bus.device.vm.VMDevice;
import li.cil.oc2.api.bus.device.vm.VMDeviceLoadResult;
import li.cil.oc2.api.bus.device.vm.event.VMResumingRunningEvent;
import li.cil.oc2.common.bus.device.util.IdentityProxy;
import li.cil.oc2.common.bus.device.util.OptionalAddress;
import li.cil.oc2.common.bus.device.util.OptionalInterrupt;
import li.cil.oc2.common.serialization.BlobStorage;
import li.cil.oc2.common.serialization.NBTSerialization;
import li.cil.oc2.common.util.NBTTagIds;
import li.cil.sedna.api.device.BlockDevice;
import li.cil.sedna.device.virtio.VirtIOBlockDevice;
import net.minecraft.nbt.CompoundNBT;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("UnstableApiUsage")
public abstract class AbstractHardDriveVMDevice<TBlock extends BlockDevice, TIdentity> extends IdentityProxy<TIdentity> implements VMDevice, ItemDevice {
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
    private CompoundNBT deviceNbt;

    // Offline persisted data.
    protected UUID blobHandle;

    ///////////////////////////////////////////////////////////////

    protected AbstractHardDriveVMDevice(final TIdentity identity) {
        super(identity);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public VMDeviceLoadResult load(final VMContext context) {
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

        if (deviceNbt != null) {
            NBTSerialization.deserialize(deviceNbt, device);
        }

        return VMDeviceLoadResult.success();
    }

    @Override
    public void unload() {
        // Since we cannot serialize the data in a regular serialize call due to the
        // actual data being unloaded at that point, but want to permanently persist
        // it (it's the contents of the block device) we need to serialize it in the
        // unload, too. Don't need to wait for the job, though.
        serializeData();

        data = null;
        jobHandle = null;

        device = null;
        deviceNbt = null;
        address.clear();
        interrupt.clear();
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
            nbt.putUniqueId(BLOB_HANDLE_TAG_NAME, blobHandle);
        }
    }

    @Override
    public void importFromItemStack(final CompoundNBT nbt) {
        if (nbt.hasUniqueId(BLOB_HANDLE_TAG_NAME)) {
            blobHandle = nbt.getUniqueId(BLOB_HANDLE_TAG_NAME);
        }
    }

    @Override
    public CompoundNBT serializeNBT() {
        final CompoundNBT tag = new CompoundNBT();

        serializeData();
        if (device != null) {
            deviceNbt = NBTSerialization.serialize(device);
        }
        if (deviceNbt != null) {
            tag.put(DEVICE_TAG_NAME, deviceNbt);
        }
        if (address.isPresent()) {
            tag.putLong(ADDRESS_TAG_NAME, address.getAsLong());
        }
        if (interrupt.isPresent()) {
            tag.putInt(INTERRUPT_TAG_NAME, interrupt.getAsInt());
        }

        if (blobHandle != null) {
            tag.putUniqueId(BLOB_HANDLE_TAG_NAME, blobHandle);
        }

        return tag;
    }

    @Override
    public void deserializeNBT(final CompoundNBT tag) {
        if (tag.hasUniqueId(BLOB_HANDLE_TAG_NAME)) {
            blobHandle = tag.getUniqueId(BLOB_HANDLE_TAG_NAME);
        }

        if (tag.contains(DEVICE_TAG_NAME, NBTTagIds.TAG_COMPOUND)) {
            deviceNbt = tag.getCompound(DEVICE_TAG_NAME);
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

    ///////////////////////////////////////////////////////////////

    private boolean allocateDevice(final VMContext context) {
        if (!context.getMemoryAllocator().claimMemory(getSize())) {
            return false;
        }

        device = new VirtIOBlockDevice(context.getMemoryMap(), data);

        return true;
    }

    private void awaitStorageOperation() {
        if (jobHandle != null) {
            jobHandle.await();
            jobHandle = null;
        }
    }
}
