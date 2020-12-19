package li.cil.oc2.common.bus.device;

import li.cil.oc2.Constants;
import li.cil.oc2.api.bus.device.vm.*;
import li.cil.oc2.common.bus.device.provider.util.AbstractObjectProxy;
import li.cil.oc2.common.serialization.BlobStorage;
import li.cil.oc2.common.util.NBTTagIds;
import li.cil.sedna.api.device.PhysicalMemory;
import li.cil.sedna.device.memory.Memory;
import li.cil.sedna.memory.PhysicalMemoryInputStream;
import li.cil.sedna.memory.PhysicalMemoryOutputStream;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;

import java.util.OptionalLong;
import java.util.UUID;

public final class MemoryDevice extends AbstractObjectProxy<ItemStack> implements VMDevice, VMDeviceLifecycleListener {
    private static final String BLOB_HANDLE_NBT_TAG_NAME = "blob";
    private static final String ADDRESS_NBT_TAG_NAME = "address";

    final int RAM_SIZE = 8 * Constants.MEGABYTE;

    ///////////////////////////////////////////////////////////////

    private BlobStorage.JobHandle jobHandle;
    private PhysicalMemory device;

    ///////////////////////////////////////////////////////////////

    private UUID blobHandle;
    private Long address;

    ///////////////////////////////////////////////////////////////

    public MemoryDevice(final ItemStack value) {
        super(value);
    }

    @Override
    public VMDeviceLoadResult load(final VMContext context) {
        if (!allocateDevice(context)) {
            return VMDeviceLoadResult.fail();
        }

        if (!claimAddress(context)) {
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
    public CompoundNBT serializeNBT() {
        final CompoundNBT nbt = new CompoundNBT();

        if (device != null) {
            blobHandle = BlobStorage.validateHandle(blobHandle);
            nbt.putUniqueId(BLOB_HANDLE_NBT_TAG_NAME, blobHandle);

            jobHandle = BlobStorage.submitSave(blobHandle, new PhysicalMemoryInputStream(device));
        }
        if (address != null) {
            nbt.putLong(ADDRESS_NBT_TAG_NAME, address);
        }

        return nbt;
    }

    @Override
    public void deserializeNBT(final CompoundNBT nbt) {
        if (nbt.hasUniqueId(BLOB_HANDLE_NBT_TAG_NAME)) {
            blobHandle = nbt.getUniqueId(BLOB_HANDLE_NBT_TAG_NAME);
        }
        if (nbt.contains(ADDRESS_NBT_TAG_NAME, NBTTagIds.TAG_LONG)) {
            address = nbt.getLong(ADDRESS_NBT_TAG_NAME);
        }
    }

    ///////////////////////////////////////////////////////////////

    private boolean allocateDevice(final VMContext context) {
        if (!context.getMemoryAllocator().claimMemory(RAM_SIZE)) {
            return false;
        }

        device = Memory.create(RAM_SIZE);

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

    private void loadPersistedState() {
        if (blobHandle != null) {
            jobHandle = BlobStorage.submitLoad(blobHandle, new PhysicalMemoryOutputStream(device));
        }
    }

    private void awaitStorageOperation() {
        if (jobHandle != null) {
            jobHandle.await();
            jobHandle = null;
        }
    }

    private void unload() {
        // RAM is volatile, so free up our persisted blob when device is unloaded.
        BlobStorage.freeHandle(blobHandle);
        blobHandle = null;

        device = null;
        address = null;
        jobHandle = null;
    }
}
