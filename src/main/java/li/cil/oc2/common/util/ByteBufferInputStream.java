package li.cil.oc2.common.util;

import java.io.InputStream;
import java.nio.ByteBuffer;

public final class ByteBufferInputStream extends InputStream {
    private final ByteBuffer buffer;

    public ByteBufferInputStream(final ByteBuffer buffer) {
        this.buffer = buffer.slice();
    }

    @Override
    public int read() {
        if (!buffer.hasRemaining()) {
            return -1;
        }
        return buffer.get();
    }

    @Override
    public int read(final byte[] b, final int off, int len) {
        len = Math.min(len, buffer.remaining());
        if (len == 0) {
            return -1;
        }

        buffer.get(b, off, len);

        return len;
    }
}
