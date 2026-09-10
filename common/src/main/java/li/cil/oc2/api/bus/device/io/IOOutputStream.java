/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.bus.device.io;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * The results an {@link IOCallback} hands back to the guest, with correctly encoding writers.
 * <p>
 * At most {@link IOCallback#MAX_DATA_SIZE} bytes fit; writing more fails the call.
 *
 * @see IOCallback
 * @see IOInputStream
 */
public final class IOOutputStream extends FilterOutputStream {
    public IOOutputStream(final OutputStream stream) {
        super(stream);
    }

    // --------------------------------------------------------------------- //

    /**
     * Writes the low byte of the specified value.
     *
     * @param value the value to write.
     * @throws IOException if writing fails.
     */
    public void writeU8(final int value) throws IOException {
        out.write(value & 0xFF);
    }

    /**
     * Writes the low two bytes of the specified value.
     *
     * @param value the value to write.
     * @throws IOException if writing fails.
     */
    public void writeU16(final int value) throws IOException {
        writeU8(value);
        writeU8(value >>> 8);
    }

    /**
     * Writes the low four bytes of the specified value.
     *
     * @param value the value to write.
     * @throws IOException if writing fails.
     */
    public void writeU32(final long value) throws IOException {
        writeU16((int) value);
        writeU16((int) (value >>> 16));
    }

    /**
     * Writes the specified text as printable ASCII.
     * <p>
     * This does <em>not</em> write a terminator automatically. Write a {@code \0} when passing multiple strings.
     *
     * @param value the text to write.
     * @throws IOException if writing fails.
     */
    public void writeString(final String value) throws IOException {
        out.write(value.getBytes(StandardCharsets.US_ASCII));
    }
}
