/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.vm.item;

import li.cil.sedna.api.device.BlockDevice;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

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
        if (blob.isValid()) {
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

        final FileChannel channel = blob.open();
        try {
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
        } catch (final IOException e) {
            blob.release();
            throw e;
        }

        return buffer;
    }
}
