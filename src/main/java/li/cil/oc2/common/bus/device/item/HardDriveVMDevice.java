/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.item;

import li.cil.oc2.common.serialization.BlobStorage;
import li.cil.oc2.common.util.Location;
import li.cil.oc2.common.util.SoundEvents;
import li.cil.oc2.common.util.ThrottledSoundEmitter;
import li.cil.sedna.device.block.ByteBufferBlockDevice;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class HardDriveVMDevice extends AbstractBlockDeviceVMDevice<ByteBufferBlockDevice, ItemStack> {
    private final int size;
    private final ThrottledSoundEmitter soundEmitter;

    ///////////////////////////////////////////////////////////////////

    public HardDriveVMDevice(final ItemStack identity, final int size, final boolean readonly, final Supplier<Optional<Location>> location) {
        super(identity, readonly);
        this.size = size;
        this.soundEmitter = new ThrottledSoundEmitter(location, SoundEvents.HDD_ACCESS.get())
            .withMinInterval(Duration.ofSeconds(1));
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected CompletableFuture<ByteBufferBlockDevice> createBlockDevice() {
        blobHandle = BlobStorage.validateHandle(blobHandle);

        return CompletableFuture.supplyAsync(() -> {
            try {
                final FileChannel channel = BlobStorage.getOrOpen(blobHandle);
                final MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, size);
                return ByteBufferBlockDevice.wrap(buffer, readonly);
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        }, WORKERS);
    }

    @Override
    protected void handleDataAccess() {
        soundEmitter.play();
    }
}
