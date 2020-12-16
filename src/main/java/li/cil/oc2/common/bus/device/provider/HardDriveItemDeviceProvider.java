package li.cil.oc2.common.bus.device.provider;

import li.cil.oc2.Constants;
import li.cil.oc2.OpenComputers;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.api.bus.device.vm.*;
import li.cil.oc2.common.bus.device.provider.util.AbstractItemDeviceProvider;
import li.cil.oc2.common.bus.device.provider.util.AbstractObjectProxy;
import li.cil.oc2.common.serialization.BlobStorage;
import li.cil.oc2.common.serialization.NBTSerialization;
import li.cil.oc2.common.util.ByteBufferInputStream;
import li.cil.oc2.common.util.ByteBufferOutputStream;
import li.cil.oc2.common.util.NBTTagIds;
import li.cil.oc2.common.vm.Allocator;
import li.cil.sedna.device.block.ByteBufferBlockDevice;
import li.cil.sedna.device.virtio.VirtIOBlockDevice;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.common.util.LazyOptional;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.UUID;

public class HardDriveItemDeviceProvider extends AbstractItemDeviceProvider {
    public HardDriveItemDeviceProvider() {
        super(OpenComputers.HDD_8M_ITEM.get());
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected LazyOptional<Device> getItemDevice(final ItemDeviceQuery query) {
        return LazyOptional.of(() -> new HardDiskDriveDevice(query.getItemStack()));
    }

    ///////////////////////////////////////////////////////////////////

    private static final class HardDiskDriveDevice extends AbstractObjectProxy<ItemStack> implements VMDevice, VMDeviceLifecycleListener {
        private static final UUID SERIALIZATION_KEY = UUID.fromString("8842cf60-a6e6-44d2-b442-380007625078");

        private static final String BLOB_HANDLE_NBT_TAG_NAME = "blob";
        private static final String DEVICE_NBT_TAG_NAME = "device";
        private static final String ADDRESS_NBT_TAG_NAME = "address";
        private static final String INTERRUPT_NBT_TAG_NAME = "interrupt";

        final int HDD_SIZE = 2 * Constants.MEGABYTE;

        ///////////////////////////////////////////////////////////////

        private final UUID allocHandle = Allocator.createHandle();
        private BlobStorage.JobHandle jobHandle;
        private ByteBufferBlockDevice data;
        private VirtIOBlockDevice device;

        ///////////////////////////////////////////////////////////////

        private UUID blobHandle;
        private CompoundNBT deviceNbt;
        private Long address;
        private Integer interrupt;

        ///////////////////////////////////////////////////////////////

        public HardDiskDriveDevice(final ItemStack stack) {
            super(stack);
        }

        @Override
        public VMDeviceLoadResult load(final VMContext context) {
            if (!allocateDevice(context)) {
                return VMDeviceLoadResult.fail();
            }

            if (!claimAddress(context)) {
                return VMDeviceLoadResult.fail();
            }

            if (!claimInterrupt(context)) {
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
                blobHandle = BlobStorage.validateHandle(blobHandle);
                nbt.putUniqueId(BLOB_HANDLE_NBT_TAG_NAME, blobHandle);

                jobHandle = BlobStorage.submitSave(blobHandle, new ByteBufferInputStream(data.getView()));
            }
            if (device != null) {
                deviceNbt = NBTSerialization.serialize(device);
                nbt.put(DEVICE_NBT_TAG_NAME, deviceNbt);
            }
            if (address != null) {
                nbt.putLong(ADDRESS_NBT_TAG_NAME, address);
            }
            if (interrupt != null) {
                nbt.putInt(INTERRUPT_NBT_TAG_NAME, interrupt);
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

        private boolean allocateDevice(final VMContext context) {
            if (!Allocator.claimMemory(allocHandle, HDD_SIZE)) {
                return false;
            }

            data = ByteBufferBlockDevice.create(HDD_SIZE, false);
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
                Allocator.freeMemory(allocHandle);
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
                Allocator.freeMemory(allocHandle);
                return false;
            }

            this.interrupt = claimedInterrupt.getAsInt();

            device.getInterrupt().set(this.interrupt, context.getInterruptController());

            return true;
        }

        private void loadPersistedState() {
            if (blobHandle != null) {
                jobHandle = BlobStorage.submitLoad(blobHandle, new ByteBufferOutputStream(data.getView()));
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
            // Finish saves on unload to ensure future loads will read correct data.
            awaitStorageOperation();

            Allocator.freeMemory(allocHandle);
            data = null;

            device = null;
            address = null;
            interrupt = null;
        }
    }
}
