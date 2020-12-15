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

    @Override
    protected LazyOptional<Device> getItemDevice(final ItemDeviceQuery query) {
        return LazyOptional.of(() -> new MemoryDevice(query.getItemStack()));
    }

    private static final class MemoryDevice extends AbstractObjectProxy<ItemStack> implements VMDevice, VMLifecycleEventListener {
        private static final UUID SERIALIZATION_KEY = UUID.fromString("c82f8c1c-d7ff-43b0-ab44-989e1fe818bb");

        private static final String RAM_BLOB_HANDLE_NBT_TAG_NAME = "ram";
        private static final String RAM_ADDRESS_NBT_TAG_NAME = "address";

        final int RAM_SIZE = 8 * Constants.MEGABYTE;

        private final UUID ramAllocHandle = Allocator.createHandle();
        private BlobStorage.JobHandle ramJobHandle;

        private PhysicalMemory ram;
        private UUID ramBlobHandle;

        private Long address;

        public MemoryDevice(final ItemStack value) {
            super(value);
        }

        @Override
        public VMDeviceLoadResult load(final VMContext context) {
            if (!Allocator.claimMemory(ramAllocHandle, RAM_SIZE)) {
                return VMDeviceLoadResult.fail();
            }

            ram = Memory.create(RAM_SIZE);

            final OptionalLong claimedAddress;
            if (this.address != null) {
                claimedAddress = context.getMemoryRangeAllocator().claimMemoryRange(this.address, ram);
            } else {
                claimedAddress = context.getMemoryRangeAllocator().claimMemoryRange(ram);
            }

            if (!claimedAddress.isPresent()) {
                Allocator.freeMemory(ramAllocHandle);
                return VMDeviceLoadResult.fail();
            }

            this.address = claimedAddress.getAsLong();

            if (ramBlobHandle != null) {
                if (ramJobHandle != null) {
                    ramJobHandle.await();
                }

                ramJobHandle = BlobStorage.submitLoad(ramBlobHandle, new PhysicalMemoryOutputStream(ram));
            }

            return VMDeviceLoadResult.success();
        }

        @Override
        public void unload() {
            // Finish saves on unload to ensure future loads will read correct data.
            if (ramJobHandle != null) {
                ramJobHandle.await();
                ramJobHandle = null;
            }

            BlobStorage.freeHandle(ramBlobHandle);
            ramBlobHandle = null;

            Allocator.freeMemory(ramAllocHandle);
            ram = null;

            address = null;
        }

        @Override
        public void handleLifecycleEvent(final VMLifecycleEventType event) {
            if (event == VMLifecycleEventType.RESUME_RUNNING) {
                if (ramJobHandle != null) {
                    ramJobHandle.await();
                    ramJobHandle = null;
                }
            }
        }

        @Override
        public Optional<UUID> getSerializationKey() {
            return Optional.of(SERIALIZATION_KEY);
        }

        @Override
        public CompoundNBT serializeNBT() {
            final CompoundNBT nbt = new CompoundNBT();

            if (ram != null) {
                if (ramJobHandle != null) {
                    ramJobHandle.await();
                }

                ramBlobHandle = BlobStorage.validateHandle(ramBlobHandle);
                nbt.putUniqueId(RAM_BLOB_HANDLE_NBT_TAG_NAME, ramBlobHandle);

                ramJobHandle = BlobStorage.submitSave(ramBlobHandle, new PhysicalMemoryInputStream(ram));
            }

            if (address != null) {
                nbt.putLong(RAM_ADDRESS_NBT_TAG_NAME, address);
            }

            return nbt;
        }

        @Override
        public void deserializeNBT(final CompoundNBT nbt) {
            if (nbt.hasUniqueId(RAM_BLOB_HANDLE_NBT_TAG_NAME)) {
                ramBlobHandle = nbt.getUniqueId(RAM_BLOB_HANDLE_NBT_TAG_NAME);
            }
            if (nbt.contains(RAM_ADDRESS_NBT_TAG_NAME, NBTTagIds.TAG_LONG)) {
                address = nbt.getLong(RAM_ADDRESS_NBT_TAG_NAME);
            }
        }
    }
}
