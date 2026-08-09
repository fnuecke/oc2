/* SPDX-License-Identifier: BSD 2-Clause "Simplified" License */

package li.cil.oc2.jcodec.codecs.h264.decode;

import li.cil.oc2.jcodec.common.ArrayUtil;
import li.cil.oc2.jcodec.common.tools.MathUtil;

import static li.cil.oc2.jcodec.codecs.h264.H264Const.LUMA_4x4_BLOCK_LUT;
import static li.cil.oc2.jcodec.codecs.h264.H264Const.LUMA_4x4_POS_LUT;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * <p>
 * Prediction builder class for intra 16x16 coded macroblocks
 *
 * @author The JCodec project
 */
public final class Intra16x16PredictionBuilder {
    public static void predictWithMode(final int predMode, final int[][] residual, final boolean leftAvailable, final boolean topAvailable,
                                       final byte[] leftRow, final byte[] topLine, final byte[] topLeft, final int x, final byte[] pixOut) {
        switch (predMode) {
            case 0 -> predictVertical(residual, topLine, x, pixOut);
            case 1 -> predictHorizontal(residual, leftRow, pixOut);
            case 2 -> predictDC(residual, leftAvailable, topAvailable, leftRow, topLine, x, pixOut);
            case 3 -> predictPlane(residual, leftRow, topLine, topLeft, x, pixOut);
        }
    }

    public static void lumaPred(final int predMode, final boolean leftAvailable, final boolean topAvailable, final byte[] leftRow,
                                final byte[] topLine, final byte topLeft, final int x, final byte[][] pred) {
        switch (predMode) {
            case 0 -> lumaVerticalPred(topLine, x, pred);
            case 1 -> lumaHorizontalPred(leftRow, pred);
            case 2 -> lumaDCPred(leftAvailable, topAvailable, leftRow, topLine, x, pred);
            case 3 -> lumaPlanePred(leftRow, topLine, topLeft, x, pred);
        }
    }

    public static int lumaPredSAD(final int predMode, final boolean leftAvailable, final boolean topAvailable, final byte[] leftRow,
                                  final byte[] topLine, final byte topLeft, final int x, final byte[] pred) {
        return switch (predMode) {
            case 0 -> lumaVerticalPredSAD(topAvailable, topLine, x, pred);
            case 1 -> lumaHorizontalPredSAD(leftAvailable, leftRow, pred);
            default -> lumaDCPredSAD(leftAvailable, topAvailable, leftRow, topLine, x, pred);
            case 3 -> lumaPlanePredSAD(leftAvailable, topAvailable, leftRow, topLine, topLeft, x, pred);
        };
    }

    public static void predictVertical(final int[][] residual, final byte[] topLine, final int x, final byte[] pixOut) {
        int off = 0;
        for (int j = 0; j < 16; j++) {
            for (int i = 0; i < 16; i++, off++)
                pixOut[off] = (byte) MathUtil.clip(residual[LUMA_4x4_BLOCK_LUT[off]][LUMA_4x4_POS_LUT[off]] + topLine[x + i],
                    -128, 127);
        }
    }

    public static void lumaVerticalPred(final byte[] topLine, final int x, final byte[][] pred) {
        int off = 0;
        for (int j = 0; j < 16; j++) {
            for (int i = 0; i < 16; i++, off++)
                pred[LUMA_4x4_BLOCK_LUT[off]][LUMA_4x4_POS_LUT[off]] = topLine[x + i];
        }
    }

    public static int lumaVerticalPredSAD(final boolean topAvailable, final byte[] topLine, final int x, final byte[] pred) {
        if (!topAvailable)
            return Integer.MAX_VALUE;
        int sad = 0;
        for (int j = 0; j < 16; j++) {
            for (int i = 0; i < 16; i++)
                sad += MathUtil.abs(pred[(j << 4) + i] - topLine[x + i]);
        }
        return sad;
    }

    public static void predictHorizontal(final int[][] residual, final byte[] leftRow, final byte[] pixOut) {
        int off = 0;
        for (int j = 0; j < 16; j++) {
            for (int i = 0; i < 16; i++, off++)
                pixOut[off] = (byte) MathUtil.clip(residual[LUMA_4x4_BLOCK_LUT[off]][LUMA_4x4_POS_LUT[off]] + leftRow[j], -128,
                    127);
        }
    }

    public static void lumaHorizontalPred(final byte[] leftRow, final byte[][] pred) {
        int off = 0;
        for (int j = 0; j < 16; j++) {
            for (int i = 0; i < 16; i++, off++)
                pred[LUMA_4x4_BLOCK_LUT[off]][LUMA_4x4_POS_LUT[off]] = leftRow[j];
        }
    }

    public static int lumaHorizontalPredSAD(final boolean leftAvailable, final byte[] leftRow, final byte[] pred) {
        if (!leftAvailable)
            return Integer.MAX_VALUE;
        int sad = 0;
        for (int j = 0; j < 16; j++) {
            for (int i = 0; i < 16; i++)
                sad += MathUtil.abs(pred[(j << 4) + i] - leftRow[j]);
        }
        return sad;
    }

    public static void predictDC(final int[][] residual, final boolean leftAvailable, final boolean topAvailable, final byte[] leftRow,
                                 final byte[] topLine, final int x, final byte[] pixOut) {
        final int s0 = getDC(leftAvailable, topAvailable, leftRow, topLine, x);

        for (int i = 0; i < 256; i++)
            pixOut[i] = (byte) MathUtil.clip(residual[LUMA_4x4_BLOCK_LUT[i]][LUMA_4x4_POS_LUT[i]] + s0, -128, 127);
    }

    public static void lumaDCPred(final boolean leftAvailable, final boolean topAvailable, final byte[] leftRow, final byte[] topLine, final int x,
                                  final byte[][] pred) {
        final int s0 = getDC(leftAvailable, topAvailable, leftRow, topLine, x);

        for (int i = 0; i < pred.length; i++)
            for (int j = 0; j < pred[i].length; j++)
                pred[i][j] += s0;
    }

    public static int lumaDCPredSAD(final boolean leftAvailable, final boolean topAvailable, final byte[] leftRow, final byte[] topLine, final int x,
                                    final byte[] pred) {
        final int s0 = getDC(leftAvailable, topAvailable, leftRow, topLine, x);

        int sad = 0;
        for (final byte b : pred) {
            sad += MathUtil.abs(b - s0);
        }
        return sad;
    }

    private static int getDC(final boolean leftAvailable, final boolean topAvailable, final byte[] leftRow, final byte[] topLine, final int x) {
        final int s0;
        if (leftAvailable && topAvailable) {
            s0 = (ArrayUtil.sumByte(leftRow) + ArrayUtil.sumByte(topLine, x, 16) + 16) >> 5;
        } else if (leftAvailable) {
            s0 = (ArrayUtil.sumByte(leftRow) + 8) >> 4;
        } else if (topAvailable) {
            s0 = (ArrayUtil.sumByte(topLine, x, 16) + 8) >> 4;
        } else {
            s0 = 0;
        }
        return s0;
    }

    public static void predictPlane(final int[][] residual, final byte[] leftRow,
                                    final byte[] topLine, final byte[] topLeft,
                                    final int x, final byte[] pixOut) {
        int H = 0;

        for (int i = 0; i < 7; i++) {
            H += (i + 1) * (topLine[x + 8 + i] - topLine[x + 6 - i]);
        }
        H += 8 * (topLine[x + 15] - topLeft[0]);

        int V = 0;
        for (int j = 0; j < 7; j++) {
            V += (j + 1) * (leftRow[8 + j] - leftRow[6 - j]);
        }
        V += 8 * (leftRow[15] - topLeft[0]);

        final int c = (5 * V + 32) >> 6;
        final int b = (5 * H + 32) >> 6;
        final int a = 16 * (leftRow[15] + topLine[x + 15]);

        int off = 0;
        for (int j = 0; j < 16; j++) {
            for (int i = 0; i < 16; i++, off++) {
                final int val = MathUtil.clip((a + b * (i - 7) + c * (j - 7) + 16) >> 5, -128, 127);
                pixOut[off] = (byte) MathUtil.clip(residual[LUMA_4x4_BLOCK_LUT[off]][LUMA_4x4_POS_LUT[off]] + val, -128, 127);
            }
        }
    }

    public static void lumaPlanePred(final byte[] leftRow, final byte[] topLine, final byte topLeft, final int x, final byte[][] pred) {
        int H = 0;

        for (int i = 0; i < 7; i++) {
            H += (i + 1) * (topLine[x + 8 + i] - topLine[x + 6 - i]);
        }
        H += 8 * (topLine[x + 15] - topLeft);

        int V = 0;
        for (int j = 0; j < 7; j++) {
            V += (j + 1) * (leftRow[8 + j] - leftRow[6 - j]);
        }
        V += 8 * (leftRow[15] - topLeft);

        final int c = (5 * V + 32) >> 6;
        final int b = (5 * H + 32) >> 6;
        final int a = 16 * (leftRow[15] + topLine[x + 15]);

        int off = 0;
        for (int j = 0; j < 16; j++) {
            for (int i = 0; i < 16; i++, off++) {
                final int val = MathUtil.clip((a + b * (i - 7) + c * (j - 7) + 16) >> 5, -128, 127);
                pred[LUMA_4x4_BLOCK_LUT[off]][LUMA_4x4_POS_LUT[off]] = (byte) val;
            }
        }
    }

    public static int lumaPlanePredSAD(final boolean leftAvailable, final boolean topAvailable, final byte[] leftRow, final byte[] topLine, final byte topLeft, final int x, final byte[] pred) {
        if (!leftAvailable || !topAvailable)
            return Integer.MAX_VALUE;
        int H = 0;

        for (int i = 0; i < 7; i++) {
            H += (i + 1) * (topLine[x + 8 + i] - topLine[x + 6 - i]);
        }
        H += 8 * (topLine[x + 15] - topLeft);

        int V = 0;
        for (int j = 0; j < 7; j++) {
            V += (j + 1) * (leftRow[8 + j] - leftRow[6 - j]);
        }
        V += 8 * (leftRow[15] - topLeft);

        final int c = (5 * V + 32) >> 6;
        final int b = (5 * H + 32) >> 6;
        final int a = 16 * (leftRow[15] + topLine[x + 15]);

        int sad = 0;
        int off = 0;
        for (int j = 0; j < 16; j++) {
            for (int i = 0; i < 16; i++, off++) {
                final int val = MathUtil.clip((a + b * (i - 7) + c * (j - 7) + 16) >> 5, -128, 127);
                sad += MathUtil.abs(pred[off] - val);
            }
        }
        return sad;
    }
}
