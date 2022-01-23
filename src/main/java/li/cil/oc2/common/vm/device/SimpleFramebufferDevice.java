package li.cil.oc2.common.vm.device;

import li.cil.sedna.api.device.MemoryMappedDevice;
import li.cil.sedna.api.memory.MemoryAccessException;
import li.cil.sedna.utils.DirectByteBufferUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.BitSet;
import java.util.Optional;

public final class SimpleFramebufferDevice implements MemoryMappedDevice {
    public static final int STRIDE = 2;
    public static final int TILE_WIDTH = 32;
    public static final int TILE_SIZE = TILE_WIDTH * TILE_WIDTH;
    public static final int TILE_SIZE_IN_BYTES = TILE_SIZE * STRIDE;

    public record Tile(int startPixelX, int startPixelY, ByteBuffer data) {
        public void apply(final int width, final ByteBuffer buffer) {
            if (buffer.capacity() < TILE_SIZE * STRIDE) {
                throw new IllegalArgumentException();
            }

            final int startIndex = (startPixelX + startPixelY * width) * STRIDE;
            final int rowWidth = width * STRIDE;
            final int tileRowBytes = TILE_WIDTH * STRIDE;
            buffer.position(startIndex);
            for (int i = 0; i < TILE_WIDTH; i++) {
                buffer.slice(startIndex + i * rowWidth, tileRowBytes)
                    .put(data.slice(i * tileRowBytes, tileRowBytes));
            }
        }
    }

    ///////////////////////////////////////////////////////////////

    private final int width, height;
    private final ByteBuffer buffer;
    private int length;
    private final int tileCount;
    private final BitSet dirty;
    private int lastDirtyIndex;

    ///////////////////////////////////////////////////////////////

    public SimpleFramebufferDevice(final int width, final int height, final ByteBuffer buffer) {
        this.width = width;
        this.height = height;
        this.length = width * height * STRIDE;

        if (buffer.capacity() < length) {
            throw new IllegalArgumentException("Buffer too small.");
        }

        this.buffer = buffer.order(ByteOrder.LITTLE_ENDIAN);
        this.tileCount = width * height / TILE_SIZE;
        this.dirty = new BitSet(tileCount);
        setAllDirty();
    }

    ///////////////////////////////////////////////////////////////

    public void close() {
        length = 0;
        DirectByteBufferUtils.release(buffer);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void setAllDirty() {
        synchronized (dirty) {
            dirty.clear();
            dirty.flip(0, tileCount);
        }
    }

    public Optional<Tile> getNextDirtyTile() {
        final int index = dirty.nextSetBit(lastDirtyIndex);
        if (index < 0) {
            lastDirtyIndex = 0;
            return Optional.empty();
        }

        synchronized (dirty) {
            dirty.clear(index);
        }

        lastDirtyIndex = index + 1;
        if (lastDirtyIndex >= tileCount) {
            lastDirtyIndex = 0;
        }

        final int tileCountX = width / TILE_WIDTH;
        final int tileX = index % tileCountX;
        final int tileY = index / tileCountX;
        final int startPixelX = tileX * TILE_WIDTH;
        final int startPixelY = tileY * TILE_WIDTH;
        return Optional.of(new Tile(startPixelX, startPixelY, getTileData(startPixelX, startPixelY)));
    }

    @Override
    public int getLength() {
        return length;
    }

    @Override
    public long load(final int offset, final int sizeLog2) throws MemoryAccessException {
        if (offset >= 0 && offset <= length - (1 << sizeLog2)) {
            return switch (sizeLog2) {
                case 0 -> buffer.get(offset);
                case 1 -> buffer.getShort(offset);
                case 2 -> buffer.getInt(offset);
                case 3 -> buffer.getLong(offset);
                default -> throw new IllegalArgumentException();
            };
        } else {
            return 0;
        }
    }

    @Override
    public void store(final int offset, final long value, final int sizeLog2) throws MemoryAccessException {
        if (offset >= 0 && offset <= length - (1 << sizeLog2)) {
            switch (sizeLog2) {
                case 0 -> buffer.put(offset, (byte) value);
                case 1 -> buffer.putShort(offset, (short) value);
                case 2 -> buffer.putInt(offset, (int) value);
                case 3 -> buffer.putLong(offset, value);
                default -> throw new IllegalArgumentException();
            }
            setDirty(offset);
        }
    }

    ///////////////////////////////////////////////////////////////

    private int getTileIndex(final int offset) {
        final int pixelIndex = offset / STRIDE;
        final int tileCountX = width / TILE_WIDTH;
        final int pixelX = pixelIndex % width;
        final int pixelY = pixelIndex / width;
        final int tileX = pixelX / TILE_WIDTH;
        final int tileY = pixelY / TILE_WIDTH;
        return tileX + tileY * tileCountX;
    }

    private ByteBuffer getTileData(final int startPixelX, final int startPixelY) {
        final ByteBuffer result = ByteBuffer.allocate(TILE_SIZE_IN_BYTES).order(ByteOrder.LITTLE_ENDIAN);

        final int startIndex = (startPixelX + startPixelY * width) * STRIDE;
        final int rowWidth = width * STRIDE;
        for (int i = 0; i < TILE_WIDTH; i++) {
            result.put(buffer.slice(startIndex + i * rowWidth, TILE_WIDTH * 2));
        }

        result.flip();

        return result;
    }

    private void setDirty(final int offset) {
        final int tileIndex = getTileIndex(offset);
        if (!dirty.get(tileIndex)) {
            synchronized (dirty) {
                dirty.set(tileIndex);
            }
        }
    }
}
