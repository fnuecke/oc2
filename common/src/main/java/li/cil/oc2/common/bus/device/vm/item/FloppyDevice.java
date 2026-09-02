/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.vm.item;

import li.cil.oc2.api.bus.device.data.BlockDeviceData;
import li.cil.oc2.api.bus.device.vm.ArchitectureType;
import li.cil.oc2.api.bus.device.vm.context.VMContext;
import li.cil.oc2.common.serialization.BlobStorage;
import li.cil.oc2.common.util.BlockLocation;
import li.cil.sedna.device.block.ByteBufferBlockDevice;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public final class FloppyDevice extends HardDriveDevice {
    private final ArchitectureType architectureType;
    @Nullable
    private final BlockDeviceData base;

    // --------------------------------------------------------------------- //

    public FloppyDevice(final ItemStack identity, final int size, final ArchitectureType architectureType,
                        @Nullable final BlockDeviceData base, final Supplier<Optional<BlockLocation>> location) {
        super(identity, size, false, location);
        this.architectureType = architectureType;
        this.base = base;
    }

    // --------------------------------------------------------------------- //

    @Override
    protected MappedStorage createStorage(final VMContext context) {
        return switch (architectureType) {
            case RISCV -> super.createStorage(context);
            case Z80 -> new FloppyControllerStorage();
        };
    }

    @Override
    protected CompletableFuture<ByteBufferBlockDevice> createBlockDevice() throws IOException {
        final boolean isInitializing = !BlobStorage.isValidHandle(blobHandle);
        final CompletableFuture<ByteBufferBlockDevice> future = super.createBlockDevice();
        if (!isInitializing) {
            return future;
        }

        return future.thenApplyAsync(medium -> {
            try {
                if (base != null) {
                    FloppyMedia.image(medium, base.getBlockDevice());
                } else {
                    FloppyMedia.format(medium);
                }
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
            return medium;
        }, WORKERS);
    }
}
