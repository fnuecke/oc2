package li.cil.oc2.common.bus.device.item;

import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.vm.*;
import li.cil.oc2.common.Config;
import li.cil.oc2.common.bus.device.util.IdentityProxy;
import li.cil.oc2.common.bus.device.util.OptionalAddress;
import li.cil.oc2.common.item.MemoryItem;
import li.cil.oc2.common.serialization.BlobStorage;
import li.cil.oc2.common.util.NBTTagIds;
import li.cil.sedna.api.device.PhysicalMemory;
import li.cil.sedna.device.memory.Memory;
import li.cil.sedna.memory.PhysicalMemoryInputStream;
import li.cil.sedna.memory.PhysicalMemoryOutputStream;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.MathHelper;

import java.util.UUID;

public final class MemoryDevice extends IdentityProxy<ItemStack> implements VMDevice, VMDeviceLifecycleListener, ItemDevice {
    private static final String BLOB_HANDLE_TAG_NAME = "blob";
    private static final String ADDRESS_TAG_NAME = "address";

    ///////////////////////////////////////////////////////////////

    private final int size;
    private BlobStorage.JobHandle jobHandle;
    private PhysicalMemory device;

    ///////////////////////////////////////////////////////////////

    private final OptionalAddress address = new OptionalAddress();
    private UUID blobHandle;

    ///////////////////////////////////////////////////////////////

    public MemoryDevice(final ItemStack identity) {
        super(identity);
        size = MathHelper.clamp(MemoryItem.getCapacity(identity), 0, Config.maxMemorySize);
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
            nbt.putUniqueId(BLOB_HANDLE_TAG_NAME, blobHandle);

            jobHandle = BlobStorage.submitSave(blobHandle, new PhysicalMemoryInputStream(device));
        }
        if (address.isPresent()) {
            nbt.putLong(ADDRESS_TAG_NAME, address.getAsLong());
        }

        return nbt;
    }

    @Override
    public void deserializeNBT(final CompoundNBT tag) {
        if (tag.hasUniqueId(BLOB_HANDLE_TAG_NAME)) {
            blobHandle = tag.getUniqueId(BLOB_HANDLE_TAG_NAME);
        }
        if (tag.contains(ADDRESS_TAG_NAME, NBTTagIds.TAG_LONG)) {
            address.set(tag.getLong(ADDRESS_TAG_NAME));
        }
    }

    ///////////////////////////////////////////////////////////////

    private boolean allocateDevice(final VMContext context) {
        if (!context.getMemoryAllocator().claimMemory(size)) {
            return false;
        }

        device = Memory.create(size);

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
        // Memory is volatile, so free up our persisted blob when device is unloaded.
        BlobStorage.freeHandle(blobHandle);
        blobHandle = null;
        jobHandle = null;

        device = null;
        address.clear();
    }
}
