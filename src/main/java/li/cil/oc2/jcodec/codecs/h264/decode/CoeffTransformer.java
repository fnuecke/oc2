/* SPDX-License-Identifier: BSD 2-Clause "Simplified" License */

package li.cil.oc2.jcodec.codecs.h264.decode;

import li.cil.oc2.jcodec.common.ArrayUtil;

import java.util.Arrays;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * <p>
 * Integer DCT 4x4 base implementation
 *
 * @author The JCodec project
 */
public final class CoeffTransformer {
    public static final int[] zigzag4x4 = {0, 1, 4, 8, 5, 2, 3, 6, 9, 12, 13, 10, 7, 11, 14, 15};
    public static final int[] invZigzag4x4 = new int[16];

    static final int[][] dequantCoef = {
        {10, 13, 10, 13, 13, 16, 13, 16, 10, 13, 10, 13, 13, 16, 13, 16},
        {11, 14, 11, 14, 14, 18, 14, 18, 11, 14, 11, 14, 14, 18, 14, 18},
        {13, 16, 13, 16, 16, 20, 16, 20, 13, 16, 13, 16, 16, 20, 16, 20},
        {14, 18, 14, 18, 18, 23, 18, 23, 14, 18, 14, 18, 18, 23, 18, 23},
        {16, 20, 16, 20, 20, 25, 20, 25, 16, 20, 16, 20, 20, 25, 20, 25},
        {18, 23, 18, 23, 23, 29, 23, 29, 18, 23, 18, 23, 23, 29, 23, 29}
    };

    static final int[][] dequantCoef8x8 = new int[6][64];

    static final int[][] initDequantCoeff8x8 = {
        {20, 18, 32, 19, 25, 24},
        {22, 19, 35, 21, 28, 26},
        {26, 23, 42, 24, 33, 31},
        {28, 25, 45, 26, 35, 33},
        {32, 28, 51, 30, 40, 38},
        {36, 32, 58, 34, 46, 43}
    };

    public static final int[] zigzag8x8 = new int[]{0, 1, 8, 16, 9, 2, 3, 10, 17, 24, 32, 25, 18, 11, 4, 5, 12, 19, 26, 33, 40, 48, 41, 34, 27, 20, 13, 6, 7, 14, 21, 28, 35, 42, 49, 56, 57, 50, 43, 36, 29, 22, 15, 23, 30, 37, 44, 51, 58, 59, 52, 45, 38, 31, 39, 46, 53, 60, 61, 54, 47, 55, 62, 63};
    public static final int[] invZigzag8x8 = new int[64];

    static {
        for (int i = 0; i < 16; i++)
            invZigzag4x4[zigzag4x4[i]] = i;
        for (int i = 0; i < 64; i++)
            invZigzag8x8[zigzag8x8[i]] = i;
    }

    private static final int[][] quantCoeff = {
        {13107, 8066, 13107, 8066, 8066, 5243, 8066, 5243, 13107, 8066, 13107, 8066, 8066, 5243, 8066, 5243},
        {11916, 7490, 11916, 7490, 7490, 4660, 7490, 4660, 11916, 7490, 11916, 7490, 7490, 4660, 7490, 4660},
        {10082, 6554, 10082, 6554, 6554, 4194, 6554, 4194, 10082, 6554, 10082, 6554, 6554, 4194, 6554, 4194},
        {9362, 5825, 9362, 5825, 5825, 3647, 5825, 3647, 9362, 5825, 9362, 5825, 5825, 3647, 5825, 3647},
        {8192, 5243, 8192, 5243, 5243, 3355, 5243, 3355, 8192, 5243, 8192, 5243, 5243, 3355, 5243, 3355},
        {7282, 4559, 7282, 4559, 4559, 2893, 4559, 2893, 7282, 4559, 7282, 4559, 4559, 2893, 4559, 2893}};

    static {
        for (int g = 0; g < 6; g++) {
            Arrays.fill(dequantCoef8x8[g], initDequantCoeff8x8[g][5]);
            for (int i = 0; i < 8; i += 4)
                for (int j = 0; j < 8; j += 4)
                    dequantCoef8x8[g][(i << 3) + j] = initDequantCoeff8x8[g][0];
            for (int i = 1; i < 8; i += 2)
                for (int j = 1; j < 8; j += 2)
                    dequantCoef8x8[g][(i << 3) + j] = initDequantCoeff8x8[g][1];
            for (int i = 2; i < 8; i += 4)
                for (int j = 2; j < 8; j += 4)
                    dequantCoef8x8[g][(i << 3) + j] = initDequantCoeff8x8[g][2];
            for (int i = 0; i < 8; i += 4)
                for (int j = 1; j < 8; j += 2)
                    dequantCoef8x8[g][(i << 3) + j] = initDequantCoeff8x8[g][3];
            for (int i = 1; i < 8; i += 2)
                for (int j = 0; j < 8; j += 4)
                    dequantCoef8x8[g][(i << 3) + j] = initDequantCoeff8x8[g][3];
            for (int i = 0; i < 8; i += 4)
                for (int j = 2; j < 8; j += 4)
                    dequantCoef8x8[g][(i << 3) + j] = initDequantCoeff8x8[g][4];
            for (int i = 2; i < 8; i += 4)
                for (int j = 0; j < 8; j += 4)
                    dequantCoef8x8[g][(i << 3) + j] = initDequantCoeff8x8[g][4];
        }
    }

    /**
     * Inverce integer DCT transform for 4x4 block
     */
    public static void idct4x4(final int[] block) {
        idct4x4(block, block);
    }

    public static void idct4x4(final int[] block, final int[] out) {
        // Horisontal
        for (int i = 0; i < 16; i += 4) {
            final int e0 = block[i] + block[i + 2];
            final int e1 = block[i] - block[i + 2];
            final int e2 = (block[i + 1] >> 1) - block[i + 3];
            final int e3 = block[i + 1] + (block[i + 3] >> 1);

            out[i] = e0 + e3;
            out[i + 1] = e1 + e2;
            out[i + 2] = e1 - e2;
            out[i + 3] = e0 - e3;
        }

        // Vertical
        for (int i = 0; i < 4; i++) {
            final int g0 = out[i] + out[i + 8];
            final int g1 = out[i] - out[i + 8];
            final int g2 = (out[i + 4] >> 1) - out[i + 12];
            final int g3 = out[i + 4] + (out[i + 12] >> 1);
            out[i] = g0 + g3;
            out[i + 4] = g1 + g2;
            out[i + 8] = g1 - g2;
            out[i + 12] = g0 - g3;
        }

        // scale down
        for (int i = 0; i < 16; i++) {
            out[i] = (out[i] + 32) >> 6;
        }
    }

    public static void fdct4x4(final int[] block) {
        // Horizontal
        for (int i = 0; i < 16; i += 4) {
            final int t0 = block[i] + block[i + 3];
            final int t1 = block[i + 1] + block[i + 2];
            final int t2 = block[i + 1] - block[i + 2];
            final int t3 = block[i] - block[i + 3];

            block[i] = t0 + t1;
            block[i + 1] = (t3 << 1) + t2;
            block[i + 2] = t0 - t1;
            block[i + 3] = t3 - (t2 << 1);
        }

        // Vertical
        for (int i = 0; i < 4; i++) {
            final int t0 = block[i] + block[i + 12];
            final int t1 = block[i + 4] + block[i + 8];
            final int t2 = block[i + 4] - block[i + 8];
            final int t3 = block[i] - block[i + 12];

            block[i] = t0 + t1;
            block[i + 4] = t2 + (t3 << 1);
            block[i + 8] = t0 - t1;
            block[i + 12] = t3 - (t2 << 1);
        }
    }

    /**
     * Inverse Hadamard transform
     */
    public static void invDC4x4(final int[] scaled) {
        // Horizontal
        for (int i = 0; i < 16; i += 4) {
            final int e0 = scaled[i] + scaled[i + 2];
            final int e1 = scaled[i] - scaled[i + 2];
            final int e2 = scaled[i + 1] - scaled[i + 3];
            final int e3 = scaled[i + 1] + scaled[i + 3];

            scaled[i] = e0 + e3;
            scaled[i + 1] = e1 + e2;
            scaled[i + 2] = e1 - e2;
            scaled[i + 3] = e0 - e3;
        }

        // Vertical
        for (int i = 0; i < 4; i++) {
            final int g0 = scaled[i] + scaled[i + 8];
            final int g1 = scaled[i] - scaled[i + 8];
            final int g2 = scaled[i + 4] - scaled[i + 12];
            final int g3 = scaled[i + 4] + scaled[i + 12];
            scaled[i] = g0 + g3;
            scaled[i + 4] = g1 + g2;
            scaled[i + 8] = g1 - g2;
            scaled[i + 12] = g0 - g3;
        }
    }

    /**
     * Forward Hadamard transform
     */
    public static void fvdDC4x4(final int[] scaled) {
        // Horizontal
        for (int i = 0; i < 16; i += 4) {
            final int t0 = scaled[i] + scaled[i + 3];
            final int t1 = scaled[i + 1] + scaled[i + 2];
            final int t2 = scaled[i + 1] - scaled[i + 2];
            final int t3 = scaled[i] - scaled[i + 3];

            scaled[i] = t0 + t1;
            scaled[i + 1] = t3 + t2;
            scaled[i + 2] = t0 - t1;
            scaled[i + 3] = t3 - t2;
        }

        // Vertical
        for (int i = 0; i < 4; i++) {
            final int t0 = scaled[i] + scaled[i + 12];
            final int t1 = scaled[i + 4] + scaled[i + 8];
            final int t2 = scaled[i + 4] - scaled[i + 8];
            final int t3 = scaled[i] - scaled[i + 12];

            scaled[i] = (t0 + t1) >> 1;
            scaled[i + 4] = (t2 + t3) >> 1;
            scaled[i + 8] = (t0 - t1) >> 1;
            scaled[i + 12] = (t3 - t2) >> 1;
        }
    }

    public static void dequantizeAC(final int[] coeffs, final int qp, final int[] scalingList) {
        final int group = qp % 6;

        if (scalingList == null) {
            final int qbits = qp / 6;
            for (int i = 0; i < 16; i++)
                coeffs[i] = (coeffs[i] * dequantCoef[group][i]) << qbits;
        } else {
            if (qp >= 24) {
                final int qbits = qp / 6 - 4;
                for (int i = 0; i < 16; i++)
                    coeffs[i] = (coeffs[i] * dequantCoef[group][i] * scalingList[invZigzag4x4[i]]) << qbits;
            } else {
                final int qbits = 4 - qp / 6;
                final int addition = 1 << (3 - qp / 6);
                for (int i = 0; i < 16; i++)
                    coeffs[i] = (coeffs[i] * scalingList[invZigzag4x4[i]] * dequantCoef[group][i] + addition) >> qbits;
            }
        }
    }

    public static void quantizeAC(final int[] coeffs, final int qp) {
        final int level = qp / 6;
        final int offset = qp % 6;

        final int addition = 682 << (qp / 6 + 4);
        final int qbits = 15 + level;

        if (qp < 10) {
            for (int i = 0; i < 16; i++) {
                final int sign = (coeffs[i] >> 31);
                coeffs[i] = (Math.min((((coeffs[i] ^ sign) - sign) * quantCoeff[offset][i] + addition) >> qbits, 2063) ^ sign)
                    - sign;
            }
        } else {
            for (int i = 0; i < 16; i++) {
                final int sign = (coeffs[i] >> 31);
                coeffs[i] = (((((coeffs[i] ^ sign) - sign) * quantCoeff[offset][i] + addition) >> qbits) ^ sign) - sign;
            }
        }
    }

    public static void dequantizeDC4x4(final int[] coeffs, final int qp, final int[] scalingList) {
        final int group = qp % 6;

        if (qp >= 36) {
            final int qbits = qp / 6 - 6;
            if (scalingList == null) {
                for (int i = 0; i < 16; i++)
                    coeffs[i] = (coeffs[i] * (dequantCoef[group][0] << 4)) << qbits;
            } else {
                for (int i = 0; i < 16; i++)
                    coeffs[i] = (coeffs[i] * dequantCoef[group][0] * scalingList[0]) << qbits;
            }
        } else {
            final int qbits = 6 - qp / 6;
            final int addition = 1 << (5 - qp / 6);
            if (scalingList == null) {
                for (int i = 0; i < 16; i++)
                    coeffs[i] = (coeffs[i] * (dequantCoef[group][0] << 4) + addition) >> qbits;
            } else {
                for (int i = 0; i < 16; i++)
                    coeffs[i] = (coeffs[i] * dequantCoef[group][0] * scalingList[0] + addition) >> qbits;
            }
        }
    }

    public static void quantizeDC4x4(final int[] coeffs, final int qp) {
        final int level = qp / 6;
        final int offset = qp % 6;

        final int addition = 682 << (qp / 6 + 5);
        final int qbits = 16 + level;

        if (qp < 10) {
            for (int i = 0; i < 16; i++) {
                final int sign = (coeffs[i] >> 31);
                coeffs[i] = (Math.min((((coeffs[i] ^ sign) - sign) * quantCoeff[offset][0] + addition) >> qbits, 2063) ^ sign) - sign;
            }
        } else {
            for (int i = 0; i < 16; i++) {
                final int sign = (coeffs[i] >> 31);
                coeffs[i] = (((((coeffs[i] ^ sign) - sign) * quantCoeff[offset][0] + addition) >> qbits) ^ sign) - sign;
            }
        }
    }

    /**
     * Inverse Hadamard 2x2
     */
    public static void invDC2x2(final int[] block) {
        final int t0;
        final int t1;
        final int t2;
        final int t3;

        t0 = block[0] + block[1];
        t1 = block[0] - block[1];
        t2 = block[2] + block[3];
        t3 = block[2] - block[3];

        block[0] = (t0 + t2);
        block[1] = (t1 + t3);
        block[2] = (t0 - t2);
        block[3] = (t1 - t3);
    }

    /**
     * Forward Hadamard 2x2
     */
    public static void fvdDC2x2(final int[] block) {
        invDC2x2(block);
    }

    public static void dequantizeDC2x2(final int[] transformed, final int qp, final int[] scalingList) {
        final int group = qp % 6;

        if (scalingList == null) {
            final int shift = qp / 6;

            for (int i = 0; i < 4; i++) {
                transformed[i] = ((transformed[i] * dequantCoef[group][0]) << shift) >> 1;
            }
        } else {
            if (qp >= 24) {
                final int qbits = qp / 6 - 4;
                for (int i = 0; i < 4; i++) {
                    transformed[i] = ((transformed[i] * dequantCoef[group][0] * scalingList[0]) << qbits) >> 1;
                }
            } else {
                final int qbits = 4 - qp / 6;
                final int addition = 1 << (3 - qp / 6);
                for (int i = 0; i < 4; i++) {
                    transformed[i] = ((transformed[i] * dequantCoef[group][0] * scalingList[0] + addition) >> qbits) >> 1;
                }
            }
        }
    }

    public static void quantizeDC2x2(final int[] coeffs, final int qp) {
        final int level = qp / 6;
        final int offset = qp % 6;

        final int addition = 682 << (qp / 6 + 5);
        final int qbits = 16 + level;

        if (qp < 4) {
            for (int i = 0; i < 4; i++) {
                final int sign = (coeffs[i] >> 31);
                coeffs[i] = (Math.min((((coeffs[i] ^ sign) - sign) * quantCoeff[offset][0] + addition) >> qbits, 2063) ^ sign) - sign;
            }
        } else {
            for (int i = 0; i < 4; i++) {
                final int sign = (coeffs[i] >> 31);
                coeffs[i] = (((((coeffs[i] ^ sign) - sign) * quantCoeff[offset][0] + addition) >> qbits) ^ sign) - sign;
            }
        }
    }

    public static void reorderDC4x4(final int[] dc) {
        ArrayUtil.swap(dc, 2, 4);
        ArrayUtil.swap(dc, 3, 5);
        ArrayUtil.swap(dc, 10, 12);
        ArrayUtil.swap(dc, 11, 13);
    }

    public static void fvdDC4x2() {
    }

    public static void quantizeDC4x2() {
    }

    public static void invDC4x2() {
    }

    public static void dequantizeDC4x2() {
    }

    /**
     * Coefficients are <<4 on exit
     */
    public static void dequantizeAC8x8(final int[] coeffs, final int qp, final int[] scalingList) {
        final int group = qp % 6;

        if (qp >= 36) {
            final int qbits = qp / 6 - 6;
            if (scalingList == null) {
                for (int i = 0; i < 64; i++)
                    coeffs[i] = (coeffs[i] * (dequantCoef8x8[group][i]) << 4) << qbits;
            } else {
                for (int i = 0; i < 64; i++)
                    coeffs[i] = (coeffs[i] * dequantCoef8x8[group][i] * scalingList[invZigzag8x8[i]]) << qbits;
            }
        } else {
            final int qbits = 6 - qp / 6;
            final int addition = 1 << (5 - qp / 6);
            if (scalingList == null) {
                for (int i = 0; i < 64; i++)
                    coeffs[i] = (coeffs[i] * (dequantCoef8x8[group][i] << 4) + addition) >> qbits;
            } else {
                for (int i = 0; i < 64; i++)
                    coeffs[i] = (coeffs[i] * dequantCoef8x8[group][i] * scalingList[invZigzag8x8[i]] + addition) >> qbits;
            }
        }
    }

    public static void idct8x8(final int[] ac) {
        int off = 0;

        // Horizontal
        for (int row = 0; row < 8; row++) {
            final int e0 = ac[off] + ac[off + 4];
            final int e1 = -ac[off + 3] + ac[off + 5] - ac[off + 7] - (ac[off + 7] >> 1);
            final int e2 = ac[off] - ac[off + 4];
            final int e3 = ac[off + 1] + ac[off + 7] - ac[off + 3] - (ac[off + 3] >> 1);
            final int e4 = (ac[off + 2] >> 1) - ac[off + 6];
            final int e5 = -ac[off + 1] + ac[off + 7] + ac[off + 5] + (ac[off + 5] >> 1);
            final int e6 = ac[off + 2] + (ac[off + 6] >> 1);
            final int e7 = ac[off + 3] + ac[off + 5] + ac[off + 1] + (ac[off + 1] >> 1);

            final int f0 = e0 + e6;
            final int f1 = e1 + (e7 >> 2);
            final int f2 = e2 + e4;
            final int f3 = e3 + (e5 >> 2);
            final int f4 = e2 - e4;
            final int f5 = (e3 >> 2) - e5;
            final int f6 = e0 - e6;
            final int f7 = e7 - (e1 >> 2);

            ac[off] = f0 + f7;
            ac[off + 1] = f2 + f5;
            ac[off + 2] = f4 + f3;
            ac[off + 3] = f6 + f1;
            ac[off + 4] = f6 - f1;
            ac[off + 5] = f4 - f3;
            ac[off + 6] = f2 - f5;
            ac[off + 7] = f0 - f7;

            off += 8;
        }

        // Vertical
        for (int col = 0; col < 8; col++) {
            final int e0 = ac[col] + ac[col + 32];
            final int e1 = -ac[col + 24] + ac[col + 40] - ac[col + 56] - (ac[col + 56] >> 1);
            final int e2 = ac[col] - ac[col + 32];
            final int e3 = ac[col + 8] + ac[col + 56] - ac[col + 24] - (ac[col + 24] >> 1);
            final int e4 = (ac[col + 16] >> 1) - ac[col + 48];
            final int e5 = -ac[col + 8] + ac[col + 56] + ac[col + 40] + (ac[col + 40] >> 1);
            final int e6 = ac[col + 16] + (ac[col + 48] >> 1);
            final int e7 = ac[col + 24] + ac[col + 40] + ac[col + 8] + (ac[col + 8] >> 1);

            final int f0 = e0 + e6;
            final int f1 = e1 + (e7 >> 2);
            final int f2 = e2 + e4;
            final int f3 = e3 + (e5 >> 2);
            final int f4 = e2 - e4;
            final int f5 = (e3 >> 2) - e5;
            final int f6 = e0 - e6;
            final int f7 = e7 - (e1 >> 2);

            ac[col] = f0 + f7;
            ac[col + 8] = f2 + f5;
            ac[col + 16] = f4 + f3;
            ac[col + 24] = f6 + f1;
            ac[col + 32] = f6 - f1;
            ac[col + 40] = f4 - f3;
            ac[col + 48] = f2 - f5;
            ac[col + 56] = f0 - f7;
        }

        // scale down
        for (int i = 0; i < 64; i++) {
            ac[i] = (ac[i] + 32) >> 6;
        }
    }
}
