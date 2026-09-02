/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.vm.block;

import li.cil.oc2.api.bus.device.data.BlockDeviceData;
import li.cil.oc2.api.bus.device.vm.ArchitectureType;
import li.cil.oc2.api.bus.device.vm.context.VMContext;
import li.cil.oc2.common.Config;
import li.cil.oc2.common.bus.device.vm.item.AbstractBlockStorageDevice;
import li.cil.oc2.common.bus.device.vm.item.FloppyControllerStorage;
import li.cil.oc2.common.bus.device.vm.item.FloppyMedia;
import li.cil.oc2.common.bus.device.vm.item.MappedStorage;
import li.cil.oc2.common.item.FloppyItem;
import li.cil.oc2.common.serialization.BlobStorage;
import li.cil.oc2.common.util.ItemStackUtils;
import li.cil.oc2.common.util.StorageItemUtils;
import li.cil.sedna.api.device.BlockDevice;
import li.cil.sedna.device.block.ByteBufferBlockDevice;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class DiskDriveDevice<T extends BlockEntity & DiskDriveContainer> extends AbstractBlockStorageDevice<BlockDevice, T> {
    public static final String DATA_TAG_NAME = "data";

    private static final ByteBufferBlockDevice EMPTY_BLOCK_DEVICE = ByteBufferBlockDevice.create(0, false);

    // --------------------------------------------------------------------- //

    private final ArchitectureType architectureType;

    // --------------------------------------------------------------------- //

    public DiskDriveDevice(final T container, final ArchitectureType architectureType) {
        super(container, false);
        this.architectureType = architectureType;
    }

    // --------------------------------------------------------------------- //

    public ArchitectureType getArchitectureType() {
        return architectureType;
    }

    @Override
    protected MappedStorage createStorage(final VMContext context) {
        return switch (architectureType) {
            case RISCV -> super.createStorage(context);
            case Z80 -> new FloppyControllerStorage();
        };
    }

    // --------------------------------------------------------------------- //

    @Override
    public boolean equals(@Nullable final Object o) {
        return super.equals(o) && architectureType == ((DiskDriveDevice<?>) o).architectureType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), architectureType);
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
        return Math.min(FloppyItem.MAX_CAPACITY, Config.maxBlobCapacity);
    }

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

        final BlockDeviceData data = floppy.getData(stack);

        if (!BlobStorage.isValidHandle(blobHandle)) {
            importFromItemStack(ItemStackUtils.getModDataTag(stack).getCompound(DATA_TAG_NAME));
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
                final ByteBufferBlockDevice medium = ByteBufferBlockDevice.createFromFileChannel(channel, capacity, false);
                if (isNew) {
                    if (data != null) {
                        FloppyMedia.image(medium, data.getBlockDevice());
                    } else {
                        FloppyMedia.format(medium);
                    }
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
        StorageItemUtils.setState(identity.getDiskItemStack(), StorageItemUtils.State.CORRUPTED);
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

}
