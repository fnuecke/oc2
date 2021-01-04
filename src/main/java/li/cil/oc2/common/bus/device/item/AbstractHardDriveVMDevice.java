package li.cil.oc2.common.bus.device.item;

import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.vm.*;
import li.cil.oc2.common.bus.device.util.IdentityProxy;
import li.cil.oc2.common.bus.device.util.OptionalAddress;
import li.cil.oc2.common.bus.device.util.OptionalInterrupt;
import li.cil.oc2.common.serialization.BlobStorage;
import li.cil.oc2.common.serialization.NBTSerialization;
import li.cil.oc2.common.util.NBTTagIds;
import li.cil.sedna.api.device.BlockDevice;
import li.cil.sedna.device.virtio.VirtIOBlockDevice;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;
import java.util.UUID;

public abstract class AbstractHardDriveVMDevice<T extends BlockDevice> extends IdentityProxy<ItemStack> implements VMDevice, VMDeviceLifecycleListener, ItemDevice {
    private static final String DEVICE_NBT_TAG_NAME = "device";
    private static final String ADDRESS_NBT_TAG_NAME = "address";
    private static final String INTERRUPT_NBT_TAG_NAME = "interrupt";
    private static final String BLOB_HANDLE_NBT_TAG_NAME = "blob";

    ///////////////////////////////////////////////////////////////

    private BlobStorage.JobHandle jobHandle;
    private T data;
    private VirtIOBlockDevice device;

    ///////////////////////////////////////////////////////////////

    // Online persisted data.
    private final OptionalAddress address = new OptionalAddress();
    private final OptionalInterrupt interrupt = new OptionalInterrupt();
    private CompoundNBT deviceNbt;

    // Offline persisted data.
    private UUID blobHandle;

    ///////////////////////////////////////////////////////////////

    protected AbstractHardDriveVMDevice(final ItemStack stack) {
        super(stack);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public VMDeviceLoadResult load(final VMContext context) {
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

        loadPersistedState();

        return VMDeviceLoadResult.success();
    }

    @Override
    public void handleLifecycleEvent(final VMDeviceLifecycleEventType event) {
        switch (event) {
            case RESUME_RUNNING:
                awaitStorageOperation();
                break;
            case UNLOAD:
                unload();
                break;
        }
    }

    @Override
    public void exportToItemStack(final CompoundNBT nbt) {
        if (blobHandle != null) {
            nbt.putUniqueId(BLOB_HANDLE_NBT_TAG_NAME, blobHandle);
        }
    }

    @Override
    public void importFromItemStack(final CompoundNBT nbt) {
        if (nbt.hasUniqueId(BLOB_HANDLE_NBT_TAG_NAME)) {
            blobHandle = nbt.getUniqueId(BLOB_HANDLE_NBT_TAG_NAME);
        }
    }

    @Override
    public CompoundNBT serializeNBT() {
        final CompoundNBT nbt = new CompoundNBT();

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
        if (device != null) {
            deviceNbt = NBTSerialization.serialize(device);
        }
        if (deviceNbt != null) {
            nbt.put(DEVICE_NBT_TAG_NAME, deviceNbt);
        }
        if (address.isPresent()) {
            nbt.putLong(ADDRESS_NBT_TAG_NAME, address.getAsLong());
        }
        if (interrupt.isPresent()) {
            nbt.putInt(INTERRUPT_NBT_TAG_NAME, interrupt.getAsInt());
        }

        if (blobHandle != null) {
            nbt.putUniqueId(BLOB_HANDLE_NBT_TAG_NAME, blobHandle);
        }

        return nbt;
    }

    @Override
    public void deserializeNBT(final CompoundNBT nbt) {
        if (nbt.hasUniqueId(BLOB_HANDLE_NBT_TAG_NAME)) {
            blobHandle = nbt.getUniqueId(BLOB_HANDLE_NBT_TAG_NAME);
        }

        if (nbt.contains(DEVICE_NBT_TAG_NAME, NBTTagIds.TAG_COMPOUND)) {
            deviceNbt = nbt.getCompound(DEVICE_NBT_TAG_NAME);
        }
        if (nbt.contains(ADDRESS_NBT_TAG_NAME, NBTTagIds.TAG_LONG)) {
            address.set(nbt.getLong(ADDRESS_NBT_TAG_NAME));
        }
        if (nbt.contains(INTERRUPT_NBT_TAG_NAME, NBTTagIds.TAG_INT)) {
            interrupt.set(nbt.getInt(INTERRUPT_NBT_TAG_NAME));
        }
    }

    ///////////////////////////////////////////////////////////////

    protected abstract int getSize();

    protected abstract T createDevice();

    protected abstract Optional<InputStream> getSerializationStream(T device);

    protected abstract OutputStream getDeserializationStream(T device);

    ///////////////////////////////////////////////////////////////

    private boolean allocateDevice(final VMContext context) {
        if (!context.getMemoryAllocator().claimMemory(getSize())) {
            return false;
        }

        data = createDevice();
        device = new VirtIOBlockDevice(context.getMemoryMap(), data);

        return true;
    }

    private void loadPersistedState() {
        if (blobHandle != null) {
            jobHandle = BlobStorage.submitLoad(blobHandle, getDeserializationStream(data));
        }
        if (deviceNbt != null) {
            NBTSerialization.deserialize(deviceNbt, device);
        }
    }

    private void awaitStorageOperation() {
        if (jobHandle != null) {
            jobHandle.await();
            jobHandle = null;
        }
    }

    private void unload() {
        // Since we cannot serialize the data in a regular serialize call due to the
        // actual data being unloaded at that point, but want to permanently persist
        // it (it's the contents of the block device) we need to serialize it in the
        // unload, too. Don't need to wait for the job, though.
        if (data != null) {
            final Optional<InputStream> optional = getSerializationStream(data);
            optional.ifPresent(stream -> {
                blobHandle = BlobStorage.validateHandle(blobHandle);
                BlobStorage.submitSave(blobHandle, stream);
            });
            if (!optional.isPresent()) {
                BlobStorage.freeHandle(blobHandle);
                blobHandle = null;
            }
        }

        data = null;
        jobHandle = null;

        device = null;
        deviceNbt = null;
        address.clear();
        interrupt.clear();
    }
}
