/* SPDX-License-Identifier: BSD 2-Clause "Simplified" License */

package li.cil.oc2.jcodec.common.model;

import java.util.Arrays;

import static java.lang.System.arraycopy;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * <p>
 * The data is -128 shifted, so 0 is represented by -128 and 255 is represented
 * by +127
 *
 * @author The JCodec project
 */
public class Picture {
    private ColorSpace color;

    private final int width;
    private final int height;

    private final byte[][] data;

    public static Picture createPicture(final int width, final int height, final byte[][] data, final ColorSpace color) {
        return new Picture(width, height, data, color);
    }

    public Picture(final int width, final int height, final byte[][] data, final ColorSpace color) {
        this.width = width;
        this.height = height;
        this.data = data;
        this.color = color;

        if (color != null) {
            for (int i = 0; i < color.nComp; i++) {
                int mask = 0xff >> (8 - color.compWidth[i]);
                if ((width & mask) != 0)
                    throw new IllegalArgumentException("Component " + i + " width should be a multiple of " + (1 << color.compWidth[i]) + " for colorspace: " + color);
                mask = 0xff >> (8 - color.compHeight[i]);
                if ((height & mask) != 0)
                    throw new IllegalArgumentException("Component " + i + " height should be a multiple of " + (1 << color.compHeight[i]) + " for colorspace: " + color);
            }
        }
    }

    public static Picture copyPicture(final Picture other) {
        return new Picture(other.width, other.height, other.data, other.color);
    }

    public static Picture create(final int width, final int height, final ColorSpace colorSpace) {
        final int[] planeSizes = new int[ColorSpace.MAX_PLANES];
        for (int i = 0; i < colorSpace.nComp; i++) {
            planeSizes[colorSpace.compPlane[i]] += (width >> colorSpace.compWidth[i]) * (height >> colorSpace.compHeight[i]);
        }
        int nPlanes = 0;
        for (int i = 0; i < ColorSpace.MAX_PLANES; i++)
            nPlanes += planeSizes[i] != 0 ? 1 : 0;

        final byte[][] data1 = new byte[nPlanes][];
        for (int i = 0, plane = 0; i < ColorSpace.MAX_PLANES; i++) {
            if (planeSizes[i] != 0) {
                data1[plane++] = new byte[planeSizes[i]];
            }
        }
        return new Picture(width, height, data1, colorSpace);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public byte[] getPlaneData(final int plane) {
        return data[plane];
    }

    public ColorSpace getColor() {
        return color;
    }

    public void setColor(final ColorSpace color) {
        this.color = color;
    }

    public byte[][] getData() {
        return data;
    }

    public int getPlaneWidth(final int plane) {
        return width >> color.compWidth[plane];
    }

    public int getPlaneHeight(final int plane) {
        return height >> color.compHeight[plane];
    }

    public boolean compatible(final Picture src) {
        return src.color == color && src.width == width && src.height == height;
    }

    public Picture createCompatible() {
        return Picture.create(width, height, color);
    }

    public void copyFrom(final Picture src) {
        if (!compatible(src))
            throw new IllegalArgumentException("Can not copy to incompatible picture");
        for (int plane = 0; plane < color.nComp; plane++) {
            if (data[plane] == null)
                continue;
            arraycopy(src.data[plane], 0, data[plane], 0, (width >> color.compWidth[plane]) * (height >> color.compHeight[plane]));
        }
    }

    public void fill(final int val) {
        for (final byte[] plane : data) {
            Arrays.fill(plane, (byte) val);
        }
    }

    public Size getSize() {
        return new Size(width, height);
    }
}
