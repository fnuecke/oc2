/* SPDX-License-Identifier: BSD 2-Clause "Simplified" License */

package li.cil.oc2.jcodec.codecs.h264.decode;

import li.cil.oc2.jcodec.common.tools.MathUtil;

import static li.cil.oc2.jcodec.codecs.h264.H264Const.CHROMA_BLOCK_LUT;
import static li.cil.oc2.jcodec.codecs.h264.H264Const.CHROMA_POS_LUT;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * <p>
 * Prediction builder for chroma samples
 *
 * @author The JCodec project
 */
public final class ChromaPredictionBuilder {
    public static void predictWithMode(final int[][] residual, final int chromaMode, final int mbX, final boolean leftAvailable, final boolean topAvailable,
                                       final byte[] leftRow, final byte[] topLine, final byte[] topLeft, final byte[] pixOut) {
        switch (chromaMode) {
            case 0 -> predictDC(residual, mbX, leftAvailable, topAvailable, leftRow, topLine, pixOut);
            case 1 -> predictHorizontal(residual, leftRow, pixOut);
            case 2 -> predictVertical(residual, mbX, topLine, pixOut);
            case 3 -> predictPlane(residual, mbX, leftRow, topLine, topLeft, pixOut);
        }
    }

    public static void buildPred(final int chromaMode, final int mbX, final boolean leftAvailable, final boolean topAvailable,
                                 final byte[] leftRow, final byte[] topLine, final byte topLeft, final byte[][] pixOut) {
        switch (chromaMode) {
            case 0 -> buildPredDC(mbX, leftAvailable, topAvailable, leftRow, topLine, pixOut);
            case 1 -> buildPredHorz(leftRow, pixOut);
            case 2 -> buildPredVert(mbX, topLine, pixOut);
            case 3 -> buildPredPlane(mbX, leftRow, topLine, topLeft, pixOut);
        }
    }

    public static int predSAD(final int chromaMode, final int mbX, final boolean leftAvailable, final boolean topAvailable,
                              final byte[] leftRow, final byte[] topLine, final byte topLeft, final byte[] pix) {
        return switch (chromaMode) {
            default -> predDCSAD(mbX, leftAvailable, topAvailable, leftRow, topLine, pix);
            case 1 -> predHorizontalSAD(leftRow, pix);
            case 2 -> predVerticalSAD(mbX, topLine, pix);
            case 3 -> predPlaneSAD(mbX, leftRow, topLine, topLeft, pix);
        };
    }

    public static boolean predAvb(final int chromaMode, final boolean leftAvailable, final boolean topAvailable) {
        return switch (chromaMode) {
            default -> true;
            case 1 -> leftAvailable;
            case 2 -> topAvailable;
            case 3 -> leftAvailable && topAvailable;
        };
    }

    public static void predictDC(final int[][] planeData, final int mbX, final boolean leftAvailable, final boolean topAvailable,
                                 final byte[] leftRow, final byte[] topLine, final byte[] pixOut) {
        predictDCInside(planeData, 0, 0, mbX, leftAvailable, topAvailable, leftRow, topLine, pixOut);
        predictDCTopBorder(planeData, 1, 0, mbX, leftAvailable, topAvailable, leftRow, topLine, pixOut);
        predictDCLeftBorder(planeData, 0, 1, mbX, leftAvailable, topAvailable, leftRow, topLine, pixOut);
        predictDCInside(planeData, 1, 1, mbX, leftAvailable, topAvailable, leftRow, topLine, pixOut);
    }

    public static int predDCSAD(final int mbX, final boolean leftAvailable, final boolean topAvailable,
                                final byte[] leftRow, final byte[] topLine, final byte[] pixOut) {
        return predictDCInsideSAD(0, 0, mbX, leftAvailable, topAvailable, leftRow, topLine, pixOut)
            + predictDCTopBorderSAD(1, 0, mbX, leftAvailable, topAvailable, leftRow, topLine, pixOut)
            + predictDCLeftBorderSAD(0, 1, mbX, leftAvailable, topAvailable, leftRow, topLine, pixOut)
            + predictDCInsideSAD(1, 1, mbX, leftAvailable, topAvailable, leftRow, topLine, pixOut);
    }

    public static void buildPredDC(final int mbX, final boolean leftAvailable, final boolean topAvailable,
                                   final byte[] leftRow, final byte[] topLine, final byte[][] pixOut) {
        buildPredDCIns(0, 0, mbX, leftAvailable, topAvailable, leftRow, topLine, pixOut[0]);
        buildPredDCTop(1, 0, mbX, leftAvailable, topAvailable, leftRow, topLine, pixOut[1]);
        buildPredDCLft(0, 1, mbX, leftAvailable, topAvailable, leftRow, topLine, pixOut[2]);
        buildPredDCIns(1, 1, mbX, leftAvailable, topAvailable, leftRow, topLine, pixOut[3]);
    }

    public static void predictVertical(final int[][] residual, final int mbX, final byte[] topLine, final byte[] pixOut) {
        for (int off = 0, j = 0; j < 8; j++) {
            for (int i = 0; i < 8; i++, off++)
                pixOut[off] = (byte) MathUtil.clip(
                    residual[CHROMA_BLOCK_LUT[off]][CHROMA_POS_LUT[off]] + topLine[(mbX << 3) + i], -128, 127);
        }
    }

    public static int predVerticalSAD(final int mbX, final byte[] topLine, final byte[] pixOut) {
        int sad = 0;
        for (int off = 0, j = 0; j < 8; j++) {
            for (int i = 0; i < 8; i++, off++)
                sad += MathUtil.abs(pixOut[off] - topLine[(mbX << 3) + i]);
        }
        return sad;
    }

    public static void buildPredVert(final int mbX, final byte[] topLine, final byte[][] pixOut) {
        for (int off = 0, j = 0; j < 8; j++) {
            for (int i = 0; i < 8; i++, off++)
                pixOut[CHROMA_BLOCK_LUT[off]][CHROMA_POS_LUT[off]] = topLine[(mbX << 3) + i];
        }
    }

    public static void predictHorizontal(final int[][] residual, final byte[] leftRow, final byte[] pixOut) {
        for (int off = 0, j = 0; j < 8; j++) {
            for (int i = 0; i < 8; i++, off++)
                pixOut[off] = (byte) MathUtil.clip(residual[CHROMA_BLOCK_LUT[off]][CHROMA_POS_LUT[off]] + leftRow[j], -128, 127);
        }
    }

    public static int predHorizontalSAD(final byte[] leftRow, final byte[] pixOut) {
        int sad = 0;
        for (int off = 0, j = 0; j < 8; j++) {
            for (int i = 0; i < 8; i++, off++)
                sad += MathUtil.abs(pixOut[off] - leftRow[j]);
        }
        return sad;
    }

    public static void buildPredHorz(final byte[] leftRow, final byte[][] pixOut) {
        for (int off = 0, j = 0; j < 8; j++) {
            for (int i = 0; i < 8; i++, off++)
                pixOut[CHROMA_BLOCK_LUT[off]][CHROMA_POS_LUT[off]] = leftRow[j];
        }
    }

    public static void predictDCInside(final int[][] residual, final int blkX, final int blkY, final int mbX,
                                       final boolean leftAvailable, final boolean topAvailable,
                                       final byte[] leftRow, final byte[] topLine, final byte[] pixOut) {
        int s0;
        final int blkOffX = (blkX << 2) + (mbX << 3);
        final int blkOffY = blkY << 2;

        if (leftAvailable && topAvailable) {
            s0 = 0;
            for (int i = 0; i < 4; i++)
                s0 += leftRow[i + blkOffY];
            for (int i = 0; i < 4; i++)
                s0 += topLine[blkOffX + i];

            s0 = (s0 + 4) >> 3;
        } else if (leftAvailable) {
            s0 = 0;
            for (int i = 0; i < 4; i++)
                s0 += leftRow[blkOffY + i];
            s0 = (s0 + 2) >> 2;
        } else if (topAvailable) {
            s0 = 0;
            for (int i = 0; i < 4; i++)
                s0 += topLine[blkOffX + i];
            s0 = (s0 + 2) >> 2;
        } else {
            s0 = 0;
        }
        for (int off = (blkY << 5) + (blkX << 2), j = 0; j < 4; j++, off += 8) {
            pixOut[off] = (byte) MathUtil.clip(residual[CHROMA_BLOCK_LUT[off]][CHROMA_POS_LUT[off]] + s0, -128, 127);
            pixOut[off + 1] = (byte) MathUtil.clip(residual[CHROMA_BLOCK_LUT[off + 1]][CHROMA_POS_LUT[off + 1]] + s0, -128, 127);
            pixOut[off + 2] = (byte) MathUtil.clip(residual[CHROMA_BLOCK_LUT[off + 2]][CHROMA_POS_LUT[off + 2]] + s0, -128, 127);
            pixOut[off + 3] = (byte) MathUtil.clip(residual[CHROMA_BLOCK_LUT[off + 3]][CHROMA_POS_LUT[off + 3]] + s0, -128, 127);
        }
    }

    public static int predictDCInsideSAD(final int blkX, final int blkY, final int mbX,
                                         final boolean leftAvailable, final boolean topAvailable,
                                         final byte[] leftRow, final byte[] topLine, final byte[] pixOut) {
        int s0;
        final int blkOffX = (blkX << 2) + (mbX << 3);
        final int blkOffY = blkY << 2;

        if (leftAvailable && topAvailable) {
            s0 = 0;
            for (int i = 0; i < 4; i++)
                s0 += leftRow[i + blkOffY];
            for (int i = 0; i < 4; i++)
                s0 += topLine[blkOffX + i];

            s0 = (s0 + 4) >> 3;
        } else if (leftAvailable) {
            s0 = 0;
            for (int i = 0; i < 4; i++)
                s0 += leftRow[blkOffY + i];
            s0 = (s0 + 2) >> 2;
        } else if (topAvailable) {
            s0 = 0;
            for (int i = 0; i < 4; i++)
                s0 += topLine[blkOffX + i];
            s0 = (s0 + 2) >> 2;
        } else {
            s0 = 0;
        }
        int sad = 0;
        for (int off = (blkY << 5) + (blkX << 2), j = 0; j < 4; j++, off += 8) {
            sad += MathUtil.abs(pixOut[off] - s0);
            sad += MathUtil.abs(pixOut[off + 1] - s0);
            sad += MathUtil.abs(pixOut[off + 2] - s0);
            sad += MathUtil.abs(pixOut[off + 3] - s0);
        }
        return sad;
    }

    public static void buildPredDCIns(final int blkX, final int blkY, final int mbX,
                                      final boolean leftAvailable, final boolean topAvailable,
                                      final byte[] leftRow, final byte[] topLine, final byte[] pixOut) {
        int s0;
        final int blkOffX = (blkX << 2) + (mbX << 3);
        final int blkOffY = blkY << 2;

        if (leftAvailable && topAvailable) {
            s0 = 0;
            for (int i = 0; i < 4; i++)
                s0 += leftRow[i + blkOffY];
            for (int i = 0; i < 4; i++)
                s0 += topLine[blkOffX + i];

            s0 = (s0 + 4) >> 3;
        } else if (leftAvailable) {
            s0 = 0;
            for (int i = 0; i < 4; i++)
                s0 += leftRow[blkOffY + i];
            s0 = (s0 + 2) >> 2;
        } else if (topAvailable) {
            s0 = 0;
            for (int i = 0; i < 4; i++)
                s0 += topLine[blkOffX + i];
            s0 = (s0 + 2) >> 2;
        } else {
            s0 = 0;
        }
        for (int j = 0; j < 16; j++) {
            pixOut[j] = (byte) s0;
        }
    }

    public static void predictDCTopBorder(final int[][] residual, final int blkX, final int blkY, final int mbX,
                                          final boolean leftAvailable, final boolean topAvailable,
                                          final byte[] leftRow, final byte[] topLine, final byte[] pixOut) {
        int s1;
        final int blkOffX = (blkX << 2) + (mbX << 3);
        final int blkOffY = blkY << 2;
        if (topAvailable) {
            s1 = 0;
            for (int i = 0; i < 4; i++)
                s1 += topLine[blkOffX + i];

            s1 = (s1 + 2) >> 2;
        } else if (leftAvailable) {
            s1 = 0;
            for (int i = 0; i < 4; i++)
                s1 += leftRow[blkOffY + i];
            s1 = (s1 + 2) >> 2;
        } else {
            s1 = 0;
        }
        for (int off = (blkY << 5) + (blkX << 2), j = 0; j < 4; j++, off += 8) {
            pixOut[off] = (byte) MathUtil.clip(residual[CHROMA_BLOCK_LUT[off]][CHROMA_POS_LUT[off]] + s1, -128, 127);
            pixOut[off + 1] = (byte) MathUtil.clip(residual[CHROMA_BLOCK_LUT[off + 1]][CHROMA_POS_LUT[off + 1]] + s1, -128, 127);
            pixOut[off + 2] = (byte) MathUtil.clip(residual[CHROMA_BLOCK_LUT[off + 2]][CHROMA_POS_LUT[off + 2]] + s1, -128, 127);
            pixOut[off + 3] = (byte) MathUtil.clip(residual[CHROMA_BLOCK_LUT[off + 3]][CHROMA_POS_LUT[off + 3]] + s1, -128, 127);
        }
    }

    public static int predictDCTopBorderSAD(final int blkX, final int blkY, final int mbX,
                                            final boolean leftAvailable, final boolean topAvailable,
                                            final byte[] leftRow, final byte[] topLine, final byte[] pixOut) {
        int s1;
        final int blkOffX = (blkX << 2) + (mbX << 3);
        final int blkOffY = blkY << 2;
        if (topAvailable) {
            s1 = 0;
            for (int i = 0; i < 4; i++)
                s1 += topLine[blkOffX + i];

            s1 = (s1 + 2) >> 2;
        } else if (leftAvailable) {
            s1 = 0;
            for (int i = 0; i < 4; i++)
                s1 += leftRow[blkOffY + i];
            s1 = (s1 + 2) >> 2;
        } else {
            s1 = 0;
        }
        int sad = 0;
        for (int off = (blkY << 5) + (blkX << 2), j = 0; j < 4; j++, off += 8) {
            sad += MathUtil.abs(pixOut[off] - s1);
            sad += MathUtil.abs(pixOut[off + 1] - s1);
            sad += MathUtil.abs(pixOut[off + 2] - s1);
            sad += MathUtil.abs(pixOut[off + 3] - s1);
        }
        return sad;
    }

    public static void buildPredDCTop(final int blkX, final int blkY, final int mbX,
                                      final boolean leftAvailable, final boolean topAvailable,
                                      final byte[] leftRow, final byte[] topLine, final byte[] pixOut) {
        int s1;
        final int blkOffX = (blkX << 2) + (mbX << 3);
        final int blkOffY = blkY << 2;
        if (topAvailable) {
            s1 = 0;
            for (int i = 0; i < 4; i++)
                s1 += topLine[blkOffX + i];

            s1 = (s1 + 2) >> 2;
        } else if (leftAvailable) {
            s1 = 0;
            for (int i = 0; i < 4; i++)
                s1 += leftRow[blkOffY + i];
            s1 = (s1 + 2) >> 2;
        } else {
            s1 = 0;
        }
        for (int j = 0; j < 16; j++) {
            pixOut[j] = (byte) s1;
        }
    }

    public static void predictDCLeftBorder(final int[][] residual, final int blkX, final int blkY, final int mbX,
                                           final boolean leftAvailable, final boolean topAvailable,
                                           final byte[] leftRow, final byte[] topLine, final byte[] pixOut) {
        int s2;
        final int blkOffX = (blkX << 2) + (mbX << 3);
        final int blkOffY = blkY << 2;
        if (leftAvailable) {
            s2 = 0;
            for (int i = 0; i < 4; i++)
                s2 += leftRow[blkOffY + i];
            s2 = (s2 + 2) >> 2;
        } else if (topAvailable) {
            s2 = 0;
            for (int i = 0; i < 4; i++)
                s2 += topLine[blkOffX + i];
            s2 = (s2 + 2) >> 2;
        } else {
            s2 = 0;
        }
        for (int off = (blkY << 5) + (blkX << 2), j = 0; j < 4; j++, off += 8) {
            pixOut[off] = (byte) MathUtil.clip(residual[CHROMA_BLOCK_LUT[off]][CHROMA_POS_LUT[off]] + s2, -128, 127);
            pixOut[off + 1] = (byte) MathUtil.clip(residual[CHROMA_BLOCK_LUT[off + 1]][CHROMA_POS_LUT[off + 1]] + s2, -128, 127);
            pixOut[off + 2] = (byte) MathUtil.clip(residual[CHROMA_BLOCK_LUT[off + 2]][CHROMA_POS_LUT[off + 2]] + s2, -128, 127);
            pixOut[off + 3] = (byte) MathUtil.clip(residual[CHROMA_BLOCK_LUT[off + 3]][CHROMA_POS_LUT[off + 3]] + s2, -128, 127);
        }
    }

    public static int predictDCLeftBorderSAD(final int blkX, final int blkY, final int mbX,
                                             final boolean leftAvailable, final boolean topAvailable,
                                             final byte[] leftRow, final byte[] topLine, final byte[] pixOut) {
        int s2;
        final int blkOffX = (blkX << 2) + (mbX << 3);
        final int blkOffY = blkY << 2;
        if (leftAvailable) {
            s2 = 0;
            for (int i = 0; i < 4; i++)
                s2 += leftRow[blkOffY + i];
            s2 = (s2 + 2) >> 2;
        } else if (topAvailable) {
            s2 = 0;
            for (int i = 0; i < 4; i++)
                s2 += topLine[blkOffX + i];
            s2 = (s2 + 2) >> 2;
        } else {
            s2 = 0;
        }
        int sad = 0;
        for (int off = (blkY << 5) + (blkX << 2), j = 0; j < 4; j++, off += 8) {
            sad += MathUtil.abs(pixOut[off] - s2);
            sad += MathUtil.abs(pixOut[off + 1] - s2);
            sad += MathUtil.abs(pixOut[off + 2] - s2);
            sad += MathUtil.abs(pixOut[off + 3] - s2);
        }
        return sad;
    }

    public static void buildPredDCLft(final int blkX, final int blkY, final int mbX,
                                      final boolean leftAvailable, final boolean topAvailable,
                                      final byte[] leftRow, final byte[] topLine, final byte[] pixOut) {
        int s2;
        final int blkOffX = (blkX << 2) + (mbX << 3);
        final int blkOffY = blkY << 2;
        if (leftAvailable) {
            s2 = 0;
            for (int i = 0; i < 4; i++)
                s2 += leftRow[blkOffY + i];
            s2 = (s2 + 2) >> 2;
        } else if (topAvailable) {
            s2 = 0;
            for (int i = 0; i < 4; i++)
                s2 += topLine[blkOffX + i];
            s2 = (s2 + 2) >> 2;
        } else {
            s2 = 0;
        }
        for (int j = 0; j < 16; j++) {
            pixOut[j] = (byte) s2;
        }
    }

    public static void predictPlane(final int[][] residual, final int mbX, final byte[] leftRow, final byte[] topLine, final byte[] topLeft, final byte[] pixOut) {
        int H = 0;
        final int blkOffX = (mbX << 3);

        for (int i = 0; i < 3; i++) {
            H += (i + 1) * (topLine[blkOffX + 4 + i] - topLine[blkOffX + 2 - i]);
        }
        H += 4 * (topLine[blkOffX + 7] - topLeft[0]);

        int V = 0;
        for (int j = 0; j < 3; j++) {
            V += (j + 1) * (leftRow[4 + j] - leftRow[2 - j]);
        }
        V += 4 * (leftRow[7] - topLeft[0]);

        final int c = (34 * V + 32) >> 6;
        final int b = (34 * H + 32) >> 6;
        final int a = 16 * (leftRow[7] + topLine[blkOffX + 7]);

        for (int off = 0, j = 0; j < 8; j++) {
            for (int i = 0; i < 8; i++, off++) {
                final int val = (a + b * (i - 3) + c * (j - 3) + 16) >> 5;
                pixOut[off] = (byte) MathUtil.clip(residual[CHROMA_BLOCK_LUT[off]][CHROMA_POS_LUT[off]] + MathUtil.clip(val, -128, 127),
                    -128, 127);
            }
        }
    }

    public static int predPlaneSAD(final int mbX, final byte[] leftRow, final byte[] topLine, final byte topLeft, final byte[] pixOut) {
        int sad = 0;
        int H = 0;
        final int blkOffX = (mbX << 3);

        for (int i = 0; i < 3; i++) {
            H += (i + 1) * (topLine[blkOffX + 4 + i] - topLine[blkOffX + 2 - i]);
        }
        H += 4 * (topLine[blkOffX + 7] - topLeft);

        int V = 0;
        for (int j = 0; j < 3; j++) {
            V += (j + 1) * (leftRow[4 + j] - leftRow[2 - j]);
        }
        V += 4 * (leftRow[7] - topLeft);

        final int c = (34 * V + 32) >> 6;
        final int b = (34 * H + 32) >> 6;
        final int a = 16 * (leftRow[7] + topLine[blkOffX + 7]);

        for (int off = 0, j = 0; j < 8; j++) {
            for (int i = 0; i < 8; i++, off++) {
                final int val = (a + b * (i - 3) + c * (j - 3) + 16) >> 5;
                sad += MathUtil.abs(pixOut[off] - MathUtil.clip(val, -128, 127));
            }
        }
        return sad;
    }

    public static void buildPredPlane(final int mbX, final byte[] leftRow, final byte[] topLine, final byte topLeft, final byte[][] pixOut) {
        int H = 0;
        final int blkOffX = (mbX << 3);

        for (int i = 0; i < 3; i++) {
            H += (i + 1) * (topLine[blkOffX + 4 + i] - topLine[blkOffX + 2 - i]);
        }
        H += 4 * (topLine[blkOffX + 7] - topLeft);

        int V = 0;
        for (int j = 0; j < 3; j++) {
            V += (j + 1) * (leftRow[4 + j] - leftRow[2 - j]);
        }
        V += 4 * (leftRow[7] - topLeft);

        final int c = (34 * V + 32) >> 6;
        final int b = (34 * H + 32) >> 6;
        final int a = 16 * (leftRow[7] + topLine[blkOffX + 7]);

        for (int off = 0, j = 0; j < 8; j++) {
            for (int i = 0; i < 8; i++, off++) {
                final int val = (a + b * (i - 3) + c * (j - 3) + 16) >> 5;
                pixOut[CHROMA_BLOCK_LUT[off]][CHROMA_POS_LUT[off]] = (byte) MathUtil.clip(val, -128, 127);
            }
        }
    }
}
