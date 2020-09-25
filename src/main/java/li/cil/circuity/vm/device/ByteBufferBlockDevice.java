package li.cil.circuity.vm.device;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public final class ByteBufferBlockDevice implements BlockDevice {
    private final int capacity;
    private final ByteBuffer data;
    private final boolean readonly;

    public static ByteBufferBlockDevice create(final int size, final boolean readonly) {
        return new ByteBufferBlockDevice(ByteBuffer.allocate(size), readonly);
    }

    public static ByteBufferBlockDevice createFromFile(final File file, final boolean readonly) throws IOException {
        return createFromFile(file, file.length(), readonly);
    }

    public static ByteBufferBlockDevice createFromFile(final File file, final long length, final boolean readonly) throws IOException {
        final String fileMode = readonly ? "r" : "rw";
        final RandomAccessFile mappedFile = new RandomAccessFile(file, fileMode);
        final FileChannel.MapMode mapMode = readonly ? FileChannel.MapMode.READ_ONLY : FileChannel.MapMode.READ_WRITE;
        final MappedByteBuffer buffer = mappedFile.getChannel().map(mapMode, 0, length);
        return new ByteBufferBlockDevice(buffer, readonly);
    }

    public ByteBufferBlockDevice(final ByteBuffer data, final boolean readonly) {
        this.capacity = data.remaining();
        this.data = ByteBuffer.allocate(capacity);
        this.data.put(data);
        this.readonly = readonly;
    }

    @Override
    public boolean isReadonly() {
        return readonly;
    }

    @Override
    public long getCapacity() {
        return capacity;
    }

    @Override
    public ByteBuffer getView(final long offset, final int length) {
        if (offset < 0 || offset > Integer.MAX_VALUE) {
            throw new IllegalArgumentException();
        }

        data.limit((int) offset + length);
        data.position((int) offset);
        final ByteBuffer slice = data.slice();
        if (readonly) {
            return slice.asReadOnlyBuffer();
        } else {
            return slice;
        }
    }
}
