/* SPDX-License-Identifier: BSD 2-Clause "Simplified" License */

package li.cil.oc2.jcodec.codecs.h264.encode;

import li.cil.oc2.jcodec.codecs.h264.decode.deblock.DeblockingFilter;
import li.cil.oc2.jcodec.common.model.Picture;
import li.cil.oc2.jcodec.common.tools.MathUtil;

import static java.lang.Math.abs;
import static li.cil.oc2.jcodec.codecs.h264.H264Const.QP_SCALE_CR;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * <p>
 * Contains various deblocking filter routines for deblocking on MB bases
 *
 * @author Stan Vitvitskyy
 */
public final class MBDeblocker {
    static final int[][] LOOKUP_IDX_P_V = new int[][]{
        {3, 7, 11, 15},
        {0, 4, 8, 12},
        {1, 5, 9, 13},
        {2, 6, 10, 14}
    };
    static final int[][] LOOKUP_IDX_Q_V = new int[][]{
        {0, 4, 8, 12},
        {1, 5, 9, 13},
        {2, 6, 10, 14},
        {3, 7, 11, 15}
    };
    static final int[][] LOOKUP_IDX_P_H = new int[][]{
        {12, 13, 14, 15},
        {0, 1, 2, 3},
        {4, 5, 6, 7},
        {8, 9, 10, 11}
    };
    static final int[][] LOOKUP_IDX_Q_H = new int[][]{
        {0, 1, 2, 3},
        {4, 5, 6, 7},
        {8, 9, 10, 11},
        {12, 13, 14, 15}
    };

    private static int calcQpChroma(final int qp) {
        return QP_SCALE_CR[MathUtil.clip(qp, 0, 51)];
    }

    /**
     * Deblocks bottom edge of topOutMB, right edge of leftOutMB and left/top and
     * inner block edges of outMB
     *
     * @param curMB         Pixels of the current MB
     * @param leftMB        Pixels of the leftMB
     * @param topMB         Pixels of the tipMB
     * @param vertStrength  Border strengths for vertical edges (filtered first)
     * @param horizStrength Border strengths for the horizontal edges
     */
    public void deblockMBGeneric(final EncodedMB curMB, final EncodedMB leftMB, final EncodedMB topMB, final int[][] vertStrength, final int[][] horizStrength) {
        final Picture curPix = curMB.getPixels();

        final int curChQp = calcQpChroma(curMB.getQp());
        if (leftMB != null) {
            final Picture leftPix = leftMB.getPixels();

            final int leftChQp = calcQpChroma(leftMB.getQp());
            final int avgQp = MathUtil.clip((leftMB.getQp() + curMB.getQp() + 1) >> 1, 0, 51);
            final int avgChQp = MathUtil.clip((leftChQp + curChQp + 1) >> 1, 0, 51);

            deblockBorder(vertStrength[0], avgQp, leftPix.getPlaneData(0), 3, curPix.getPlaneData(0), 0, P_POS_V, Q_POS_V, false);
            deblockBorderChroma(vertStrength[0], avgChQp, leftPix.getPlaneData(1), 3, curPix.getPlaneData(1), 0, P_POS_V_CHR, Q_POS_V_CHR, false);
            deblockBorderChroma(vertStrength[0], avgChQp, leftPix.getPlaneData(2), 3, curPix.getPlaneData(2), 0, P_POS_V_CHR, Q_POS_V_CHR, false);
        }
        for (int i = 1; i < 4; i++) {
            deblockBorder(vertStrength[i], curMB.getQp(), curPix.getPlaneData(0), i - 1, curPix.getPlaneData(0), i, P_POS_V, Q_POS_V, false);
        }
        deblockBorderChroma(vertStrength[2], curChQp, curPix.getPlaneData(1), 1, curPix.getPlaneData(1), 2, P_POS_V_CHR, Q_POS_V_CHR, false);
        deblockBorderChroma(vertStrength[2], curChQp, curPix.getPlaneData(2), 1, curPix.getPlaneData(2), 2, P_POS_V_CHR, Q_POS_V_CHR, false);

        if (topMB != null) {
            final Picture topPix = topMB.getPixels();

            final int topChQp = calcQpChroma(topMB.getQp());
            final int avgQp = MathUtil.clip((topMB.getQp() + curMB.getQp() + 1) >> 1, 0, 51);
            final int avgChQp = MathUtil.clip((topChQp + curChQp + 1) >> 1, 0, 51);

            deblockBorder(horizStrength[0], avgQp, topPix.getPlaneData(0), 3, curPix.getPlaneData(0), 0, P_POS_H, Q_POS_H, true);
            deblockBorderChroma(horizStrength[0], avgChQp, topPix.getPlaneData(1), 3, curPix.getPlaneData(1), 0, P_POS_H_CHR, Q_POS_H_CHR, true);
            deblockBorderChroma(horizStrength[0], avgChQp, topPix.getPlaneData(2), 3, curPix.getPlaneData(2), 0, P_POS_H_CHR, Q_POS_H_CHR, true);
        }
        for (int i = 1; i < 4; i++) {
            deblockBorder(horizStrength[i], curMB.getQp(), curPix.getPlaneData(0), i - 1, curPix.getPlaneData(0), i, P_POS_H, Q_POS_H, true);
        }
        deblockBorderChroma(horizStrength[2], curChQp, curPix.getPlaneData(1), 1, curPix.getPlaneData(1), 2, P_POS_H_CHR, Q_POS_H_CHR, true);
        deblockBorderChroma(horizStrength[2], curChQp, curPix.getPlaneData(2), 1, curPix.getPlaneData(2), 2, P_POS_H_CHR, Q_POS_H_CHR, true);
    }

    /**
     * @param cur  Pixels and parameters of encoded and reconstructed current
     *             macroblock
     * @param left Pixels and parameters of encoded and reconstructed left
     *             macroblock
     * @param top  Pixels and parameters of encoded and reconstructed top macroblock
     */
    public void deblockMBP(final EncodedMB cur, final EncodedMB left, final EncodedMB top) {
        final int[][] vertStrength = new int[4][4];
        final int[][] horizStrength = new int[4][4];

        calcStrengthForBlocks(cur, left, vertStrength, LOOKUP_IDX_P_V, LOOKUP_IDX_Q_V);
        calcStrengthForBlocks(cur, top, horizStrength, LOOKUP_IDX_P_H, LOOKUP_IDX_Q_H);

        deblockMBGeneric(cur, left, top, vertStrength, horizStrength);
    }

    private void deblockBorder(final int[] boundary, final int qp, final byte[] p, final int pi, final byte[] q, final int qi, final int[][] pTab, final int[][] qTab,
                               final boolean horiz) {
        final int inc1 = horiz ? 16 : 1;
        final int inc2 = inc1 * 2;
        final int inc3 = inc1 * 3;
        for (int b = 0; b < 4; b++) {
            if (boundary[b] == 4) {
                for (int i = 0, ii = b << 2; i < 4; ++i, ++ii)
                    filterBs4(qp, qp, p, q, pTab[pi][ii] - inc3, pTab[pi][ii] - inc2, pTab[pi][ii] - inc1, pTab[pi][ii],
                        qTab[qi][ii], qTab[qi][ii] + inc1, qTab[qi][ii] + inc2, qTab[qi][ii] + inc3);
            } else if (boundary[b] > 0) {
                for (int i = 0, ii = b << 2; i < 4; ++i, ++ii)
                    filterBs(boundary[b], qp, qp, p, q, pTab[pi][ii] - inc2, pTab[pi][ii] - inc1, pTab[pi][ii],
                        qTab[qi][ii], qTab[qi][ii] + inc1, qTab[qi][ii] + inc2);

            }
        }
    }

    private void filterBs4Chr(final int indexAlpha, final int indexBeta, final byte[] pelsP, final byte[] pelsQ,
                              final int p1Idx, final int p0Idx, final int q0Idx, final int q1Idx) {
        _filterBs4(indexAlpha, indexBeta, pelsP, pelsQ, -1, -1, p1Idx, p0Idx, q0Idx, q1Idx, -1, -1, true);
    }

    private void filterBsChr(final int bs, final int indexAlpha, final int indexBeta, final byte[] pelsP, final byte[] pelsQ,
                             final int p1Idx, final int p0Idx, final int q0Idx, final int q1Idx) {
        _filterBs(bs, indexAlpha, indexBeta, pelsP, pelsQ, -1, p1Idx, p0Idx, q0Idx, q1Idx, -1, true);
    }

    private void filterBs4(final int indexAlpha, final int indexBeta, final byte[] pelsP, final byte[] pelsQ,
                           final int p3Idx, final int p2Idx, final int p1Idx, final int p0Idx, final int q0Idx,
                           final int q1Idx, final int q2Idx, final int q3Idx) {
        _filterBs4(indexAlpha, indexBeta, pelsP, pelsQ, p3Idx, p2Idx, p1Idx, p0Idx, q0Idx, q1Idx, q2Idx, q3Idx, false);
    }

    private void filterBs(final int bs, final int indexAlpha, final int indexBeta, final byte[] pelsP, final byte[] pelsQ,
                          final int p2Idx, final int p1Idx, final int p0Idx, final int q0Idx, final int q1Idx, final int q2Idx) {
        _filterBs(bs, indexAlpha, indexBeta, pelsP, pelsQ, p2Idx, p1Idx, p0Idx, q0Idx, q1Idx, q2Idx, false);
    }

    private void _filterBs4(final int indexAlpha, final int indexBeta, final byte[] pelsP, final byte[] pelsQ,
                            final int p3Idx, final int p2Idx, final int p1Idx, final int p0Idx, final int q0Idx,
                            final int q1Idx, final int q2Idx, final int q3Idx, final boolean isChroma) {
        final int p0 = pelsP[p0Idx];
        final int q0 = pelsQ[q0Idx];
        final int p1 = pelsP[p1Idx];
        final int q1 = pelsQ[q1Idx];

        final int alphaThresh = DeblockingFilter.alphaTab[indexAlpha];
        final int betaThresh = DeblockingFilter.betaTab[indexBeta];

        final boolean filterEnabled = abs(p0 - q0) < alphaThresh && abs(p1 - p0) < betaThresh && abs(q1 - q0) < betaThresh;

        if (!filterEnabled)
            return;

        final boolean conditionP;
        final boolean conditionQ;

        if (isChroma) {
            conditionP = false;
            conditionQ = false;
        } else {
            final int ap = abs(pelsP[p2Idx] - p0);
            final int aq = abs(pelsQ[q2Idx] - q0);

            conditionP = ap < betaThresh && abs(p0 - q0) < ((alphaThresh >> 2) + 2);
            conditionQ = aq < betaThresh && abs(p0 - q0) < ((alphaThresh >> 2) + 2);
        }

        if (conditionP) {
            final int p3 = pelsP[p3Idx];
            final int p2 = pelsP[p2Idx];

            final int p0n = (p2 + 2 * p1 + 2 * p0 + 2 * q0 + q1 + 4) >> 3;
            final int p1n = (p2 + p1 + p0 + q0 + 2) >> 2;
            final int p2n = (2 * p3 + 3 * p2 + p1 + p0 + q0 + 4) >> 3;
            pelsP[p0Idx] = (byte) MathUtil.clip(p0n, -128, 127);
            pelsP[p1Idx] = (byte) MathUtil.clip(p1n, -128, 127);
            pelsP[p2Idx] = (byte) MathUtil.clip(p2n, -128, 127);
        } else {
            final int p0n = (2 * p1 + p0 + q1 + 2) >> 2;
            pelsP[p0Idx] = (byte) MathUtil.clip(p0n, -128, 127);
        }

        if (conditionQ) {
            final int q2 = pelsQ[q2Idx];
            final int q3 = pelsQ[q3Idx];
            final int q0n = (p1 + 2 * p0 + 2 * q0 + 2 * q1 + q2 + 4) >> 3;
            final int q1n = (p0 + q0 + q1 + q2 + 2) >> 2;
            final int q2n = (2 * q3 + 3 * q2 + q1 + q0 + p0 + 4) >> 3;
            pelsQ[q0Idx] = (byte) MathUtil.clip(q0n, -128, 127);
            pelsQ[q1Idx] = (byte) MathUtil.clip(q1n, -128, 127);
            pelsQ[q2Idx] = (byte) MathUtil.clip(q2n, -128, 127);
        } else {
            final int q0n = (2 * q1 + q0 + p1 + 2) >> 2;
            pelsQ[q0Idx] = (byte) MathUtil.clip(q0n, -128, 127);
        }
    }

    private void _filterBs(final int bs, final int indexAlpha, final int indexBeta, final byte[] pelsP, final byte[] pelsQ, final int p2Idx, final int p1Idx,
                           final int p0Idx, final int q0Idx, final int q1Idx, final int q2Idx, final boolean isChroma) {
        final int p1 = pelsP[p1Idx];
        final int p0 = pelsP[p0Idx];
        final int q0 = pelsQ[q0Idx];
        final int q1 = pelsQ[q1Idx];

        final int alphaThresh = DeblockingFilter.alphaTab[indexAlpha];
        final int betaThresh = DeblockingFilter.betaTab[indexBeta];

        final boolean filterEnabled = abs(p0 - q0) < alphaThresh && abs(p1 - p0) < betaThresh && abs(q1 - q0) < betaThresh;

        if (!filterEnabled)
            return;

        final int tC0 = DeblockingFilter.tcs[bs - 1][indexAlpha];

        final boolean conditionP;
        final boolean conditionQ;
        final int tC;
        if (!isChroma) {
            final int ap = abs(pelsP[p2Idx] - p0);
            final int aq = abs(pelsQ[q2Idx] - q0);
            tC = tC0 + ((ap < betaThresh) ? 1 : 0) + ((aq < betaThresh) ? 1 : 0);
            conditionP = ap < betaThresh;
            conditionQ = aq < betaThresh;
        } else {
            tC = tC0 + 1;
            conditionP = false;
            conditionQ = false;
        }

        int sigma = ((((q0 - p0) << 2) + (p1 - q1) + 4) >> 3);
        sigma = sigma < -tC ? -tC : Math.min(sigma, tC);

        int p0n = p0 + sigma;
        p0n = Math.max(p0n, -128);
        int q0n = q0 - sigma;
        q0n = Math.max(q0n, -128);

        if (conditionP) {
            final int p2 = pelsP[p2Idx];

            int diff = (p2 + ((p0 + q0 + 1) >> 1) - (p1 << 1)) >> 1;
            diff = diff < -tC0 ? -tC0 : Math.min(diff, tC0);
            final int p1n = p1 + diff;
            pelsP[p1Idx] = (byte) MathUtil.clip(p1n, -128, 127);
        }

        if (conditionQ) {
            final int q2 = pelsQ[q2Idx];
            int diff = (q2 + ((p0 + q0 + 1) >> 1) - (q1 << 1)) >> 1;
            diff = diff < -tC0 ? -tC0 : Math.min(diff, tC0);
            final int q1n = q1 + diff;
            pelsQ[q1Idx] = (byte) MathUtil.clip(q1n, -128, 127);
        }

        pelsQ[q0Idx] = (byte) MathUtil.clip(q0n, -128, 127);
        pelsP[p0Idx] = (byte) MathUtil.clip(p0n, -128, 127);
    }

    private void deblockBorderChroma(final int[] boundary, final int qp, final byte[] p, final int pi, final byte[] q, final int qi, final int[][] pTab,
                                     final int[][] qTab, final boolean horiz) {
        final int inc1 = horiz ? 8 : 1;
        for (int b = 0; b < 4; b++) {
            if (boundary[b] == 4) {
                for (int i = 0, ii = b << 1; i < 2; ++i, ++ii)
                    filterBs4Chr(qp, qp, p, q, pTab[pi][ii] - inc1, pTab[pi][ii], qTab[qi][ii], qTab[qi][ii] + inc1);
            } else if (boundary[b] > 0) {
                for (int i = 0, ii = b << 1; i < 2; ++i, ++ii)
                    filterBsChr(boundary[b], qp, qp, p, q, pTab[pi][ii] - inc1, pTab[pi][ii], qTab[qi][ii], qTab[qi][ii] + inc1);
            }
        }
    }

    private static int[][] buildPPosH() {
        final int[][] qPos = new int[4][16];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 16; j++) {
                qPos[i][j] = j + (i << 6) + 48;
            }
        }
        return qPos;
    }

    private static int[][] buildQPosH() {
        final int[][] pPos = new int[4][16];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 16; j++) {
                pPos[i][j] = j + (i << 6);
            }
        }
        return pPos;
    }

    private static int[][] buildPPosV() {
        final int[][] qPos = new int[4][16];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 16; j++) {
                qPos[i][j] = (j << 4) + (i << 2) + 3;
            }
        }
        return qPos;
    }

    private static int[][] buildQPosV() {
        final int[][] pPos = new int[4][16];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 16; j++) {
                pPos[i][j] = (j << 4) + (i << 2);
            }
        }
        return pPos;
    }

    private static int[][] buildPPosHChr() {
        final int[][] qPos = new int[4][8];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 8; j++) {
                qPos[i][j] = j + (i << 4) + 8;
            }
        }
        return qPos;
    }

    private static int[][] buildQPosHChr() {
        final int[][] pPos = new int[4][8];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 8; j++) {
                pPos[i][j] = j + (i << 4);
            }
        }
        return pPos;
    }

    private static int[][] buildPPosVChr() {
        final int[][] qPos = new int[4][8];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 8; j++) {
                qPos[i][j] = (j << 3) + (i << 1) + 1;
            }
        }
        return qPos;
    }

    private static int[][] buildQPosVChr() {
        final int[][] pPos = new int[4][8];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 8; j++) {
                pPos[i][j] = (j << 3) + (i << 1);
            }
        }
        return pPos;
    }

    static void calcStrengthForBlocks(final EncodedMB cur, final EncodedMB other, final int[][] outStrength, final int[][] LOOKUP_IDX_P,
                                      final int[][] LOOKUP_IDX_Q) {
        final boolean thisIntra = cur.getType().isIntra();
        if (other != null) {
            final boolean otherIntra = other.getType().isIntra();
            for (int i = 0; i < 4; ++i) {
                final int bsMvx = strengthMv(other.getMx()[LOOKUP_IDX_P[0][i]], cur.getMx()[LOOKUP_IDX_Q[0][i]]);
                final int bsMvy = strengthMv(other.getMy()[LOOKUP_IDX_P[0][i]], cur.getMy()[LOOKUP_IDX_Q[0][i]]);
                final int bsNc = strengthNc(other.getNc()[LOOKUP_IDX_P[0][i]], cur.getNc()[LOOKUP_IDX_Q[0][i]]);
                final int max3 = MathUtil.max3(bsMvx, bsMvy, bsNc);
                outStrength[0][i] = (otherIntra || thisIntra) ? 4 : max3;
            }
        }

        for (int i = 1; i < 4; i++) {
            for (int j = 0; j < 4; ++j) {
                final int bsMvx = strengthMv(cur.getMx()[LOOKUP_IDX_P[i][j]], cur.getMx()[LOOKUP_IDX_Q[i][j]]);
                final int bsMvy = strengthMv(cur.getMy()[LOOKUP_IDX_P[i][j]], cur.getMy()[LOOKUP_IDX_Q[i][j]]);
                final int bsNc = strengthNc(cur.getNc()[LOOKUP_IDX_P[i][j]], cur.getNc()[LOOKUP_IDX_Q[i][j]]);
                final int max3 = MathUtil.max3(bsMvx, bsMvy, bsNc);
                outStrength[i][j] = thisIntra ? 3 : max3;
            }
        }
    }

    private static int strengthNc(final int ncA, final int ncB) {
        return ncA > 0 || ncB > 0 ? 2 : 0;
    }

    private static int strengthMv(final int v0, final int v1) {
        return abs(v0 - v1) >= 4 ? 1 : 0;
    }

    private static final int[][] P_POS_V = buildPPosV();
    private static final int[][] Q_POS_V = buildQPosV();
    private static final int[][] P_POS_H = buildPPosH();
    private static final int[][] Q_POS_H = buildQPosH();

    private static final int[][] P_POS_V_CHR = buildPPosVChr();
    private static final int[][] Q_POS_V_CHR = buildQPosVChr();

    private static final int[][] P_POS_H_CHR = buildPPosHChr();
    private static final int[][] Q_POS_H_CHR = buildQPosHChr();
}
