/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.vm.item;

import com.google.common.io.ByteStreams;
import li.cil.sedna.api.device.BlockDevice;
import li.cil.sedna.device.block.ByteBufferBlockDevice;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

public final class FloppyMedia {
    private static final byte EMPTY_DIRECTORY_ENTRY = (byte) 0xE5; // what CP/M reads as an unused entry

    // --------------------------------------------------------------------- //

    public static void image(final ByteBufferBlockDevice medium, final BlockDevice base) throws IOException {
        try (InputStream input = base.getInputStream(0);
             OutputStream output = medium.getOutputStream()) {
            ByteStreams.copy(input, output);
        }
    }

    public static void format(final ByteBufferBlockDevice medium) throws IOException {
        final byte[] empty = new byte[4096];
        Arrays.fill(empty, EMPTY_DIRECTORY_ENTRY);

        try (OutputStream stream = medium.getOutputStream()) {
            long remaining = medium.getCapacity();
            while (remaining > 0) {
                final int count = (int) Math.min(empty.length, remaining);
                stream.write(empty, 0, count);
                remaining -= count;
            }
        }
    }

    private FloppyMedia() {
    }
}
