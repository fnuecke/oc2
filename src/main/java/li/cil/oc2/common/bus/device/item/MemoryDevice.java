package li.cil.oc2.common.bus.device.item;

import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.vm.VMDevice;
import li.cil.oc2.api.bus.device.vm.VMDeviceLoadResult;
import li.cil.oc2.api.bus.device.vm.context.VMContext;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.bus.device.util.IdentityProxy;
import li.cil.oc2.common.bus.device.util.OptionalAddress;
import li.cil.oc2.common.serialization.BlobStorage;
import li.cil.oc2.common.util.NBTTagIds;
import li.cil.sedna.api.device.PhysicalMemory;
import li.cil.sedna.device.memory.ByteBufferMemory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.UUID;

public final class MemoryDevice extends IdentityProxy<ItemStack> implements VMDevice, ItemDevice {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final String BLOB_HANDLE_TAG_NAME = "blob";
    private static final String ADDRESS_TAG_NAME = "address";

    ///////////////////////////////////////////////////////////////

    private final int size;
    private PhysicalMemory device;

    ///////////////////////////////////////////////////////////////

    private final OptionalAddress address = new OptionalAddress();
    private UUID blobHandle;

    ///////////////////////////////////////////////////////////////

    public MemoryDevice(final ItemStack identity, final int capacity) {
        super(identity);
        size = capacity;
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

        context.getEventBus().register(this);

        return VMDeviceLoadResult.success();
    }

    @Override
    public void unmount() {
        if (device != null) {
            try {
                device.close();
            } catch (final Exception e) {
                LOGGER.error(e);
            }

            device = null;
        }

        if (blobHandle != null) {
            BlobStorage.close(blobHandle);
        }
    }

    @Override
    public void dispose() {
        // Memory is volatile, so free up our persisted blob when device is disposed.
        if (blobHandle != null) {
            BlobStorage.delete(blobHandle);
            blobHandle = null;
        }

        address.clear();
    }

    @Override
    public CompoundTag serializeNBT() {
        final CompoundTag tag = new CompoundTag();

        if (blobHandle != null) {
            tag.putUUID(BLOB_HANDLE_TAG_NAME, blobHandle);
        }
        if (address.isPresent()) {
            tag.putLong(ADDRESS_TAG_NAME, address.getAsLong());
        }

        return tag;
    }

    @Override
    public void deserializeNBT(final CompoundTag tag) {
        if (tag.hasUUID(BLOB_HANDLE_TAG_NAME)) {
            blobHandle = tag.getUUID(BLOB_HANDLE_TAG_NAME);
        }
        if (tag.contains(ADDRESS_TAG_NAME, NBTTagIds.TAG_LONG)) {
            address.set(tag.getLong(ADDRESS_TAG_NAME));
        }
    }

    ///////////////////////////////////////////////////////////////

    private boolean allocateDevice(final VMContext context) {
        if (!context.getMemoryAllocator().claimMemory(Constants.PAGE_SIZE)) {
            return false;
        }

        try {
            blobHandle = BlobStorage.validateHandle(blobHandle);
            final FileChannel channel = BlobStorage.getOrOpen(blobHandle);
            final MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, size);
            device = new ByteBufferMemory(size, buffer);
        } catch (final IOException e) {
            return false;
        }

        return true;
    }
}
