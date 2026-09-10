/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.bus.device.io;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * The arguments a guest wrote for an {@link IOCallback}, with correctly decoding readers.
 * <p>
 * Running out of bytes throws {@link EOFException}, which the guest sees as having called the
 * method with the wrong arguments. Reading through {@link InputStream#read()} still yields
 * {@code -1} instead, as usual.
 *
 * @see IOCallback
 * @see IOOutputStream
 */
public final class IOInputStream extends FilterInputStream {
    public IOInputStream(final InputStream stream) {
        super(stream);
    }

    // --------------------------------------------------------------------- //

    /**
     * Reads one byte.
     *
     * @return the value, {@code 0} to {@code 255}.
     * @throws EOFException if no byte is left.
     * @throws IOException  if reading fails.
     */
    public int readU8() throws IOException {
        final int value = in.read();
        if (value < 0) {
            throw new EOFException("Expected another byte of arguments.");
        }
        return value;
    }

    /**
     * Reads two bytes.
     *
     * @return the value, {@code 0} to {@code 65535}.
     * @throws EOFException if fewer than two bytes are left.
     * @throws IOException  if reading fails.
     */
    public int readU16() throws IOException {
        final int low = readU8();
        final int high = readU8();
        return low | (high << 8);
    }

    /**
     * Reads four bytes.
     *
     * @return the value, {@code 0} to {@code 4294967295}.
     * @throws EOFException if fewer than four bytes are left.
     * @throws IOException  if reading fails.
     */
    public long readU32() throws IOException {
        final int low = readU16();
        final int high = readU16();
        return (low | ((long) high << 16)) & 0xFFFFFFFFL;
    }

    /**
     * Reads an ASCII string.
     * <p>
     * This reads up to what comes first: a terminating {@code \0}, or the end of the stream.
     *
     * @return the text, empty if nothing is left.
     * @throws IOException if reading fails.
     */
    public String readString() throws IOException {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        for (int value = in.read(); value > 0; value = in.read()) {
            bytes.write(value);
        }
        return bytes.toString(StandardCharsets.US_ASCII);
    }
}
