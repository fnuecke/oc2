/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.vm.block;

import li.cil.oc2.common.bus.device.vm.item.AbstractBlockStorageDevice;
import li.cil.oc2.common.item.FloppyItem;
import li.cil.oc2.common.serialization.BlobStorage;
import li.cil.sedna.api.device.BlockDevice;
import li.cil.sedna.device.block.ByteBufferBlockDevice;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.CompletableFuture;

public final class DiskDriveDevice<T extends BlockEntity & DiskDriveContainer> extends AbstractBlockStorageDevice<BlockDevice, T> {
    private static final ByteBufferBlockDevice EMPTY_BLOCK_DEVICE = ByteBufferBlockDevice.create(0, false);

    ///////////////////////////////////////////////////////////////

    public DiskDriveDevice(final T container) {
        super(container, false);
    }

    ///////////////////////////////////////////////////////////////

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

        setOpenJob(createBlockDevice().thenAcceptAsync(blockDevice -> {
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

    ///////////////////////////////////////////////////////////////

    @Override
    protected CompletableFuture<BlockDevice> createBlockDevice() {
        final ItemStack stack = identity.getDiskItemStack();
        if (stack.isEmpty() || !(stack.getItem() instanceof final FloppyItem floppy)) {
            return CompletableFuture.completedFuture(EMPTY_BLOCK_DEVICE);
        }

        final int capacity = floppy.getCapacity(stack);
        if (capacity <= 0) {
            return CompletableFuture.completedFuture(EMPTY_BLOCK_DEVICE);
        }

        blobHandle = BlobStorage.validateHandle(blobHandle);
        return CompletableFuture.supplyAsync(() -> {
            try {
                final FileChannel channel = BlobStorage.getOrOpen(blobHandle);
                final MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, capacity);
                return ByteBufferBlockDevice.wrap(buffer, false);
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        }, WORKERS);
    }

    @Override
    protected void handleDataAccess() {
        identity.handleDataAccess();
    }
}
