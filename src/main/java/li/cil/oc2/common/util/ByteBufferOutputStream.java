package li.cil.oc2.common.util;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

public final class ByteBufferOutputStream extends OutputStream {
    private final ByteBuffer buffer;

    public ByteBufferOutputStream(final ByteBuffer buffer) {
        this.buffer = buffer.slice();
    }

    @Override
    public void write(final int b) throws IOException {
        try {
            buffer.put((byte) b);
        } catch (final BufferOverflowException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        try {
            buffer.put(b, off, len);
        } catch (final BufferOverflowException e) {
            throw new IOException(e);
        }
    }
}
