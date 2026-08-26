/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.vm.item;

import li.cil.oc2.common.serialization.BlobStorage;
import li.cil.oc2.common.util.*;
import li.cil.sedna.device.block.ByteBufferBlockDevice;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class HardDriveDevice extends AbstractBlockStorageDevice<ByteBufferBlockDevice, ItemStack> {
    private final int size;
    private final Supplier<Optional<BlockLocation>> location;
    private final ThrottledSoundEmitter soundEmitter;

    // --------------------------------------------------------------------- //

    public HardDriveDevice(final ItemStack identity, final int size, final boolean readonly, final Supplier<Optional<BlockLocation>> location) {
        super(identity, readonly);
        this.size = size;
        this.location = location;
        this.soundEmitter = new ThrottledSoundEmitter(location, SoundEvents.HDD_ACCESS.get())
            .withMinInterval(Duration.ofSeconds(1));
    }

    // --------------------------------------------------------------------- //

    @Override
    protected int getMappedByteCount() {
        return size;
    }

    @Override
    protected CompletableFuture<ByteBufferBlockDevice> createBlockDevice() throws IOException {
        final boolean isNew = !BlobStorage.isValidHandle(blobHandle);
        final UUID handle = isNew ? BlobStorage.allocateHandle() : blobHandle;

        // Make sure we can access the data now, so base can handle missing/in use errors.
        final FileChannel channel = BlobStorage.open(handle, isNew);

        blobHandle = handle;

        return CompletableFuture.supplyAsync(() -> {
            try {
                return ByteBufferBlockDevice.createFromFileChannel(channel, size, readonly);
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        }, WORKERS);
    }

    @Override
    protected void handleDataAccess() {
        soundEmitter.play();
    }

    @Override
    protected void handleDataUnavailable() {
        StorageItemUtils.setCorrupted(identity);
        location.get().ifPresent(blockLocation -> blockLocation.tryGetLevel().ifPresent(level ->
            ChunkUtils.setLazyUnsaved(level, blockLocation.blockPos())));
    }
}
