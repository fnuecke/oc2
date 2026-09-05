/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.vm.device;

import li.cil.oc2.jcodec.common.model.ColorSpace;
import li.cil.oc2.jcodec.common.model.Picture;
import li.cil.oc2.jcodec.scale.RgbToYuv420j;
import li.cil.sedna.api.memory.MemoryAccessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

public final class SimpleFramebufferDeviceTests {
    private static final int WIDTH = 8;
    private static final int HEIGHT = 8;

    private ByteBuffer buffer;
    private SimpleFramebufferDevice device;
    private Picture picture;

    @BeforeEach
    public void setUp() {
        buffer = ByteBuffer.allocateDirect(WIDTH * HEIGHT * SimpleFramebufferDevice.STRIDE);
        device = new SimpleFramebufferDevice(WIDTH, HEIGHT, buffer);
        picture = Picture.create(WIDTH, HEIGHT, ColorSpace.YUV420J);
    }

    @Test
    public void startsFullyDirtyThenSettles() {
        assertTrue(device.hasChanges());
        assertTrue(device.applyChanges(picture));

        assertFalse(device.hasChanges());
        assertFalse(device.applyChanges(picture));
    }

    @Test
    public void storeMarksOnlyItsOwnLineDirty() throws MemoryAccessException {
        device.applyChanges(picture);

        device.store(offsetOf(5, 3), 0xFFFF, 1);

        assertTrue(device.hasChanges());
        assertTrue(device.applyChanges(picture));
        assertFalse(device.applyChanges(picture));
    }

    @Test
    public void loadReadsBackWhatStoreWrote() throws MemoryAccessException {
        device.store(offsetOf(0, 0), 0x1234, 1);
        device.store(offsetOf(7, 7), 0xABCD, 1);

        assertEquals(0x1234, device.load(offsetOf(0, 0), 1) & 0xFFFF);
        assertEquals(0xABCD, device.load(offsetOf(7, 7), 1) & 0xFFFF);
    }

    @Test
    public void outOfBoundsAccessIsIgnored() throws MemoryAccessException {
        final int past = WIDTH * HEIGHT * SimpleFramebufferDevice.STRIDE;

        device.store(past, 0xFFFF, 1);
        assertEquals(0, device.load(past, 1));
        assertEquals(0, device.load(-2, 1));
    }

    @Test
    public void conversionPlacesEveryPixel() throws MemoryAccessException {
        final int[][] colors = new int[HEIGHT][WIDTH];
        for (int row = 0; row < HEIGHT; row++) {
            for (int column = 0; column < WIDTH; column++) {
                final int level = (row * WIDTH + column) * 7 % 32;
                colors[row][column] = (level << 11) | ((level * 2) << 5) | level;
                device.store(offsetOf(column, row), colors[row][column], 1);
            }
        }

        assertTrue(device.applyChanges(picture));

        final byte[] luma = picture.getPlaneData(0);
        final int[] yuv = new int[3];
        for (int row = 0; row < HEIGHT; row++) {
            for (int column = 0; column < WIDTH; column++) {
                final int color = colors[row][column];
                final int r5 = (color >>> 11) & 0b11111;
                final int g6 = (color >>> 5) & 0b111111;
                final int b5 = color & 0b11111;
                RgbToYuv420j.rgb2yuv((byte) ((r5 * 255 / 0b11111) - 128),
                    (byte) ((g6 * 255 / 0b111111) - 128),
                    (byte) ((b5 * 255 / 0b11111) - 128), yuv);

                assertEquals((byte) yuv[0], luma[row * WIDTH + column],
                    "wrong luma at column " + column + ", row " + row);
            }
        }
    }

    @Test
    public void closedDeviceReportsNoChanges() {
        device.close();

        assertEquals(0, device.getLength());
        assertFalse(device.hasChanges());
        assertFalse(device.applyChanges(picture));
    }

    // --------------------------------------------------------------------- //

    private static int offsetOf(final int column, final int row) {
        return (row * WIDTH + column) * SimpleFramebufferDevice.STRIDE;
    }
}
