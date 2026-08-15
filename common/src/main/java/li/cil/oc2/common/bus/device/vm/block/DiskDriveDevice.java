/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.vm.block;

import li.cil.oc2.common.bus.device.vm.item.AbstractBlockStorageDevice;
import li.cil.oc2.common.item.FloppyItem;
import li.cil.oc2.common.serialization.BlobStorage;
import li.cil.oc2.common.util.StorageItemUtils;
import li.cil.sedna.api.device.BlockDevice;
import li.cil.sedna.device.block.ByteBufferBlockDevice;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class DiskDriveDevice<T extends BlockEntity & DiskDriveContainer> extends AbstractBlockStorageDevice<BlockDevice, T> {
    private static final ByteBufferBlockDevice EMPTY_BLOCK_DEVICE = ByteBufferBlockDevice.create(0, false);

    // ------------------------------------------------------------- //

    public DiskDriveDevice(final T container) {
        super(container, false);
    }

    // ------------------------------------------------------------- //

    public void updateBlockDevice(final CompoundTag tag) {
        joinOpenJob();

        if (device == null) {
            return;
        }

        try {
            device.setBlock(EMPTY_BLOCK_DEVICE);
        } catch (final IOException e) {
            LOGGER.error(e);
        }

        if (blobHandle != null) {
            BlobStorage.close(blobHandle);
            blobHandle = null;
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
                device.setBlock(blockDevice);
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        }, WORKERS));
    }

    public void removeBlockDevice() {
        joinOpenJob();

        if (device == null) {
            return;
        }

        try {
            device.setBlock(EMPTY_BLOCK_DEVICE);
        } catch (final IOException e) {
            LOGGER.error(e);
        }

        if (blobHandle != null) {
            BlobStorage.close(blobHandle);
            blobHandle = null;
        }
    }

    // ------------------------------------------------------------- //

    @Override
    protected CompletableFuture<BlockDevice> createBlockDevice() throws IOException {
        final ItemStack stack = identity.getDiskItemStack();
        if (stack.isEmpty() || !(stack.getItem() instanceof final FloppyItem floppy)) {
            return CompletableFuture.completedFuture(EMPTY_BLOCK_DEVICE);
        }

        final int capacity = floppy.getCapacity(stack);
        if (capacity <= 0) {
            return CompletableFuture.completedFuture(EMPTY_BLOCK_DEVICE);
        }

        final boolean isNew = !BlobStorage.isValidHandle(blobHandle);
        final UUID handle = isNew ? BlobStorage.allocateHandle() : blobHandle;

        final FileChannel channel;
        try {
            channel = BlobStorage.open(handle, isNew);
        } catch (final BlobStorage.BlobMissingException | BlobStorage.BlobInUseException e) {
            handleDataUnavailable();
            // Unlike HDD, this shouldn't stop a boot; just show up as empty.
            return CompletableFuture.completedFuture(EMPTY_BLOCK_DEVICE);
        }

        blobHandle = handle;

        return CompletableFuture.supplyAsync(() -> {
            try {
                return ByteBufferBlockDevice.createFromFileChannel(channel, capacity, false);
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        }, WORKERS);
    }

    @Override
    protected void handleDataAccess() {
        identity.handleDataAccess();
    }

    @Override
    protected void handleDataUnavailable() {
        StorageItemUtils.setCorrupted(identity.getDiskItemStack());
        identity.setChanged();
    }
}
