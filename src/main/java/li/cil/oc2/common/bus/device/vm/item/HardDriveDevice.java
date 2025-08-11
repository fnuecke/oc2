/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.vm.item;

import li.cil.oc2.common.config.AsyncConfig;
import li.cil.oc2.common.serialization.BlobStorage;
import li.cil.oc2.common.util.BlockLocation;
import li.cil.oc2.common.util.SoundEvents;
import li.cil.oc2.common.util.ThrottledSoundEmitter;
import li.cil.sedna.device.block.ByteBufferBlockDevice;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;

public class HardDriveDevice extends AbstractBlockStorageDevice<ByteBufferBlockDevice, ItemStack> {
    private final int size;
    private final ThrottledSoundEmitter soundEmitter;

    ///////////////////////////////////////////////////////////////////

    public HardDriveDevice(final ItemStack identity, final int size, final boolean readonly, final Supplier<Optional<BlockLocation>> location) {
        super(identity, readonly);
        this.size = size;
        this.soundEmitter = new ThrottledSoundEmitter(location, SoundEvents.HDD_ACCESS.get())
            .withMinInterval(Duration.ofSeconds(1));
    }

    ///////////////////////////////////////////////////////////////////

    private static final Logger LOGGER = LogManager.getLogger();
    
    @Override
    protected CompletableFuture<ByteBufferBlockDevice> createBlockDevice() {
        blobHandle = BlobStorage.validateHandle(blobHandle);
        
        if (AsyncConfig.SERVER.asyncStorageOperations.get()) {
            return BlobStorage.getOrOpenAsync(blobHandle)
                .thenApplyAsync(channel -> {
                    try {
                        if (AsyncConfig.SERVER.enableSuperDebug.get()) {
                            LOGGER.debug("Mapping buffer for blob: {}", blobHandle);
                        }
                        final MappedByteBuffer buffer = channel.map(MapMode.READ_WRITE, 0, size);
                        return ByteBufferBlockDevice.wrap(buffer, readonly);
                    } catch (final IOException e) {
                        LOGGER.error("Failed to map buffer for blob: " + blobHandle, e);
                        throw new CompletionException(e);
                    }
                }, WORKERS);
        } else {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    final FileChannel channel = BlobStorage.getOrOpen(blobHandle);
                    final MappedByteBuffer buffer = channel.map(MapMode.READ_WRITE, 0, size);
                    return ByteBufferBlockDevice.wrap(buffer, readonly);
                } catch (final IOException e) {
                    LOGGER.error("Failed to create block device", e);
                    throw new CompletionException("Failed to create block device", e);
                }
            }, WORKERS);
        }
    }

    @Override
    protected void handleDataAccess() {
        soundEmitter.play();
    }
}
