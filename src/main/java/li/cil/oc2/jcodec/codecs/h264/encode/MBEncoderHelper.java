/* SPDX-License-Identifier: BSD 2-Clause "Simplified" License */

package li.cil.oc2.jcodec.codecs.h264.encode;

import li.cil.oc2.jcodec.common.model.Picture;
import li.cil.oc2.jcodec.common.tools.MathUtil;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
public final class MBEncoderHelper {
    public static void takeSubtract(final byte[] planeData, final int planeWidth, final int planeHeight,
                                    final int x, final int y, final int[] coeff,
                                    final byte[] pred, final int blkW, final int blkH) {
        if (x + blkW < planeWidth && y + blkH < planeHeight)
            takeSubtractSafe(planeData, planeWidth, x, y, coeff, pred, blkW, blkH);
        else
            takeSubtractUnsafe(planeData, planeWidth, planeHeight, x, y, coeff, pred, blkW, blkH);
    }

    public static void takeSubtractSafe(final byte[] planeData, final int planeWidth, final int x, final int y,
                                        final int[] coeff, final byte[] pred, final int blkW, final int blkH) {
        for (int i = 0, srcOff = y * planeWidth + x, dstOff = 0; i < blkH; i++, srcOff += planeWidth) {
            for (int j = 0, srcOff1 = srcOff; j < blkW; j += 4, dstOff += 4, srcOff1 += 4) {
                coeff[dstOff] = planeData[srcOff1] - pred[dstOff];
                coeff[dstOff + 1] = planeData[srcOff1 + 1] - pred[dstOff + 1];
                coeff[dstOff + 2] = planeData[srcOff1 + 2] - pred[dstOff + 2];
                coeff[dstOff + 3] = planeData[srcOff1 + 3] - pred[dstOff + 3];
            }
        }
    }

    public static void take(final byte[] planeData, final int planeWidth, final int planeHeight,
                            final int x, final int y, final byte[] patch,
                            final int blkW, final int blkH) {
        if (x + blkW < planeWidth && y + blkH < planeHeight)
            takeSafe(planeData, planeWidth, x, y, patch, blkW, blkH);
        else
            takeExtendBorder(planeData, planeWidth, planeHeight, x, y, patch, blkW, blkH);
    }

    public static void takeSafe(final byte[] planeData, final int planeWidth,
                                final int x, final int y, final byte[] patch,
                                final int blkW, final int blkH) {
        for (int i = 0, srcOff = y * planeWidth + x, dstOff = 0; i < blkH; i++, srcOff += planeWidth) {
            for (int j = 0, srcOff1 = srcOff; j < blkW; ++j, ++dstOff, ++srcOff1) {
                patch[dstOff] = planeData[srcOff1];
            }
        }
    }

    public static void takeExtendBorder(final byte[] planeData, final int planeWidth, final int planeHeight,
                                        final int x, final int y, final byte[] patch,
                                        final int blkW, final int blkH) {
        int outOff = 0;

        int i;
        for (i = y; i < Math.min(y + blkH, planeHeight); i++) {
            int off = i * planeWidth + Math.min(x, planeWidth);
            int j;
            for (j = x; j < Math.min(x + blkW, planeWidth); j++, outOff++, off++) {
                patch[outOff] = planeData[off];
            }
            --off;
            for (; j < x + blkW; j++, outOff++) {
                patch[outOff] = planeData[off];
            }
        }
        for (; i < y + blkH; i++) {
            int off = planeHeight * planeWidth - planeWidth + Math.min(x, planeWidth);
            int j;
            for (j = x; j < Math.min(x + blkW, planeWidth); j++, outOff++, off++) {
                patch[outOff] = planeData[off];
            }
            --off;
            for (; j < x + blkW; j++, outOff++) {
                patch[outOff] = planeData[off];
            }
        }
    }

    public static void takeSubtractUnsafe(final byte[] planeData, final int planeWidth, final int planeHeight,
                                          final int x, final int y, final int[] coeff, final byte[] pred,
                                          final int blkW, final int blkH) {
        int outOff = 0;

        int i;
        for (i = y; i < Math.min(y + blkH, planeHeight); i++) {
            int off = i * planeWidth + Math.min(x, planeWidth);
            int j;
            for (j = x; j < Math.min(x + blkW, planeWidth); j++, outOff++, off++) {
                coeff[outOff] = planeData[off] - pred[outOff];
            }
            --off;
            for (; j < x + blkW; j++, outOff++) {
                coeff[outOff] = planeData[off] - pred[outOff];
            }
        }
        for (; i < y + blkH; i++) {
            int off = planeHeight * planeWidth - planeWidth + Math.min(x, planeWidth);
            int j;
            for (j = x; j < Math.min(x + blkW, planeWidth); j++, outOff++, off++) {
                coeff[outOff] = planeData[off] - pred[outOff];
            }
            --off;
            for (; j < x + blkW; j++, outOff++) {
                coeff[outOff] = planeData[off] - pred[outOff];
            }
        }
    }

    public static void putBlk(final byte[] planeData, final int[] block, final byte[] pred, final int log2stride,
                              final int blkX, final int blkY, final int blkW, final int blkH) {
        final int stride = 1 << log2stride;
        for (int line = 0, srcOff = 0, dstOff = (blkY << log2stride) + blkX; line < blkH; line++) {
            int dstOff1 = dstOff;
            for (int row = 0; row < blkW; row += 4) {
                planeData[dstOff1] = (byte) MathUtil.clip(block[srcOff] + pred[srcOff], -128, 127);
                planeData[dstOff1 + 1] = (byte) MathUtil.clip(block[srcOff + 1] + pred[srcOff + 1], -128, 127);
                planeData[dstOff1 + 2] = (byte) MathUtil.clip(block[srcOff + 2] + pred[srcOff + 2], -128, 127);
                planeData[dstOff1 + 3] = (byte) MathUtil.clip(block[srcOff + 3] + pred[srcOff + 3], -128, 127);
                srcOff += 4;
                dstOff1 += 4;
            }
            dstOff += stride;
        }
    }

    public static void putBlkPic(final Picture dest, final Picture src, final int x, final int y) {
        if (dest.getColor() != src.getColor())
            throw new RuntimeException("Incompatible color");
        for (int c = 0; c < dest.getColor().nComp; c++) {
            pubBlkOnePlane(dest.getPlaneData(c), dest.getPlaneWidth(c), src.getPlaneData(c), src.getPlaneWidth(c),
                src.getPlaneHeight(c), x >> dest.getColor().compWidth[c], y >> dest.getColor().compHeight[c]);
        }
    }

    private static void pubBlkOnePlane(final byte[] dest, final int destWidth, final byte[] src,
                                       final int srcWidth, final int srcHeight, final int x, final int y) {
        int destOff = y * destWidth + x;
        int srcOff = 0;
        for (int i = 0; i < srcHeight; i++) {
            for (int j = 0; j < srcWidth; j++, ++destOff, ++srcOff)
                dest[destOff] = src[srcOff];
            destOff += destWidth - srcWidth;
        }
    }
}
