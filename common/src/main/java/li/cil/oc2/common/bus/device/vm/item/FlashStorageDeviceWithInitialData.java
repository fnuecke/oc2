/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.vm.item;

import li.cil.oc2.common.serialization.BlobStorage;
import li.cil.sedna.api.device.BlockDevice;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.UUID;

public final class FlashStorageDeviceWithInitialData extends FlashStorageDevice {
    private final BlockDevice base;

    // --------------------------------------------------------------------- //

    public FlashStorageDeviceWithInitialData(final ItemStack identity, final int size, final BlockDevice base) {
        super(identity, size);
        this.base = base;
    }

    // --------------------------------------------------------------------- //

    @Override
    @Nullable
    protected ByteBuffer readData() throws IOException {
        if (BlobStorage.isValidHandle(blobHandle)) {
            return super.readData();
        }

        if (base.getCapacity() > size) {
            throw new IOException("Bootloader image does not fit the flash memory.");
        }

        final ByteBuffer buffer = ByteBuffer.allocate(size);
        try (InputStream stream = base.getInputStream(0)) {
            buffer.put(stream.readAllBytes());
        }
        buffer.flip();

        final UUID handle = BlobStorage.allocateHandle();
        final FileChannel channel = BlobStorage.open(handle, true);
        try {
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
        } catch (final IOException e) {
            BlobStorage.close(handle);
            throw e;
        }

        blobHandle = handle;

        return buffer;
    }
}
