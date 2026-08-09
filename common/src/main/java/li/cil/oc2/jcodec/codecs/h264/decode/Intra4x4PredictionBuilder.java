/* SPDX-License-Identifier: BSD 2-Clause "Simplified" License */

package li.cil.oc2.jcodec.codecs.h264.decode;

import li.cil.oc2.jcodec.common.tools.MathUtil;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * <p>
 * Builds intra prediction for intra 4x4 coded macroblocks
 *
 * @author The JCodec project
 */
public final class Intra4x4PredictionBuilder {
    public static void predictWithMode(final int mode, final int[] residual, final boolean leftAvailable, final boolean topAvailable,
                                       final boolean topRightAvailable, final byte[] leftRow, final byte[] topLine, final byte[] topLeft, final int mbOffX, final int blkX, final int blkY,
                                       final byte[] pixOut) {
        switch (mode) {
            case 0 -> predictVertical(residual, topLine, mbOffX, blkX, blkY, pixOut);
            case 1 -> predictHorizontal(residual, leftRow, blkX, blkY, pixOut);
            case 2 -> predictDC(residual, leftAvailable, topAvailable, leftRow, topLine, mbOffX, blkX, blkY, pixOut);
            case 3 -> predictDiagonalDownLeft(residual, topRightAvailable, topLine, mbOffX, blkX, blkY, pixOut);
            case 4 -> predictDiagonalDownRight(residual, leftRow, topLine, topLeft, mbOffX, blkX, blkY, pixOut);
            case 5 -> predictVerticalRight(residual, leftRow, topLine, topLeft, mbOffX, blkX, blkY, pixOut);
            case 6 -> predictHorizontalDown(residual, leftRow, topLine, topLeft, mbOffX, blkX, blkY, pixOut);
            case 7 -> predictVerticalLeft(residual, topRightAvailable, topLine, mbOffX, blkX, blkY, pixOut);
            case 8 -> predictHorizontalUp(residual, leftRow, blkX, blkY, pixOut);
        }

        final int oo1 = mbOffX + blkX;
        final int off1 = (blkY << 4) + blkX + 3;

        topLeft[blkY >> 2] = topLine[oo1 + 3];

        leftRow[blkY] = pixOut[off1];
        leftRow[blkY + 1] = pixOut[off1 + 16];
        leftRow[blkY + 2] = pixOut[off1 + 32];
        leftRow[blkY + 3] = pixOut[off1 + 48];

        final int off2 = (blkY << 4) + blkX + 48;
        topLine[oo1] = pixOut[off2];
        topLine[oo1 + 1] = pixOut[off2 + 1];
        topLine[oo1 + 2] = pixOut[off2 + 2];
        topLine[oo1 + 3] = pixOut[off2 + 3];
    }

    public static boolean lumaPred(final int predType, final boolean hasLeft, final boolean hasTop, final boolean hasTr, final byte[] predLeft,
                                   final byte[] predTop, final byte predTopLeft, final int blkX, final int blkY, final byte[] pred) {
        return switch (predType) {
            case 0 -> lumaPredVertical(hasTop, predTop, blkX, pred);
            case 1 -> lumaPredHorizontal(hasLeft, predLeft, blkY, pred);
            default -> lumaPredDC(hasLeft, hasTop, predLeft, predTop, blkX, blkY, pred);
            case 3 -> lumaPredDiagonalDownLeft(hasTop, hasTr, predTop, blkX, pred);
            case 4 -> lumaPredDiagonalDownRight(hasLeft, hasTop, predLeft, predTop, predTopLeft, blkX, blkY, pred);
            case 5 -> lumaPredVerticalRight(hasLeft, hasTop, predLeft, predTop, predTopLeft, blkX, blkY, pred);
            case 6 -> lumaPredHorizontalDown(hasLeft, hasTop, predLeft, predTop, predTopLeft, blkX, blkY, pred);
            case 7 -> lumaPredVerticalLeft(hasTop, hasTr, predTop, blkX, pred);
            case 8 -> lumaPredHorizontalUp(hasLeft, predLeft, blkY, pred);
        };
    }

    public static boolean available(final int predType, final boolean hasLeft, final boolean hasTop) {
        return switch (predType) {
            case 0 -> hasTop;
            case 1 -> hasLeft;
            default -> true;
            case 3 -> hasTop;
            case 4 -> hasLeft && hasTop;
            case 5 -> hasLeft && hasTop;
            case 6 -> hasLeft && hasTop;
            case 7 -> hasTop;
            case 8 -> hasLeft;
        };
    }

    private static boolean lumaPredVertical(final boolean topAvailable, final byte[] predTop,
                                            final int blkX, final byte[] resi) {
        for (int j = 0, rOff = 0; j < 4; ++j, rOff += 4) {
            resi[rOff] = predTop[blkX];
            resi[rOff + 1] = predTop[blkX + 1];
            resi[rOff + 2] = predTop[blkX + 2];
            resi[rOff + 3] = predTop[blkX + 3];
        }
        return topAvailable;
    }

    public static void predictVertical(final int[] residual, final byte[] topLine, final int mbOffX, final int blkX,
                                       final int blkY, final byte[] pixOut) {

        int pixOff = (blkY << 4) + blkX;
        final int toff = mbOffX + blkX;
        int rOff = 0;
        for (int j = 0; j < 4; ++j) {
            pixOut[pixOff] = (byte) MathUtil.clip(residual[rOff] + topLine[toff], -128, 127);
            pixOut[pixOff + 1] = (byte) MathUtil.clip(residual[rOff + 1] + topLine[toff + 1], -128, 127);
            pixOut[pixOff + 2] = (byte) MathUtil.clip(residual[rOff + 2] + topLine[toff + 2], -128, 127);
            pixOut[pixOff + 3] = (byte) MathUtil.clip(residual[rOff + 3] + topLine[toff + 3], -128, 127);
            rOff += 4;
            pixOff += 16;
        }
    }

    public static void predictHorizontal(final int[] residual, final byte[] leftRow, final int blkX,
                                         final int blkY, final byte[] pixOut) {

        int pixOff = (blkY << 4) + blkX;
        int rOff = 0;
        for (int j = 0; j < 4; j++) {
            final int l = leftRow[blkY + j];
            pixOut[pixOff] = (byte) MathUtil.clip(residual[rOff] + l, -128, 127);
            pixOut[pixOff + 1] = (byte) MathUtil.clip(residual[rOff + 1] + l, -128, 127);
            pixOut[pixOff + 2] = (byte) MathUtil.clip(residual[rOff + 2] + l, -128, 127);
            pixOut[pixOff + 3] = (byte) MathUtil.clip(residual[rOff + 3] + l, -128, 127);
            rOff += 4;
            pixOff += 16;
        }
    }

    private static boolean lumaPredHorizontal(final boolean hasLeft, final byte[] predLeft,
                                              final int blkY, final byte[] resi) {
        for (int j = 0, rOff = 0; j < 4; ++j, rOff += 4) {
            resi[rOff] = predLeft[blkY + j];
            resi[rOff + 1] = predLeft[blkY + j];
            resi[rOff + 2] = predLeft[blkY + j];
            resi[rOff + 3] = predLeft[blkY + j];
        }
        return hasLeft;
    }

    public static void predictDC(final int[] residual, final boolean leftAvailable, final boolean topAvailable, final byte[] leftRow,
                                 final byte[] topLine, final int mbOffX, final int blkX, final int blkY, final byte[] pixOut) {

        final int val = calcDC(leftAvailable, topAvailable, leftRow, topLine, mbOffX, blkX, blkY);

        int pixOff = (blkY << 4) + blkX;
        int rOff = 0;
        for (int j = 0; j < 4; j++) {
            pixOut[pixOff] = (byte) MathUtil.clip(residual[rOff] + val, -128, 127);
            pixOut[pixOff + 1] = (byte) MathUtil.clip(residual[rOff + 1] + val, -128, 127);
            pixOut[pixOff + 2] = (byte) MathUtil.clip(residual[rOff + 2] + val, -128, 127);
            pixOut[pixOff + 3] = (byte) MathUtil.clip(residual[rOff + 3] + val, -128, 127);
            pixOff += 16;
            rOff += 4;
        }
    }

    private static int calcDC(final boolean leftAvailable, final boolean topAvailable, final byte[] leftRow, final byte[] topLine, final int mbOffX,
                              final int blkX, final int blkY) {
        final int val;
        if (leftAvailable && topAvailable) {
            val = (leftRow[blkY] + leftRow[blkY + 1] + leftRow[blkY + 2] + leftRow[blkY + 3] + topLine[mbOffX + blkX]
                + topLine[mbOffX + blkX + 1] + topLine[mbOffX + blkX + 2] + topLine[mbOffX + blkX + 3] + 4) >> 3;
        } else if (leftAvailable) {
            val = (leftRow[blkY] + leftRow[blkY + 1] + leftRow[blkY + 2] + leftRow[blkY + 3] + 2) >> 2;
        } else if (topAvailable) {
            val = (topLine[mbOffX + blkX] + topLine[mbOffX + blkX + 1] + topLine[mbOffX + blkX + 2]
                + topLine[mbOffX + blkX + 3] + 2) >> 2;
        } else {
            val = 0;
        }
        return val;
    }

    private static boolean lumaPredDC(final boolean hasLeft, final boolean hasTop, final byte[] predLeft, final byte[] predTop,
                                      final int blkX, final int blkY, final byte[] resi) {
        final int val = calcDC(hasLeft, hasTop, predLeft, predTop, 0, blkX, blkY);

        for (int j = 0, rOff = 0; j < 4; ++j, rOff += 4) {
            resi[rOff] = (byte) val;
            resi[rOff + 1] = (byte) val;
            resi[rOff + 2] = (byte) val;
            resi[rOff + 3] = (byte) val;
        }

        return true;
    }

    public static void predictDiagonalDownLeft(final int[] residual, final boolean topRightAvailable,
                                               final byte[] topLine, final int mbOffX, final int blkX, final int blkY, final byte[] pixOut) {
        final int to = mbOffX + blkX;
        int tr0 = topLine[to + 3], tr1 = topLine[to + 3], tr2 = topLine[to + 3], tr3 = topLine[to + 3];
        if (topRightAvailable) {
            tr0 = topLine[to + 4];
            tr1 = topLine[to + 5];
            tr2 = topLine[to + 6];
            tr3 = topLine[to + 7];
        }

        final int c0 = ((topLine[to] + topLine[to + 2] + (topLine[to + 1] << 1) + 2) >> 2);
        final int c1 = ((topLine[to + 1] + topLine[to + 3] + (topLine[to + 2] << 1) + 2) >> 2);
        final int c2 = ((topLine[to + 2] + tr0 + (topLine[to + 3] << 1) + 2) >> 2);
        final int c3 = ((topLine[to + 3] + tr1 + (tr0 << 1) + 2) >> 2);
        final int c4 = ((tr0 + tr2 + (tr1 << 1) + 2) >> 2);
        final int c5 = ((tr1 + tr3 + (tr2 << 1) + 2) >> 2);
        final int c6 = ((tr2 + 3 * (tr3) + 2) >> 2);

        final int off = (blkY << 4) + blkX;
        pixOut[off] = (byte) MathUtil.clip(residual[0] + c0, -128, 127);
        pixOut[off + 1] = (byte) MathUtil.clip(residual[1] + c1, -128, 127);
        pixOut[off + 2] = (byte) MathUtil.clip(residual[2] + c2, -128, 127);
        pixOut[off + 3] = (byte) MathUtil.clip(residual[3] + c3, -128, 127);

        pixOut[off + 16] = (byte) MathUtil.clip(residual[4] + c1, -128, 127);
        pixOut[off + 17] = (byte) MathUtil.clip(residual[5] + c2, -128, 127);
        pixOut[off + 18] = (byte) MathUtil.clip(residual[6] + c3, -128, 127);
        pixOut[off + 19] = (byte) MathUtil.clip(residual[7] + c4, -128, 127);

        pixOut[off + 32] = (byte) MathUtil.clip(residual[8] + c2, -128, 127);
        pixOut[off + 33] = (byte) MathUtil.clip(residual[9] + c3, -128, 127);
        pixOut[off + 34] = (byte) MathUtil.clip(residual[10] + c4, -128, 127);
        pixOut[off + 35] = (byte) MathUtil.clip(residual[11] + c5, -128, 127);

        pixOut[off + 48] = (byte) MathUtil.clip(residual[12] + c3, -128, 127);
        pixOut[off + 49] = (byte) MathUtil.clip(residual[13] + c4, -128, 127);
        pixOut[off + 50] = (byte) MathUtil.clip(residual[14] + c5, -128, 127);
        pixOut[off + 51] = (byte) MathUtil.clip(residual[15] + c6, -128, 127);
    }

    private static boolean lumaPredDiagonalDownLeft(final boolean hasTop, final boolean hasTr,
                                                    final byte[] topLine, final int blkX, final byte[] resi) {
        final int to = blkX;
        int tr0 = topLine[to + 3], tr1 = topLine[to + 3], tr2 = topLine[to + 3], tr3 = topLine[to + 3];
        if (hasTr) {
            tr0 = topLine[to + 4];
            tr1 = topLine[to + 5];
            tr2 = topLine[to + 6];
            tr3 = topLine[to + 7];
        }

        final int c0 = ((topLine[to] + topLine[to + 2] + (topLine[to + 1] << 1) + 2) >> 2);
        final int c1 = ((topLine[to + 1] + topLine[to + 3] + (topLine[to + 2] << 1) + 2) >> 2);
        final int c2 = ((topLine[to + 2] + tr0 + (topLine[to + 3] << 1) + 2) >> 2);
        final int c3 = ((topLine[to + 3] + tr1 + (tr0 << 1) + 2) >> 2);
        final int c4 = ((tr0 + tr2 + (tr1 << 1) + 2) >> 2);
        final int c5 = ((tr1 + tr3 + (tr2 << 1) + 2) >> 2);
        final int c6 = ((tr2 + 3 * (tr3) + 2) >> 2);

        resi[0] = (byte) c0;
        resi[1] = (byte) c1;
        resi[2] = (byte) c2;
        resi[3] = (byte) c3;
        resi[4] = (byte) c1;
        resi[5] = (byte) c2;
        resi[6] = (byte) c3;
        resi[7] = (byte) c4;
        resi[8] = (byte) c2;
        resi[9] = (byte) c3;
        resi[10] = (byte) c4;
        resi[11] = (byte) c5;
        resi[12] = (byte) c3;
        resi[13] = (byte) c4;
        resi[14] = (byte) c5;
        resi[15] = (byte) c6;

        return hasTop;
    }

    public static void predictDiagonalDownRight(final int[] residual, final byte[] leftRow,
                                                final byte[] topLine, final byte[] topLeft,
                                                final int mbOffX, final int blkX, final int blkY, final byte[] pixOut) {

        final int off = (blkY << 4) + blkX;
        final int c0 = ((topLine[mbOffX + blkX] + 2 * topLeft[blkY >> 2] + leftRow[blkY] + 2) >> 2);

        final int c1 = ((topLeft[blkY >> 2] + (topLine[mbOffX + blkX] << 1) + topLine[mbOffX + blkX + 1] + 2) >> 2);
        final int c2 = ((topLine[mbOffX + blkX] + (topLine[mbOffX + blkX + 1] << 1) + topLine[mbOffX + blkX + 2] + 2) >> 2);
        final int c3 = ((topLine[mbOffX + blkX + 1] + (topLine[mbOffX + blkX + 2] << 1) + topLine[mbOffX + blkX + 3]
            + 2) >> 2);

        pixOut[off] = (byte) MathUtil.clip(residual[0] + c0, -128, 127);
        pixOut[off + 1] = (byte) MathUtil.clip(residual[1] + c1, -128, 127);
        pixOut[off + 2] = (byte) MathUtil.clip(residual[2] + c2, -128, 127);
        pixOut[off + 3] = (byte) MathUtil.clip(residual[3] + c3, -128, 127);

        final int c4 = ((topLeft[blkY >> 2] + (leftRow[blkY] << 1) + leftRow[blkY + 1] + 2) >> 2);
        final int c6 = ((topLeft[blkY >> 2] + (topLine[mbOffX + blkX] << 1) + topLine[mbOffX + blkX + 1] + 2) >> 2);
        final int c7 = ((topLine[mbOffX + blkX] + (topLine[mbOffX + blkX + 1] << 1) + topLine[mbOffX + blkX + 2] + 2) >> 2);

        pixOut[off + 16] = (byte) MathUtil.clip(residual[4] + c4, -128, 127);
        pixOut[off + 17] = (byte) MathUtil.clip(residual[5] + c0, -128, 127);
        pixOut[off + 18] = (byte) MathUtil.clip(residual[6] + c6, -128, 127);
        pixOut[off + 19] = (byte) MathUtil.clip(residual[7] + c7, -128, 127);

        final int c8 = ((leftRow[blkY] + (leftRow[blkY + 1] << 1) + leftRow[blkY + 2] + 2) >> 2);
        final int c9 = ((topLeft[blkY >> 2] + (leftRow[blkY] << 1) + leftRow[blkY + 1] + 2) >> 2);
        final int c11 = ((topLeft[blkY >> 2] + (topLine[mbOffX + blkX] << 1) + topLine[mbOffX + blkX + 1] + 2) >> 2);

        pixOut[off + 32] = (byte) MathUtil.clip(residual[8] + c8, -128, 127);
        pixOut[off + 33] = (byte) MathUtil.clip(residual[9] + c9, -128, 127);
        pixOut[off + 34] = (byte) MathUtil.clip(residual[10] + c0, -128, 127);
        pixOut[off + 35] = (byte) MathUtil.clip(residual[11] + c11, -128, 127);

        final int c12 = ((leftRow[blkY + 1] + (leftRow[blkY + 2] << 1) + leftRow[blkY + 3] + 2) >> 2);
        final int c13 = ((leftRow[blkY] + (leftRow[blkY + 1] << 1) + leftRow[blkY + 2] + 2) >> 2);
        final int c14 = ((topLeft[blkY >> 2] + (leftRow[blkY] << 1) + leftRow[blkY + 1] + 2) >> 2);

        pixOut[off + 48] = (byte) MathUtil.clip(residual[12] + c12, -128, 127);
        pixOut[off + 49] = (byte) MathUtil.clip(residual[13] + c13, -128, 127);
        pixOut[off + 50] = (byte) MathUtil.clip(residual[14] + c14, -128, 127);
        pixOut[off + 51] = (byte) MathUtil.clip(residual[15] + c0, -128, 127);
    }

    private static boolean lumaPredDiagonalDownRight(final boolean hasLeft, final boolean hasTop, final byte[] leftRow, final byte[] topLine,
                                                     final byte topLeft, final int blkX, final int blkY, final byte[] resi) {
        final int c0 = ((topLine[blkX] + 2 * topLeft + leftRow[blkY] + 2) >> 2);

        final int c1 = ((topLeft + (topLine[blkX] << 1) + topLine[blkX + 1] + 2) >> 2);
        final int c2 = ((topLine[blkX] + (topLine[blkX + 1] << 1) + topLine[blkX + 2] + 2) >> 2);
        final int c3 = ((topLine[blkX + 1] + (topLine[blkX + 2] << 1) + topLine[blkX + 3] + 2) >> 2);
        final int c4 = ((topLeft + (leftRow[blkY] << 1) + leftRow[blkY + 1] + 2) >> 2);
        final int c6 = ((topLeft + (topLine[blkX] << 1) + topLine[blkX + 1] + 2) >> 2);
        final int c7 = ((topLine[blkX] + (topLine[blkX + 1] << 1) + topLine[blkX + 2] + 2) >> 2);
        final int c8 = ((leftRow[blkY] + (leftRow[blkY + 1] << 1) + leftRow[blkY + 2] + 2) >> 2);
        final int c9 = ((topLeft + (leftRow[blkY] << 1) + leftRow[blkY + 1] + 2) >> 2);
        final int c11 = ((topLeft + (topLine[blkX] << 1) + topLine[blkX + 1] + 2) >> 2);
        final int c12 = ((leftRow[blkY + 1] + (leftRow[blkY + 2] << 1) + leftRow[blkY + 3] + 2) >> 2);
        final int c13 = ((leftRow[blkY] + (leftRow[blkY + 1] << 1) + leftRow[blkY + 2] + 2) >> 2);
        final int c14 = ((topLeft + (leftRow[blkY] << 1) + leftRow[blkY + 1] + 2) >> 2);

        resi[0] = (byte) c0;
        resi[1] = (byte) c1;
        resi[2] = (byte) c2;
        resi[3] = (byte) c3;
        resi[4] = (byte) c4;
        resi[5] = (byte) c0;
        resi[6] = (byte) c6;
        resi[7] = (byte) c7;
        resi[8] = (byte) c8;
        resi[9] = (byte) c9;
        resi[10] = (byte) c0;
        resi[11] = (byte) c11;
        resi[12] = (byte) c12;
        resi[13] = (byte) c13;
        resi[14] = (byte) c14;
        resi[15] = (byte) c0;

        return hasLeft && hasTop;
    }

    public static void predictVerticalRight(final int[] residual, final byte[] leftRow,
                                            final byte[] topLine, final byte[] topLeft,
                                            final int mbOffX, final int blkX, final int blkY, final byte[] pixOut) {

        final int v1 = (topLeft[blkY >> 2] + topLine[mbOffX + blkX] + 1) >> 1;
        final int v2 = (topLine[mbOffX + blkX] + topLine[mbOffX + blkX + 1] + 1) >> 1;
        final int v3 = (topLine[mbOffX + blkX + 1] + topLine[mbOffX + blkX + 2] + 1) >> 1;
        final int v4 = (topLine[mbOffX + blkX + 2] + topLine[mbOffX + blkX + 3] + 1) >> 1;
        final int v5 = (leftRow[blkY] + 2 * topLeft[blkY >> 2] + topLine[mbOffX + blkX] + 2) >> 2;
        final int v6 = (topLeft[blkY >> 2] + 2 * topLine[mbOffX + blkX] + topLine[mbOffX + blkX + 1] + 2) >> 2;
        final int v7 = (topLine[mbOffX + blkX] + 2 * topLine[mbOffX + blkX + 1] + topLine[mbOffX + blkX + 2] + 2) >> 2;
        final int v8 = (topLine[mbOffX + blkX + 1] + 2 * topLine[mbOffX + blkX + 2] + topLine[mbOffX + blkX + 3] + 2) >> 2;
        final int v9 = (topLeft[blkY >> 2] + 2 * leftRow[blkY] + leftRow[blkY + 1] + 2) >> 2;
        final int v10 = (leftRow[blkY] + 2 * leftRow[blkY + 1] + leftRow[blkY + 2] + 2) >> 2;

        final int off = (blkY << 4) + blkX;
        pixOut[off] = (byte) MathUtil.clip(residual[0] + v1, -128, 127);
        pixOut[off + 1] = (byte) MathUtil.clip(residual[1] + v2, -128, 127);
        pixOut[off + 2] = (byte) MathUtil.clip(residual[2] + v3, -128, 127);
        pixOut[off + 3] = (byte) MathUtil.clip(residual[3] + v4, -128, 127);
        pixOut[off + 16] = (byte) MathUtil.clip(residual[4] + v5, -128, 127);
        pixOut[off + 17] = (byte) MathUtil.clip(residual[5] + v6, -128, 127);
        pixOut[off + 18] = (byte) MathUtil.clip(residual[6] + v7, -128, 127);
        pixOut[off + 19] = (byte) MathUtil.clip(residual[7] + v8, -128, 127);
        pixOut[off + 32] = (byte) MathUtil.clip(residual[8] + v9, -128, 127);
        pixOut[off + 33] = (byte) MathUtil.clip(residual[9] + v1, -128, 127);
        pixOut[off + 34] = (byte) MathUtil.clip(residual[10] + v2, -128, 127);
        pixOut[off + 35] = (byte) MathUtil.clip(residual[11] + v3, -128, 127);
        pixOut[off + 48] = (byte) MathUtil.clip(residual[12] + v10, -128, 127);
        pixOut[off + 49] = (byte) MathUtil.clip(residual[13] + v5, -128, 127);
        pixOut[off + 50] = (byte) MathUtil.clip(residual[14] + v6, -128, 127);
        pixOut[off + 51] = (byte) MathUtil.clip(residual[15] + v7, -128, 127);
    }

    private static boolean lumaPredVerticalRight(final boolean hasLeft, final boolean hasTop, final byte[] predLeft, final byte[] predTop,
                                                 final byte predTopLeft, final int blkX, final int blkY, final byte[] resi) {
        final int v1 = (predTopLeft + predTop[blkX] + 1) >> 1;
        final int v2 = (predTop[blkX] + predTop[blkX + 1] + 1) >> 1;
        final int v3 = (predTop[blkX + 1] + predTop[blkX + 2] + 1) >> 1;
        final int v4 = (predTop[blkX + 2] + predTop[blkX + 3] + 1) >> 1;
        final int v5 = (predLeft[blkY] + 2 * predTopLeft + predTop[blkX] + 2) >> 2;
        final int v6 = (predTopLeft + 2 * predTop[blkX] + predTop[blkX + 1] + 2) >> 2;
        final int v7 = (predTop[blkX] + 2 * predTop[blkX + 1] + predTop[blkX + 2] + 2) >> 2;
        final int v8 = (predTop[blkX + 1] + 2 * predTop[blkX + 2] + predTop[blkX + 3] + 2) >> 2;
        final int v9 = (predTopLeft + 2 * predLeft[blkY] + predLeft[blkY + 1] + 2) >> 2;
        final int v10 = (predLeft[blkY] + 2 * predLeft[blkY + 1] + predLeft[blkY + 2] + 2) >> 2;

        resi[0] = (byte) v1;
        resi[1] = (byte) v2;
        resi[2] = (byte) v3;
        resi[3] = (byte) v4;
        resi[4] = (byte) v5;
        resi[5] = (byte) v6;
        resi[6] = (byte) v7;
        resi[7] = (byte) v8;
        resi[8] = (byte) v9;
        resi[9] = (byte) v1;
        resi[10] = (byte) v2;
        resi[11] = (byte) v3;
        resi[12] = (byte) v10;
        resi[13] = (byte) v5;
        resi[14] = (byte) v6;
        resi[15] = (byte) v7;

        return hasLeft && hasTop;
    }

    public static void predictHorizontalDown(final int[] residual,
                                             final byte[] leftRow, final byte[] topLine, final byte[] topLeft, final int mbOffX, final int blkX, final int blkY, final byte[] pixOut) {

        final int c0 = (topLeft[blkY >> 2] + leftRow[blkY] + 1) >> 1;
        final int c1 = (leftRow[blkY] + 2 * topLeft[blkY >> 2] + topLine[mbOffX + blkX] + 2) >> 2;
        final int c2 = (topLeft[blkY >> 2] + 2 * topLine[mbOffX + blkX] + topLine[mbOffX + blkX + 1] + 2) >> 2;
        final int c3 = (topLine[mbOffX + blkX] + 2 * topLine[mbOffX + blkX + 1] + topLine[mbOffX + blkX + 2] + 2) >> 2;
        final int c4 = (leftRow[blkY] + leftRow[blkY + 1] + 1) >> 1;
        final int c5 = (topLeft[blkY >> 2] + 2 * leftRow[blkY] + leftRow[blkY + 1] + 2) >> 2;
        final int c6 = (leftRow[blkY + 1] + leftRow[blkY + 2] + 1) >> 1;
        final int c7 = (leftRow[blkY] + 2 * leftRow[blkY + 1] + leftRow[blkY + 2] + 2) >> 2;
        final int c8 = (leftRow[blkY + 2] + leftRow[blkY + 3] + 1) >> 1;
        final int c9 = (leftRow[blkY + 1] + 2 * leftRow[blkY + 2] + leftRow[blkY + 3] + 2) >> 2;

        final int off = (blkY << 4) + blkX;
        pixOut[off] = (byte) MathUtil.clip(residual[0] + c0, -128, 127);
        pixOut[off + 1] = (byte) MathUtil.clip(residual[1] + c1, -128, 127);
        pixOut[off + 2] = (byte) MathUtil.clip(residual[2] + c2, -128, 127);
        pixOut[off + 3] = (byte) MathUtil.clip(residual[3] + c3, -128, 127);
        pixOut[off + 16] = (byte) MathUtil.clip(residual[4] + c4, -128, 127);
        pixOut[off + 17] = (byte) MathUtil.clip(residual[5] + c5, -128, 127);
        pixOut[off + 18] = (byte) MathUtil.clip(residual[6] + c0, -128, 127);
        pixOut[off + 19] = (byte) MathUtil.clip(residual[7] + c1, -128, 127);
        pixOut[off + 32] = (byte) MathUtil.clip(residual[8] + c6, -128, 127);
        pixOut[off + 33] = (byte) MathUtil.clip(residual[9] + c7, -128, 127);
        pixOut[off + 34] = (byte) MathUtil.clip(residual[10] + c4, -128, 127);
        pixOut[off + 35] = (byte) MathUtil.clip(residual[11] + c5, -128, 127);
        pixOut[off + 48] = (byte) MathUtil.clip(residual[12] + c8, -128, 127);
        pixOut[off + 49] = (byte) MathUtil.clip(residual[13] + c9, -128, 127);
        pixOut[off + 50] = (byte) MathUtil.clip(residual[14] + c6, -128, 127);
        pixOut[off + 51] = (byte) MathUtil.clip(residual[15] + c7, -128, 127);
    }

    private static boolean lumaPredHorizontalDown(final boolean hasLeft, final boolean hasTop, final byte[] predLeft, final byte[] predTop,
                                                  final byte predTopLeft, final int blkX, final int blkY, final byte[] resi) {
        final int c0 = (predTopLeft + predLeft[blkY] + 1) >> 1;
        final int c1 = (predLeft[blkY] + 2 * predTopLeft + predTop[blkX] + 2) >> 2;
        final int c2 = (predTopLeft + 2 * predTop[blkX] + predTop[blkX + 1] + 2) >> 2;
        final int c3 = (predTop[blkX] + 2 * predTop[blkX + 1] + predTop[blkX + 2] + 2) >> 2;
        final int c4 = (predLeft[blkY] + predLeft[blkY + 1] + 1) >> 1;
        final int c5 = (predTopLeft + 2 * predLeft[blkY] + predLeft[blkY + 1] + 2) >> 2;
        final int c6 = (predLeft[blkY + 1] + predLeft[blkY + 2] + 1) >> 1;
        final int c7 = (predLeft[blkY] + 2 * predLeft[blkY + 1] + predLeft[blkY + 2] + 2) >> 2;
        final int c8 = (predLeft[blkY + 2] + predLeft[blkY + 3] + 1) >> 1;
        final int c9 = (predLeft[blkY + 1] + 2 * predLeft[blkY + 2] + predLeft[blkY + 3] + 2) >> 2;

        resi[0] = (byte) c0;
        resi[1] = (byte) c1;
        resi[2] = (byte) c2;
        resi[3] = (byte) c3;
        resi[4] = (byte) c4;
        resi[5] = (byte) c5;
        resi[6] = (byte) c0;
        resi[7] = (byte) c1;
        resi[8] = (byte) c6;
        resi[9] = (byte) c7;
        resi[10] = (byte) c4;
        resi[11] = (byte) c5;
        resi[12] = (byte) c8;
        resi[13] = (byte) c9;
        resi[14] = (byte) c6;
        resi[15] = (byte) c7;

        return hasTop && hasLeft;
    }

    public static void predictVerticalLeft(final int[] residual, final boolean topRightAvailable,
                                           final byte[] topLine, final int mbOffX, final int blkX, final int blkY, final byte[] pixOut) {

        final int to = mbOffX + blkX;
        int tr0 = topLine[to + 3], tr1 = topLine[to + 3], tr2 = topLine[to + 3];
        if (topRightAvailable) {
            tr0 = topLine[to + 4];
            tr1 = topLine[to + 5];
            tr2 = topLine[to + 6];
        }

        final int c0 = ((topLine[to] + topLine[to + 1] + 1) >> 1);
        final int c1 = ((topLine[to + 1] + topLine[to + 2] + 1) >> 1);
        final int c2 = ((topLine[to + 2] + topLine[to + 3] + 1) >> 1);
        final int c3 = ((topLine[to + 3] + tr0 + 1) >> 1);
        final int c4 = ((tr0 + tr1 + 1) >> 1);
        final int c5 = ((topLine[to] + 2 * topLine[to + 1] + topLine[to + 2] + 2) >> 2);
        final int c6 = ((topLine[to + 1] + 2 * topLine[to + 2] + topLine[to + 3] + 2) >> 2);
        final int c7 = ((topLine[to + 2] + 2 * topLine[to + 3] + tr0 + 2) >> 2);
        final int c8 = ((topLine[to + 3] + 2 * tr0 + tr1 + 2) >> 2);
        final int c9 = ((tr0 + 2 * tr1 + tr2 + 2) >> 2);

        final int off = (blkY << 4) + blkX;
        pixOut[off] = (byte) MathUtil.clip(residual[0] + c0, -128, 127);
        pixOut[off + 1] = (byte) MathUtil.clip(residual[1] + c1, -128, 127);
        pixOut[off + 2] = (byte) MathUtil.clip(residual[2] + c2, -128, 127);
        pixOut[off + 3] = (byte) MathUtil.clip(residual[3] + c3, -128, 127);

        pixOut[off + 16] = (byte) MathUtil.clip(residual[4] + c5, -128, 127);
        pixOut[off + 17] = (byte) MathUtil.clip(residual[5] + c6, -128, 127);
        pixOut[off + 18] = (byte) MathUtil.clip(residual[6] + c7, -128, 127);
        pixOut[off + 19] = (byte) MathUtil.clip(residual[7] + c8, -128, 127);

        pixOut[off + 32] = (byte) MathUtil.clip(residual[8] + c1, -128, 127);
        pixOut[off + 33] = (byte) MathUtil.clip(residual[9] + c2, -128, 127);
        pixOut[off + 34] = (byte) MathUtil.clip(residual[10] + c3, -128, 127);
        pixOut[off + 35] = (byte) MathUtil.clip(residual[11] + c4, -128, 127);

        pixOut[off + 48] = (byte) MathUtil.clip(residual[12] + c6, -128, 127);
        pixOut[off + 49] = (byte) MathUtil.clip(residual[13] + c7, -128, 127);
        pixOut[off + 50] = (byte) MathUtil.clip(residual[14] + c8, -128, 127);
        pixOut[off + 51] = (byte) MathUtil.clip(residual[15] + c9, -128, 127);
    }

    private static boolean lumaPredVerticalLeft(final boolean hasTop, final boolean hasTr,
                                                final byte[] predTop, final int blkX, final byte[] resi) {
        final int to = blkX;
        int tr0 = predTop[to + 3], tr1 = predTop[to + 3], tr2 = predTop[to + 3];
        if (hasTr) {
            tr0 = predTop[to + 4];
            tr1 = predTop[to + 5];
            tr2 = predTop[to + 6];
        }

        final int c0 = ((predTop[to] + predTop[to + 1] + 1) >> 1);
        final int c1 = ((predTop[to + 1] + predTop[to + 2] + 1) >> 1);
        final int c2 = ((predTop[to + 2] + predTop[to + 3] + 1) >> 1);
        final int c3 = ((predTop[to + 3] + tr0 + 1) >> 1);
        final int c4 = ((tr0 + tr1 + 1) >> 1);
        final int c5 = ((predTop[to] + 2 * predTop[to + 1] + predTop[to + 2] + 2) >> 2);
        final int c6 = ((predTop[to + 1] + 2 * predTop[to + 2] + predTop[to + 3] + 2) >> 2);
        final int c7 = ((predTop[to + 2] + 2 * predTop[to + 3] + tr0 + 2) >> 2);
        final int c8 = ((predTop[to + 3] + 2 * tr0 + tr1 + 2) >> 2);
        final int c9 = ((tr0 + 2 * tr1 + tr2 + 2) >> 2);

        resi[0] = (byte) c0;
        resi[1] = (byte) c1;
        resi[2] = (byte) c2;
        resi[3] = (byte) c3;
        resi[4] = (byte) c5;
        resi[5] = (byte) c6;
        resi[6] = (byte) c7;
        resi[7] = (byte) c8;
        resi[8] = (byte) c1;
        resi[9] = (byte) c2;
        resi[10] = (byte) c3;
        resi[11] = (byte) c4;
        resi[12] = (byte) c6;
        resi[13] = (byte) c7;
        resi[14] = (byte) c8;
        resi[15] = (byte) c9;

        return hasTop;
    }

    public static void predictHorizontalUp(final int[] residual, final byte[] leftRow, final int blkX,
                                           final int blkY, final byte[] pixOut) {

        final int c0 = ((leftRow[blkY] + leftRow[blkY + 1] + 1) >> 1);
        final int c1 = ((leftRow[blkY] + (leftRow[blkY + 1] << 1) + leftRow[blkY + 2] + 2) >> 2);
        final int c2 = ((leftRow[blkY + 1] + leftRow[blkY + 2] + 1) >> 1);
        final int c3 = ((leftRow[blkY + 1] + (leftRow[blkY + 2] << 1) + leftRow[blkY + 3] + 2) >> 2);
        final int c4 = ((leftRow[blkY + 2] + leftRow[blkY + 3] + 1) >> 1);
        final int c5 = ((leftRow[blkY + 2] + (leftRow[blkY + 3] << 1) + leftRow[blkY + 3] + 2) >> 2);
        final int c6 = leftRow[blkY + 3];

        final int off = (blkY << 4) + blkX;
        pixOut[off] = (byte) MathUtil.clip(residual[0] + c0, -128, 127);
        pixOut[off + 1] = (byte) MathUtil.clip(residual[1] + c1, -128, 127);
        pixOut[off + 2] = (byte) MathUtil.clip(residual[2] + c2, -128, 127);
        pixOut[off + 3] = (byte) MathUtil.clip(residual[3] + c3, -128, 127);

        pixOut[off + 16] = (byte) MathUtil.clip(residual[4] + c2, -128, 127);
        pixOut[off + 17] = (byte) MathUtil.clip(residual[5] + c3, -128, 127);
        pixOut[off + 18] = (byte) MathUtil.clip(residual[6] + c4, -128, 127);
        pixOut[off + 19] = (byte) MathUtil.clip(residual[7] + c5, -128, 127);

        pixOut[off + 32] = (byte) MathUtil.clip(residual[8] + c4, -128, 127);
        pixOut[off + 33] = (byte) MathUtil.clip(residual[9] + c5, -128, 127);
        pixOut[off + 34] = (byte) MathUtil.clip(residual[10] + c6, -128, 127);
        pixOut[off + 35] = (byte) MathUtil.clip(residual[11] + c6, -128, 127);

        pixOut[off + 48] = (byte) MathUtil.clip(residual[12] + c6, -128, 127);
        pixOut[off + 49] = (byte) MathUtil.clip(residual[13] + c6, -128, 127);
        pixOut[off + 50] = (byte) MathUtil.clip(residual[14] + c6, -128, 127);
        pixOut[off + 51] = (byte) MathUtil.clip(residual[15] + c6, -128, 127);
    }

    private static boolean lumaPredHorizontalUp(final boolean hasLeft, final byte[] predLeft,
                                                final int blkY, final byte[] resi) {
        final int c0 = ((predLeft[blkY] + predLeft[blkY + 1] + 1) >> 1);
        final int c1 = ((predLeft[blkY] + (predLeft[blkY + 1] << 1) + predLeft[blkY + 2] + 2) >> 2);
        final int c2 = ((predLeft[blkY + 1] + predLeft[blkY + 2] + 1) >> 1);
        final int c3 = ((predLeft[blkY + 1] + (predLeft[blkY + 2] << 1) + predLeft[blkY + 3] + 2) >> 2);
        final int c4 = ((predLeft[blkY + 2] + predLeft[blkY + 3] + 1) >> 1);
        final int c5 = ((predLeft[blkY + 2] + (predLeft[blkY + 3] << 1) + predLeft[blkY + 3] + 2) >> 2);
        final int c6 = predLeft[blkY + 3];

        resi[0] = (byte) c0;
        resi[1] = (byte) c1;
        resi[2] = (byte) c2;
        resi[3] = (byte) c3;
        resi[4] = (byte) c2;
        resi[5] = (byte) c3;
        resi[6] = (byte) c4;
        resi[7] = (byte) c5;
        resi[8] = (byte) c4;
        resi[9] = (byte) c5;
        resi[10] = (byte) c6;
        resi[11] = (byte) c6;
        resi[12] = (byte) c6;
        resi[13] = (byte) c6;
        resi[14] = (byte) c6;
        resi[15] = (byte) c6;

        return hasLeft;
    }
}
