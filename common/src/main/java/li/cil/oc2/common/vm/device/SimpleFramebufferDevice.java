/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.vm.device;

import li.cil.oc2.jcodec.common.model.Picture;
import li.cil.oc2.jcodec.scale.RgbToYuv420j;
import li.cil.sedna.api.device.MemoryMappedDevice;
import li.cil.sedna.api.memory.MemoryAccessException;
import li.cil.sedna.utils.DirectByteBufferUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.BitSet;

public final class SimpleFramebufferDevice implements MemoryMappedDevice {
    public static final int STRIDE = 2;

    // --------------------------------------------------------------------- //

    private final int width, height;
    private final ByteBuffer buffer;
    private int length;
    private final BitSet dirtyLines;
    private volatile boolean hasDirtyLines;
    private final BitSet snapshotLines;
    private final ByteBuffer snapshot;
    private final int[][] conversionBuffer = new int[4][3];

    // --------------------------------------------------------------------- //

    public SimpleFramebufferDevice(final int width, final int height, final ByteBuffer buffer) {
        this.width = width;
        this.height = height;
        this.length = width * height * STRIDE;

        if (buffer.capacity() < length) {
            throw new IllegalArgumentException("Buffer too small.");
        }

        this.buffer = buffer.order(ByteOrder.LITTLE_ENDIAN);
        this.dirtyLines = new BitSet(height / 2);
        this.dirtyLines.set(0, height / 2);
        this.hasDirtyLines = true;
        this.snapshotLines = new BitSet(height / 2);
        this.snapshot = ByteBuffer.allocate(length).order(ByteOrder.LITTLE_ENDIAN);
    }

    // --------------------------------------------------------------------- //

    public void close() {
        synchronized (buffer) {
            length = 0;
            dirtyLines.clear();
            hasDirtyLines = false;
            DirectByteBufferUtils.release(buffer);
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean hasChanges() {
        return hasDirtyLines;
    }

    public boolean applyChanges(final Picture picture) {
        synchronized (buffer) {
            snapshotLines.clear();
            snapshotLines.or(dirtyLines);
            if (snapshotLines.isEmpty()) {
                return false;
            }

            dirtyLines.clear();
            hasDirtyLines = false;

            final int halfRowLength = width * STRIDE * 2;
            for (int halfRow = snapshotLines.nextSetBit(0); halfRow >= 0; halfRow = snapshotLines.nextSetBit(halfRow + 1)) {
                final int offset = halfRow * halfRowLength;
                snapshot.put(offset, buffer, offset, halfRowLength);
            }
        }

        final byte[][] pictureData = picture.getData();
        for (int halfRow = snapshotLines.nextSetBit(0); halfRow >= 0; halfRow = snapshotLines.nextSetBit(halfRow + 1)) {
            convertHalfRow(halfRow, pictureData);
        }

        return true;
    }

    @Override
    public int getLength() {
        return length;
    }

    @Override
    public long load(final int offset, final int sizeLog2) throws MemoryAccessException {
        synchronized (buffer) {
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
    }

    @Override
    public void store(final int offset, final long value, final int sizeLog2) throws MemoryAccessException {
        synchronized (buffer) {
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
    }

    // --------------------------------------------------------------------- //

    private static void r5g6b5ToYuv420(final int r5g6b5, final int[] yuv) {
        final int r5 = (r5g6b5 >>> 11) & 0b11111;
        final int g6 = (r5g6b5 >>> 5) & 0b111111;
        final int b5 = r5g6b5 & 0b11111;
        final byte r = (byte) ((r5 * 255 / 0b11111) - 128);
        final byte g = (byte) ((g6 * 255 / 0b111111) - 128);
        final byte b = (byte) ((b5 * 255 / 0b11111) - 128);
        RgbToYuv420j.rgb2yuv(r, g, b, yuv);

    }

    private void convertHalfRow(final int halfRow, final byte[][] pictureData) {
        final int[][] quadrant = conversionBuffer;
        final int row = halfRow * 2;
        int bufferIndex = row * width * STRIDE, lumaIndex = row * width, chromaIndex = halfRow * (width / 2);
        for (int halfCol = 0; halfCol < width / 2; halfCol++, bufferIndex += STRIDE * 2, lumaIndex += 2, chromaIndex++) {
            r5g6b5ToYuv420(snapshot.getShort(bufferIndex) & 0xFFFF, quadrant[0]);
            pictureData[0][lumaIndex] = (byte) quadrant[0][0];

            r5g6b5ToYuv420(snapshot.getShort(bufferIndex + STRIDE) & 0xFFFF, quadrant[1]);
            pictureData[0][lumaIndex + 1] = (byte) quadrant[1][0];

            r5g6b5ToYuv420(snapshot.getShort(bufferIndex + width * STRIDE) & 0xFFFF, quadrant[2]);
            pictureData[0][lumaIndex + width] = (byte) quadrant[2][0];

            r5g6b5ToYuv420(snapshot.getShort(bufferIndex + width * STRIDE + STRIDE) & 0xFFFF, quadrant[3]);
            pictureData[0][lumaIndex + width + 1] = (byte) quadrant[3][0];

            // Average chroma values for quadrant.
            pictureData[1][chromaIndex] = (byte) ((quadrant[0][1] + quadrant[1][1] + quadrant[2][1] + quadrant[3][1] + 2) >> 2);
            pictureData[2][chromaIndex] = (byte) ((quadrant[0][2] + quadrant[1][2] + quadrant[2][2] + quadrant[3][2] + 2) >> 2);
        }
    }

    private void setDirty(final int offset) {
        final int pixelY = offset / (width * STRIDE);
        dirtyLines.set(pixelY / 2);
        if (!hasDirtyLines) {
            hasDirtyLines = true;
        }
    }
}
