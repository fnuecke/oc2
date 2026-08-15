/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.vm.item;

import com.google.common.io.ByteStreams;
import li.cil.oc2.common.serialization.BlobStorage;
import li.cil.oc2.common.util.BlockLocation;
import li.cil.sedna.api.device.BlockDevice;
import li.cil.sedna.device.block.ByteBufferBlockDevice;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public final class HardDriveDeviceWithInitialData extends HardDriveDevice {
    private final BlockDevice base;

    // ------------------------------------------------------------- //

    public HardDriveDeviceWithInitialData(final ItemStack identity, final BlockDevice base, final boolean readonly, final Supplier<Optional<BlockLocation>> location) {
        super(identity, (int) base.getCapacity(), readonly, location);
        this.base = base;
    }

    // ------------------------------------------------------------- //

    @Override
    protected CompletableFuture<ByteBufferBlockDevice> createBlockDevice() throws IOException {
        final boolean isInitializing = !BlobStorage.isValidHandle(blobHandle);
        final CompletableFuture<ByteBufferBlockDevice> future = super.createBlockDevice();
        if (!isInitializing) {
            return future;
        }

        return future.thenApplyAsync(device -> {
            try {
                try (InputStream input = base.getInputStream(0);
                     OutputStream output = device.getOutputStream(0)) {
                    ByteStreams.copy(input, output);
                }
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
            return device;
        }, WORKERS);
    }
}
