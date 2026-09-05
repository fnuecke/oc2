/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.vm.block;

import li.cil.oc2.common.bus.device.vm.item.AbstractBlockStorageDevice;
import li.cil.oc2.common.serialization.BlobStorage;
import li.cil.oc2.common.util.ItemDeviceUtils;
import li.cil.oc2.common.util.StorageItemUtils;
import li.cil.sedna.api.device.BlockDevice;
import li.cil.sedna.device.block.ByteBufferBlockDevice;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public abstract class AbstractRemovableMediaDevice extends AbstractBlockStorageDevice<BlockDevice, RemovableMediaContainer> {
    protected static final ByteBufferBlockDevice EMPTY_BLOCK_DEVICE = ByteBufferBlockDevice.create(0, false);
    protected static final MediumInitializer LEAVE_BLANK = medium -> {
    };

    // --------------------------------------------------------------------- //

    protected AbstractRemovableMediaDevice(final RemovableMediaContainer container) {
        super(container, false);
    }

    // --------------------------------------------------------------------- //

    public abstract String getDeviceDataKey();

    public void updateBlockDevice(final CompoundTag tag) {
        if (!checkAndClearBlockDevice()) {
            return;
        }

        importFromItemStack(tag);

        final CompletableFuture<BlockDevice> job;
        try {
            job = createBlockDevice();
        } catch (final IOException e) {
            handleDataUnavailable();
            return;
        }

        setOpenJob(job.thenAcceptAsync(blockDevice -> {
            try {
                setMedium(blockDevice);
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        }, WORKERS));
    }

    public void removeBlockDevice() {
        checkAndClearBlockDevice();
    }

    // --------------------------------------------------------------------- //

    protected abstract int getMediumCapacity(ItemStack stack);

    protected abstract boolean isSupportedMedium(ItemStack stack);

    protected abstract MediumInitializer createMediumInitializer(ItemStack stack);

    @Override
    protected int getMappedByteCount() {
        return getMediumCapacity(identity.getMediaItemStack());
    }

    @Override
    protected CompletableFuture<BlockDevice> createBlockDevice() throws IOException {
        final ItemStack stack = identity.getMediaItemStack();
        if (stack.isEmpty() || !isSupportedMedium(stack)) {
            return CompletableFuture.completedFuture(EMPTY_BLOCK_DEVICE);
        }

        final int capacity = getMediumCapacity(stack);
        if (capacity <= 0) {
            return CompletableFuture.completedFuture(EMPTY_BLOCK_DEVICE);
        }

        if (!BlobStorage.isValidHandle(blobHandle)) {
            importFromItemStack(ItemDeviceUtils.getDeviceData(stack, getDeviceDataKey()));
        }

        final boolean isNew = !BlobStorage.isValidHandle(blobHandle);
        final UUID handle = isNew ? BlobStorage.allocateHandle() : blobHandle;

        final FileChannel channel;
        try {
            channel = BlobStorage.open(handle, isNew);
        } catch (final BlobStorage.BlobMissingException | BlobStorage.BlobInUseException e) {
            handleDataUnavailable();
            return CompletableFuture.completedFuture(EMPTY_BLOCK_DEVICE);
        }

        blobHandle = handle;

        final MediumInitializer initializer = isNew ? createMediumInitializer(stack) : LEAVE_BLANK;
        return CompletableFuture.supplyAsync(() -> {
            try {
                final ByteBufferBlockDevice medium = ByteBufferBlockDevice.createFromFileChannel(channel, capacity, false);
                if (isNew) {
                    initializer.initialize(medium);
                }
                return medium;
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        }, WORKERS);
    }

    @Override
    protected void setBlockDevice(final BlockDevice blockDevice) throws IOException {
        setMedium(blockDevice);
    }

    @Override
    protected void handleDataAccess() {
        identity.handleDataAccess();
    }

    @Override
    protected void handleDataUnavailable() {
        StorageItemUtils.setState(identity.getMediaItemStack(), StorageItemUtils.State.CORRUPTED);
        identity.setChanged();
    }

    // --------------------------------------------------------------------- //

    private void setMedium(@Nullable final BlockDevice medium) throws IOException {
        if (storage == null) {
            return;
        }

        final BlockDevice present = medium == EMPTY_BLOCK_DEVICE ? null : medium;
        storage.setBlockDevice(present != null ? withAccessListener(present) : null);
    }

    private boolean checkAndClearBlockDevice() {
        joinOpenJob();

        if (storage == null) {
            return false;
        }

        runtime.get().join();

        try {
            setMedium(null);
        } catch (final IOException e) {
            LOGGER.error(e);
        }

        if (blobHandle != null) {
            BlobStorage.close(blobHandle);
            blobHandle = null;
        }
        return true;
    }

    // --------------------------------------------------------------------- //

    @FunctionalInterface
    protected interface MediumInitializer {
        void initialize(ByteBufferBlockDevice medium) throws IOException;
    }
}
