package li.cil.oc2.common.bus.device.provider;

import li.cil.oc2.Constants;
import li.cil.oc2.OpenComputers;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.api.bus.device.vm.*;
import li.cil.oc2.common.bus.device.provider.util.AbstractItemDeviceProvider;
import li.cil.oc2.common.bus.device.provider.util.AbstractObjectProxy;
import li.cil.oc2.common.serialization.BlobStorage;
import li.cil.oc2.common.util.NBTTagIds;
import li.cil.oc2.common.vm.Allocator;
import li.cil.sedna.api.device.PhysicalMemory;
import li.cil.sedna.device.memory.Memory;
import li.cil.sedna.memory.PhysicalMemoryInputStream;
import li.cil.sedna.memory.PhysicalMemoryOutputStream;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.common.util.LazyOptional;

import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

public final class MemoryItemDeviceProvider extends AbstractItemDeviceProvider {
    public MemoryItemDeviceProvider() {
        super(OpenComputers.RAM_8M_ITEM.get());
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected LazyOptional<Device> getItemDevice(final ItemDeviceQuery query) {
        return LazyOptional.of(() -> new MemoryDevice(query.getItemStack()));
    }

    ///////////////////////////////////////////////////////////////////

    private static final class MemoryDevice extends AbstractObjectProxy<ItemStack> implements VMDevice, VMDeviceLifecycleListener {
        private static final UUID SERIALIZATION_KEY = UUID.fromString("c82f8c1c-d7ff-43b0-ab44-989e1fe818bb");

        private static final String BLOB_HANDLE_NBT_TAG_NAME = "blob";
        private static final String ADDRESS_NBT_TAG_NAME = "address";

        final int RAM_SIZE = 8 * Constants.MEGABYTE;

        ///////////////////////////////////////////////////////////////

        private final UUID allocHandle = Allocator.createHandle();
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
            if (!allocateDevice()) {
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
        public Optional<UUID> getSerializationKey() {
            return Optional.of(SERIALIZATION_KEY);
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

        private boolean allocateDevice() {
            if (!Allocator.claimMemory(allocHandle, RAM_SIZE)) {
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
                Allocator.freeMemory(allocHandle);
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
            // Finish saves on unload to ensure future loads will read correct data.
            awaitStorageOperation();

            Allocator.freeMemory(allocHandle);
            device = null;

            // RAM is volatile, so free up our persisted blob when device is unloaded.
            BlobStorage.freeHandle(blobHandle);
            blobHandle = null;

            address = null;
        }
    }
}
