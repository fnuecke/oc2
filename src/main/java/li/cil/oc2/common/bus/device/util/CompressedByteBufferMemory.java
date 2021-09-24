package li.cil.oc2.common.bus.device.util;

import li.cil.sedna.api.device.PhysicalMemory;
import li.cil.sedna.api.memory.MemoryAccessException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class CompressedByteBufferMemory extends PhysicalMemory {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final boolean PROFILING = true;

    private final FileChannel file;
    private final ByteBuffer data;

    @NotNull
    private static ByteBuffer createBuffer(final FileChannel compressedFile, int size) {
        ByteBuffer buffer = ByteBuffer.allocate(size);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        try {
            if(compressedFile.size() == 0) return buffer; // The file is empty so we create an empty byte buffer.

            long startTime;
            if(PROFILING) startTime = System.nanoTime();

            // Read the compressed file into a byte buffer
            ByteBuffer compressedBuffer = ByteBuffer.allocate((int) compressedFile.size());
            compressedFile.read(compressedBuffer, 0);
            compressedBuffer.rewind();

            GZIPInputStream decompressor = new GZIPInputStream(new InputStream() {
                @Override
                public int read() {
                    if (!compressedBuffer.hasRemaining()) return -1;
                    int value = compressedBuffer.get();
                    return value >= 0 ? value : value+256;
                }
            });
            // Decompress the entire file.
            int count = 0;
            int n;
            while(size-count > 0 && (n = decompressor.read(buffer.array(), count, size-count)) != -1) count += n;
            if(count < size) LOGGER.warn("Could not fill the entire memory buffer with the provided compressed file.");

            if(PROFILING) {
                long endTime = System.nanoTime();
                LOGGER.info("Finished memory decompression, took " + ((endTime-startTime)/1000000f) + "ms.");
            }
        } catch (IOException e) {
            LOGGER.error(e);
        }
        return buffer;
    }

    private static void saveBuffer(final FileChannel compressedFile, final ByteBuffer buffer) {
        try {
            long startTime;
            if(PROFILING) startTime = System.nanoTime();

            // Compress and write memory to the file.
            compressedFile.position(0);
            GZIPOutputStream compressor = new GZIPOutputStream(new OutputStream() {
                final ByteBuffer buffer = ByteBuffer.allocateDirect(1024 * 1024);

                @Override
                public void write(final int b) throws IOException {
                    if(!this.buffer.hasRemaining()) this.flush();
                    this.buffer.put((byte)b);
                }

                @Override
                public void flush() throws IOException {
                    this.buffer.limit(this.buffer.position());
                    this.buffer.position(0);
                    compressedFile.write(this.buffer);
                    this.buffer.rewind();
                }

                @Override
                public void close() throws IOException {
                    this.flush();
                }
            });
            compressor.write(buffer.array(), 0, buffer.capacity());
            compressor.close();

            if(PROFILING) {
                long endTime = System.nanoTime();
                LOGGER.info("Finished memory compression, took " + ((endTime-startTime)/1000000f) + "ms.");
            }
        } catch (IOException e) {
            LOGGER.error(e);
        }
    }

    public CompressedByteBufferMemory(final FileChannel file, int size) throws IOException {
        this.file = file;
        this.data = createBuffer(this.file, size);
    }

    @Override
    public void close() throws Exception {
        saveBuffer(this.file, this.data);
    }

    @Override
    public int getLength() {
        return this.data.capacity();
    }

    /* Copied from li.cil.sedna.device.memory.ByteBufferMemory */

    @Override
    public long load(int offset, int sizeLog2) throws MemoryAccessException {
        if (offset >= 0 && offset <= this.getLength() - (1 << sizeLog2)) {
            switch(sizeLog2) {
                case 0:
                    return this.data.get(offset);
                case 1:
                    return this.data.getShort(offset);
                case 2:
                    return this.data.getInt(offset);
                case 3:
                    return this.data.getLong(offset);
                default:
                    throw new IllegalArgumentException();
            }
        } else {
            throw new MemoryAccessException();
        }
    }

    @Override
    public void store(int offset, long value, int sizeLog2) throws MemoryAccessException {
        if (offset >= 0 && offset <= this.getLength() - (1 << sizeLog2)) {
            switch(sizeLog2) {
                case 0:
                    this.data.put(offset, (byte)((int)value));
                    break;
                case 1:
                    this.data.putShort(offset, (short)((int)value));
                    break;
                case 2:
                    this.data.putInt(offset, (int)value);
                    break;
                case 3:
                    this.data.putLong(offset, value);
                    break;
                default:
                    throw new IllegalArgumentException();
            }

        } else {
            throw new MemoryAccessException();
        }
    }

    @Override
    public void load(int offset, ByteBuffer dst) throws MemoryAccessException {
        if (offset >= 0 && offset <= this.getLength() - dst.remaining()) {
            ByteBuffer slice = this.data.slice();
            slice.position(offset);
            slice.limit(offset + dst.remaining());
            dst.put(slice);
        } else {
            throw new MemoryAccessException();
        }
    }

    @Override
    public void store(int offset, ByteBuffer src) throws MemoryAccessException {
        if (offset >= 0 && offset <= this.getLength() - src.remaining()) {
            ByteBuffer slice = this.data.slice();
            slice.position(offset);
            slice.limit(offset + src.remaining());
            slice.put(src);
        } else {
            throw new MemoryAccessException();
        }
    }
}
