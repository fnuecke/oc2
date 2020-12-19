package li.cil.oc2.common.bus.device;

import li.cil.oc2.api.bus.device.vm.*;
import li.cil.oc2.common.bus.device.provider.util.AbstractObjectProxy;
import li.cil.oc2.common.serialization.BlobStorage;
import li.cil.oc2.common.serialization.NBTSerialization;
import li.cil.oc2.common.util.NBTTagIds;
import li.cil.oc2.common.vm.Allocator;
import li.cil.sedna.api.device.BlockDevice;
import li.cil.sedna.device.virtio.VirtIOBlockDevice;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.UUID;

public abstract class AbstractHardDiskDriveDevice<T extends BlockDevice> extends AbstractObjectProxy<ItemStack> implements VMDevice, VMDeviceLifecycleListener {
    private static final UUID SERIALIZATION_KEY = UUID.fromString("8842cf60-a6e6-44d2-b442-380007625078");

    private static final String DEVICE_NBT_TAG_NAME = "device";
    private static final String ADDRESS_NBT_TAG_NAME = "address";
    private static final String INTERRUPT_NBT_TAG_NAME = "interrupt";
    private static final String BLOB_HANDLE_NBT_TAG_NAME = "blob";

    ///////////////////////////////////////////////////////////////

    private final UUID allocHandle = Allocator.createHandle();
    private BlobStorage.JobHandle jobHandle;
    private T data;
    private VirtIOBlockDevice device;

    ///////////////////////////////////////////////////////////////

    // Online persisted data.
    private CompoundNBT deviceNbt;
    private Long address;
    private Integer interrupt;

    // Offline persisted data.
    private UUID blobHandle;

    ///////////////////////////////////////////////////////////////

    protected AbstractHardDiskDriveDevice(final ItemStack stack) {
        super(stack);
    }

    @Override
    public VMDeviceLoadResult load(final VMContext context) {
        if (!allocateDevice(context)) {
            return VMDeviceLoadResult.fail();
        }

        if (!claimAddress(context)) {
            Allocator.freeMemory(allocHandle);
            return VMDeviceLoadResult.fail();
        }

        if (!claimInterrupt(context)) {
            Allocator.freeMemory(allocHandle);
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
    public Optional<UUID> getSerializationKey() {
        return Optional.of(SERIALIZATION_KEY);
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
        if (address != null) {
            nbt.putLong(ADDRESS_NBT_TAG_NAME, address);
        }
        if (interrupt != null) {
            nbt.putInt(INTERRUPT_NBT_TAG_NAME, interrupt);
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
            address = nbt.getLong(ADDRESS_NBT_TAG_NAME);
        }
        if (nbt.contains(INTERRUPT_NBT_TAG_NAME, NBTTagIds.TAG_INT)) {
            interrupt = nbt.getInt(INTERRUPT_NBT_TAG_NAME);
        }
    }

    ///////////////////////////////////////////////////////////////

    protected abstract int getSize();

    protected abstract T createDevice();

    protected abstract Optional<InputStream> getSerializationStream(T device);

    protected abstract OutputStream getDeserializationStream(T device);

    ///////////////////////////////////////////////////////////////

    private boolean allocateDevice(final VMContext context) {
        if (!Allocator.claimMemory(allocHandle, getSize())) {
            return false;
        }

        data = createDevice();
        device = new VirtIOBlockDevice(context.getMemoryMap(), data);

        return true;
    }

    private boolean claimAddress(final VMContext context) {
        final OptionalLong claimedAddress;
        if (this.address != null) {
            claimedAddress = context.getMemoryRangeAllocator().claimMemoryRange(this.address, device);
        } else {
            claimedAddress = context.getMemoryRangeAllocator().claimMemoryRange(device);
        }

        if (!claimedAddress.isPresent()) {
            return false;
        }

        this.address = claimedAddress.getAsLong();

        return true;
    }

    private boolean claimInterrupt(final VMContext context) {
        final OptionalInt claimedInterrupt;
        if (this.interrupt != null) {
            claimedInterrupt = context.getInterruptAllocator().claimInterrupt(this.interrupt);
        } else {
            claimedInterrupt = context.getInterruptAllocator().claimInterrupt();
        }

        if (!claimedInterrupt.isPresent()) {
            return false;
        }

        this.interrupt = claimedInterrupt.getAsInt();

        device.getInterrupt().set(this.interrupt, context.getInterruptController());

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

        Allocator.freeMemory(allocHandle);
        data = null;

        device = null;
        deviceNbt = null;
        address = null;
        interrupt = null;
    }
}
