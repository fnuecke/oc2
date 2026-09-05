/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.vm.block;

import li.cil.oc2.api.bus.device.data.BlockDeviceData;
import li.cil.oc2.common.Config;
import li.cil.oc2.common.bus.device.provider.item.FlashMemoryItemDeviceProvider;
import li.cil.oc2.common.bus.device.vm.item.AbstractBlockStorageDevice;
import li.cil.oc2.common.bus.device.vm.item.FloppyMedia;
import li.cil.oc2.common.item.FlashMemoryItem;
import li.cil.oc2.common.serialization.BlobStorage;
import li.cil.oc2.common.util.ItemDeviceUtils;
import li.cil.oc2.common.util.StorageItemUtils;
import li.cil.sedna.api.device.BlockDevice;
import li.cil.sedna.device.block.ByteBufferBlockDevice;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class FlashDriveDevice<T extends BlockEntity & FlashDriveContainer> extends AbstractBlockStorageDevice<BlockDevice, T> {
    private static final ByteBufferBlockDevice EMPTY_BLOCK_DEVICE = ByteBufferBlockDevice.create(0, false);

    // --------------------------------------------------------------------- //

    public FlashDriveDevice(final T container) {
        super(container, false);
    }

    // --------------------------------------------------------------------- //

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

    @Override
    protected int getMappedByteCount() {
        return getMediumCapacity(identity.getFlashItemStack());
    }

    @Override
    protected CompletableFuture<BlockDevice> createBlockDevice() throws IOException {
        final ItemStack stack = identity.getFlashItemStack();
        if (stack.isEmpty() || !(stack.getItem() instanceof FlashMemoryItem)) {
            return CompletableFuture.completedFuture(EMPTY_BLOCK_DEVICE);
        }

        final int capacity = getMediumCapacity(stack);
        if (capacity <= 0) {
            return CompletableFuture.completedFuture(EMPTY_BLOCK_DEVICE);
        }

        if (!BlobStorage.isValidHandle(blobHandle)) {
            importFromItemStack(ItemDeviceUtils.getDeviceData(stack, FlashMemoryItemDeviceProvider.DEVICE_DATA_KEY));
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

        final BlockDeviceData data = ((FlashMemoryItem) stack.getItem()).getData(stack);
        return CompletableFuture.supplyAsync(() -> {
            try {
                final ByteBufferBlockDevice medium = ByteBufferBlockDevice.createFromFileChannel(channel, capacity, false);
                if (isNew && data != null) {
                    FloppyMedia.image(medium, data.getBlockDevice());
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
    protected void handleDataUnavailable() {
        StorageItemUtils.setState(identity.getFlashItemStack(), StorageItemUtils.State.CORRUPTED);
        identity.setChanged();
    }

    // --------------------------------------------------------------------- //

    private static int getMediumCapacity(final ItemStack stack) {
        if (!(stack.getItem() instanceof final FlashMemoryItem flash)) {
            return 0;
        }

        return Math.min(flash.getCapacity(stack), Config.maxBlobCapacity);
    }

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
}
