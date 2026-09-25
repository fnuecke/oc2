/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.vm.item;

import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.vm.VMDevice;
import li.cil.oc2.api.bus.device.vm.VMDeviceLoadResult;
import li.cil.oc2.api.bus.device.vm.context.VMContext;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.bus.device.util.IdentityProxy;
import li.cil.oc2.common.bus.device.util.OptionalAddress;
import li.cil.oc2.common.serialization.BlobReference;
import li.cil.oc2.common.serialization.BlobStorage;
import li.cil.oc2.common.util.NBTTagIds;
import li.cil.sedna.api.device.PhysicalMemory;
import li.cil.sedna.device.memory.ByteBufferMemory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public final class MemoryDevice extends IdentityProxy<ItemStack> implements VMDevice, ItemDevice {
    private static final Logger LOGGER = LogManager.getLogger(MemoryDevice.class);

    private static final String ADDRESS_TAG_NAME = "address";

    // --------------------------------------------------------------------- //

    private final int size;
    private PhysicalMemory device;

    // --------------------------------------------------------------------- //

    private final OptionalAddress address = new OptionalAddress();
    private final BlobReference blob = new BlobReference();

    // --------------------------------------------------------------------- //

    public MemoryDevice(final ItemStack identity, final int capacity) {
        super(identity);
        size = capacity;
    }

    // --------------------------------------------------------------------- //

    @Override
    public VMDeviceLoadResult mount(final VMContext context) {
        if (allocateDevice(context) instanceof AllocationFailure(Component message, boolean permanent)) {
            VMDeviceLoadResult result = VMDeviceLoadResult.fail();
            if (message != null) {
                result = result.withErrorMessage(message);
            }
            return permanent ? result.asPermanent() : result;
        }

        // RAM that doesn't fit shouldn't prevent boot. This is usually the case for the Z80 e.g.
        if (!address.claim(context.getMemoryRangeAllocator(), device)) {
            closeDevice();
            blob.delete();
        }

        return VMDeviceLoadResult.success();
    }

    @Override
    public void unmount() {
        closeDevice();
        blob.close();
    }

    @Override
    public void dispose() {
        // Memory is volatile, so free up our persisted blob when device is disposed.
        blob.delete();

        address.clear();
    }

    @Override
    public CompoundTag serializeNBT() {
        final CompoundTag tag = new CompoundTag();

        blob.writeTo(tag);
        if (address.isPresent()) {
            tag.putLong(ADDRESS_TAG_NAME, address.getAsLong());
        }

        return tag;
    }

    @Override
    public void deserializeNBT(final CompoundTag tag) {
        blob.readFrom(tag);
        if (tag.contains(ADDRESS_TAG_NAME, NBTTagIds.TAG_LONG)) {
            address.set(tag.getLong(ADDRESS_TAG_NAME));
        }
    }

    // --------------------------------------------------------------------- //

    private AllocationResult allocateDevice(final VMContext context) {
        if (!context.getMemoryAllocator().claimMemory(size)) {
            return new AllocationFailure(true);
        }

        if (blob.isStale()) {
            blob.delete();
            return new AllocationFailure(Component.translatable(Constants.COMPUTER_ERROR_STATE_LOST), true);
        }

        try {
            final MappedByteBuffer buffer = blob.open().map(FileChannel.MapMode.READ_WRITE, 0, size);
            device = new ByteBufferMemory(size, buffer);
        } catch (final BlobStorage.BlobStorageFullException e) {
            LOGGER.error(e);
            return new AllocationFailure(Component.translatable(Constants.COMPUTER_ERROR_STORAGE_FULL), false);
        } catch (final BlobStorage.BlobMissingException | BlobStorage.BlobInUseException e) {
            blob.release();
            return new AllocationFailure(Component.translatable(Constants.COMPUTER_ERROR_MEMORY_CORRUPTED), true);
        } catch (final IOException e) {
            LOGGER.error(e);
            blob.close();
            return new AllocationFailure(false);
        }

        return new AllocationSuccess();
    }

    private void closeDevice() {
        if (device == null) {
            return;
        }

        try {
            device.close();
        } catch (final Exception e) {
            LOGGER.error(e);
        }

        device = null;
    }

    private sealed interface AllocationResult permits AllocationSuccess, AllocationFailure {
    }

    private record AllocationSuccess() implements AllocationResult {
    }

    private record AllocationFailure(@Nullable Component message, boolean permanent) implements AllocationResult {
        public AllocationFailure(final boolean permanent) {
            this(null, permanent);
        }
    }
}
