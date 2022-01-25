/* SPDX-License-Identifier: BSD 2-Clause "Simplified" License */

package li.cil.oc2.jcodec.codecs.h264.decode.deblock;

import li.cil.oc2.jcodec.codecs.h264.decode.DeblockerInput;
import li.cil.oc2.jcodec.codecs.h264.io.model.SliceHeader;
import li.cil.oc2.jcodec.common.model.ColorSpace;
import li.cil.oc2.jcodec.common.model.Picture;
import li.cil.oc2.jcodec.common.tools.MathUtil;

import static java.lang.Math.abs;
import static li.cil.oc2.jcodec.codecs.h264.H264Utils.Mv.*;
import static li.cil.oc2.jcodec.common.tools.MathUtil.clip;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * <p>
 * A filter that removes DCT artifacts on block boundaries.
 * <p>
 * It's operation is dependant on QP and is designed the way that the strenth is
 * adjusted to the likelyhood of appearence of blocking artifacts on the
 * specific edges.
 * <p>
 * Builds a parameter for deblocking filter based on the properties of specific
 * macroblocks.
 * <p>
 * A parameter specifies the behavior of deblocking filter on each of 8 edges
 * that need to filtered for a macroblock.
 * <p>
 * For each edge the following things are evaluated on it's both sides: presence
 * of DCT coded residual; motion vector difference; spatial location.
 *
 * @author The JCodec project
 */
public final class DeblockingFilter {
    public static final int[] alphaTab = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 4, 5, 6, 7, 8, 9, 10, 12, 13, 15, 17, 20, 22, 25, 28, 32, 36, 40, 45, 50, 56, 63, 71, 80, 90, 101, 113, 127, 144, 162, 182, 203, 226, 255, 255};
    public static final int[] betaTab = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 6, 6, 7, 7, 8, 8, 9, 9, 10, 10, 11, 11, 12, 12, 13, 13, 14, 14, 15, 15, 16, 16, 17, 17, 18, 18};

    public static final int[][] tcs = new int[][]{
        new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 4, 4, 4, 5, 6, 6, 7, 8, 9, 10, 11, 13},
        new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 4, 4, 5, 5, 6, 7, 8, 8, 10, 11, 12, 13, 15, 17},
        new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 4, 4, 4, 5, 6, 6, 7, 8, 9, 10, 11, 13, 14, 16, 18, 20, 23, 25}
    };

    private final DeblockerInput di;

    public DeblockingFilter(final DeblockerInput di) {
        this.di = di;
    }

    public void deblockFrame(final Picture result) {
        final ColorSpace color = result.getColor();
        final int[][] bsV = new int[4][4];
        final int[][] bsH = new int[4][4];
        for (int i = 0; i < di.shs.length; i++) {
            calcBsH(i, bsH);
            calcBsV(i, bsV);

            for (int c = 0; c < color.nComp; c++) {
                fillVerticalEdge(result, c, i, bsV);
                fillHorizontalEdge(result, c, i, bsH);
            }
        }
    }

    private int calcBoundaryStrenth(final boolean atMbBoundary, final boolean leftIntra, final boolean rightIntra, final int leftCoeff,
                                    final int rightCoeff, final int mvA0, final int mvB0, final int mvA1, final int mvB1, final int mbAddrA, final int mbAddrB) {
        if (atMbBoundary && (leftIntra || rightIntra))
            return 4;
        else if (leftIntra || rightIntra)
            return 3;
        else {
            if (leftCoeff > 0 || rightCoeff > 0)
                return 2;

            final int nA = (mvRef(mvA0) == -1 ? 0 : 1) + (mvRef(mvA1) == -1 ? 0 : 1);
            final int nB = (mvRef(mvB0) == -1 ? 0 : 1) + (mvRef(mvB1) == -1 ? 0 : 1);

            if (nA != nB)
                return 1;

            final Picture ra0 = mvRef(mvA0) < 0 ? null : di.refsUsed[mbAddrA][0][mvRef(mvA0)];
            final Picture ra1 = mvRef(mvA1) < 0 ? null : di.refsUsed[mbAddrA][1][mvRef(mvA1)];

            final Picture rb0 = mvRef(mvB0) < 0 ? null : di.refsUsed[mbAddrB][0][mvRef(mvB0)];
            final Picture rb1 = mvRef(mvB1) < 0 ? null : di.refsUsed[mbAddrB][1][mvRef(mvB1)];

            if (ra0 != rb0 && ra0 != rb1 || ra1 != rb0 && ra1 != rb1 || rb0 != ra0 && rb0 != ra1 || rb1 != ra0 && rb1 != ra1)
                return 1;

            if (ra0 == ra1 && ra1 == rb0 && rb0 == rb1) {
                return ra0 != null && (mvThresh(mvA0, mvB0) || mvThresh(mvA1, mvB0) || mvThresh(mvA0, mvB1) || mvThresh(mvA1, mvB1)) ? 1 : 0;
            } else if (ra0 == rb0 && ra1 == rb1) {
                return ra0 != null && mvThresh(mvA0, mvB0) || ra1 != null && mvThresh(mvA1, mvB1) ? 1 : 0;
            } else if (ra0 == rb1 && ra1 == rb0) {
                return ra0 != null && mvThresh(mvA0, mvB1) || ra1 != null && mvThresh(mvA1, mvB0) ? 1 : 0;
            }
        }

        return 0;
    }

    private boolean mvThresh(final int v0, final int v1) {
        return abs(mvX(v0) - mvX(v1)) >= 4 || abs(mvY(v0) - mvY(v1)) >= 4;
    }

    private static int getIdxBeta(final int sliceBetaOffset, final int avgQp) {
        return MathUtil.clip(avgQp + sliceBetaOffset, 0, 51);
    }

    private static int getIdxAlpha(final int sliceAlphaC0Offset, final int avgQp) {
        return MathUtil.clip(avgQp + sliceAlphaC0Offset, 0, 51);
    }

    private void calcBsH(final int mbAddr, final int[][] bs) {
        final SliceHeader sh = di.shs[mbAddr];
        final int mbWidth = sh.sps.picWidthInMbsMinus1 + 1;

        final int mbX = mbAddr % mbWidth;
        final int mbY = mbAddr / mbWidth;

        final boolean topAvailable = mbY > 0 && (sh.disableDeblockingFilterIdc != 2 || di.shs[mbAddr - mbWidth] == sh);
        final boolean thisIntra = di.mbTypes[mbAddr] != null && di.mbTypes[mbAddr].isIntra();

        if (topAvailable) {
            final boolean topIntra = di.mbTypes[mbAddr - mbWidth] != null && di.mbTypes[mbAddr - mbWidth].isIntra();
            for (int blkX = 0; blkX < 4; blkX++) {
                final int thisBlkX = (mbX << 2) + blkX;
                final int thisBlkY = (mbY << 2);

                bs[0][blkX] = calcBoundaryStrenth(true, topIntra, thisIntra, di.nCoeff[thisBlkY][thisBlkX],
                    di.nCoeff[thisBlkY - 1][thisBlkX], di.mvs.getMv(thisBlkX, thisBlkY, 0),
                    di.mvs.getMv(thisBlkX, thisBlkY - 1, 0), di.mvs.getMv(thisBlkX, thisBlkY, 1),
                    di.mvs.getMv(thisBlkX, thisBlkY - 1, 1), mbAddr, mbAddr - mbWidth);

            }
        }

        for (int blkY = 1; blkY < 4; blkY++) {
            for (int blkX = 0; blkX < 4; blkX++) {
                final int thisBlkX = (mbX << 2) + blkX;
                final int thisBlkY = (mbY << 2) + blkY;

                bs[blkY][blkX] = calcBoundaryStrenth(false, thisIntra, thisIntra, di.nCoeff[thisBlkY][thisBlkX],
                    di.nCoeff[thisBlkY - 1][thisBlkX], di.mvs.getMv(thisBlkX, thisBlkY, 0),
                    di.mvs.getMv(thisBlkX, thisBlkY - 1, 0), di.mvs.getMv(thisBlkX, thisBlkY, 1),
                    di.mvs.getMv(thisBlkX, thisBlkY - 1, 1), mbAddr, mbAddr);
            }
        }
    }

    private void fillHorizontalEdge(final Picture pic, final int comp, final int mbAddr, final int[][] bs) {
        final SliceHeader sh = di.shs[mbAddr];
        final int mbWidth = sh.sps.picWidthInMbsMinus1 + 1;

        final int alpha = sh.sliceAlphaC0OffsetDiv2 << 1;
        final int beta = sh.sliceBetaOffsetDiv2 << 1;

        final int mbX = mbAddr % mbWidth;
        final int mbY = mbAddr / mbWidth;

        final boolean topAvailable = mbY > 0 && (sh.disableDeblockingFilterIdc != 2 || di.shs[mbAddr - mbWidth] == sh);
        final int curQp = di.mbQps[comp][mbAddr];

        final int cW = 2 - pic.getColor().compWidth[comp];
        final int cH = 2 - pic.getColor().compHeight[comp];
        if (topAvailable) {
            final int topQp = di.mbQps[comp][mbAddr - mbWidth];
            final int avgQp = (topQp + curQp + 1) >> 1;
            for (int blkX = 0; blkX < 4; blkX++) {
                final int thisBlkX = (mbX << 2) + blkX;
                final int thisBlkY = (mbY << 2);

                filterBlockEdgeHoris(pic, comp, thisBlkX << cW, thisBlkY << cH, getIdxAlpha(alpha, avgQp),
                    getIdxBeta(beta, avgQp), bs[0][blkX], 1 << cW);
            }
        }

        final boolean skip4x4 = comp == 0 && di.tr8x8Used[mbAddr] || cH == 1;

        for (int blkY = 1; blkY < 4; blkY++) {
            if (skip4x4 && (blkY & 1) == 1)
                continue;

            for (int blkX = 0; blkX < 4; blkX++) {
                final int thisBlkX = (mbX << 2) + blkX;
                final int thisBlkY = (mbY << 2) + blkY;

                filterBlockEdgeHoris(pic, comp, thisBlkX << cW, thisBlkY << cH, getIdxAlpha(alpha, curQp),
                    getIdxBeta(beta, curQp), bs[blkY][blkX], 1 << cW);
            }
        }
    }

    private void calcBsV(final int mbAddr, final int[][] bs) {
        final SliceHeader sh = di.shs[mbAddr];
        final int mbWidth = sh.sps.picWidthInMbsMinus1 + 1;

        final int mbX = mbAddr % mbWidth;
        final int mbY = mbAddr / mbWidth;

        final boolean leftAvailable = mbX > 0 && (sh.disableDeblockingFilterIdc != 2 || di.shs[mbAddr - 1] == sh);
        final boolean thisIntra = di.mbTypes[mbAddr] != null && di.mbTypes[mbAddr].isIntra();

        if (leftAvailable) {
            final boolean leftIntra = di.mbTypes[mbAddr - 1] != null && di.mbTypes[mbAddr - 1].isIntra();
            for (int blkY = 0; blkY < 4; blkY++) {
                final int thisBlkX = (mbX << 2);
                final int thisBlkY = (mbY << 2) + blkY;
                bs[blkY][0] = calcBoundaryStrenth(true, leftIntra, thisIntra, di.nCoeff[thisBlkY][thisBlkX],
                    di.nCoeff[thisBlkY][thisBlkX - 1], di.mvs.getMv(thisBlkX, thisBlkY, 0),
                    di.mvs.getMv(thisBlkX - 1, thisBlkY, 0), di.mvs.getMv(thisBlkX, thisBlkY, 1),
                    di.mvs.getMv(thisBlkX - 1, thisBlkY, 1), mbAddr, mbAddr - 1);
            }
        }

        for (int blkX = 1; blkX < 4; blkX++) {
            for (int blkY = 0; blkY < (1 << 2); blkY++) {
                final int thisBlkX = (mbX << 2) + blkX;
                final int thisBlkY = (mbY << 2) + blkY;
                bs[blkY][blkX] = calcBoundaryStrenth(false, thisIntra, thisIntra, di.nCoeff[thisBlkY][thisBlkX],
                    di.nCoeff[thisBlkY][thisBlkX - 1], di.mvs.getMv(thisBlkX, thisBlkY, 0),
                    di.mvs.getMv(thisBlkX - 1, thisBlkY, 0), di.mvs.getMv(thisBlkX, thisBlkY, 1),
                    di.mvs.getMv(thisBlkX - 1, thisBlkY, 1), mbAddr, mbAddr);
            }
        }
    }

    private void fillVerticalEdge(final Picture pic, final int comp, final int mbAddr, final int[][] bs) {
        final SliceHeader sh = di.shs[mbAddr];
        final int mbWidth = sh.sps.picWidthInMbsMinus1 + 1;

        final int alpha = sh.sliceAlphaC0OffsetDiv2 << 1;
        final int beta = sh.sliceBetaOffsetDiv2 << 1;

        final int mbX = mbAddr % mbWidth;
        final int mbY = mbAddr / mbWidth;

        final boolean leftAvailable = mbX > 0 && (sh.disableDeblockingFilterIdc != 2 || di.shs[mbAddr - 1] == sh);
        final int curQp = di.mbQps[comp][mbAddr];

        final int cW = 2 - pic.getColor().compWidth[comp];
        final int cH = 2 - pic.getColor().compHeight[comp];
        if (leftAvailable) {
            final int leftQp = di.mbQps[comp][mbAddr - 1];
            final int avgQpV = (leftQp + curQp + 1) >> 1;
            for (int blkY = 0; blkY < 4; blkY++) {
                final int thisBlkX = (mbX << 2);
                final int thisBlkY = (mbY << 2) + blkY;
                filterBlockEdgeVert(pic, comp, thisBlkX << cW, thisBlkY << cH, getIdxAlpha(alpha, avgQpV),
                    getIdxBeta(beta, avgQpV), bs[blkY][0], 1 << cH);
            }
        }
        final boolean skip4x4 = comp == 0 && di.tr8x8Used[mbAddr] || cW == 1;

        for (int blkX = 1; blkX < 4; blkX++) {
            if (skip4x4 && (blkX & 1) == 1)
                continue;
            for (int blkY = 0; blkY < 4; blkY++) {
                final int thisBlkX = (mbX << 2) + blkX;
                final int thisBlkY = (mbY << 2) + blkY;
                filterBlockEdgeVert(pic, comp, thisBlkX << cW, thisBlkY << cH, getIdxAlpha(alpha, curQp),
                    getIdxBeta(beta, curQp), bs[blkY][blkX], 1 << cH);
            }
        }
    }

    private void filterBlockEdgeHoris(final Picture pic, final int comp, final int x, final int y, final int indexAlpha, final int indexBeta, final int bs, final int blkW) {
        final int stride = pic.getPlaneWidth(comp);
        final int offset = y * stride + x;

        for (int pixOff = 0; pixOff < blkW; pixOff++) {
            final int p2Idx = offset - 3 * stride + pixOff;
            final int p1Idx = offset - 2 * stride + pixOff;
            final int p0Idx = offset - stride + pixOff;
            final int q0Idx = offset + pixOff;
            final int q1Idx = offset + stride + pixOff;
            final int q2Idx = offset + 2 * stride + pixOff;

            if (bs == 4) {
                final int p3Idx = offset - 4 * stride + pixOff;
                final int q3Idx = offset + 3 * stride + pixOff;

                filterBs4(indexAlpha, indexBeta, pic.getPlaneData(comp), pic.getPlaneData(comp), p3Idx, p2Idx, p1Idx,
                    p0Idx, q0Idx, q1Idx, q2Idx, q3Idx, comp != 0);
            } else if (bs > 0) {

                filterBs(bs, indexAlpha, indexBeta, pic.getPlaneData(comp), pic.getPlaneData(comp), p2Idx, p1Idx,
                    p0Idx, q0Idx, q1Idx, q2Idx, comp != 0);
            }
        }
    }

    private void filterBlockEdgeVert(final Picture pic, final int comp, final int x, final int y, final int indexAlpha, final int indexBeta, final int bs, final int blkH) {
        final int stride = pic.getPlaneWidth(comp);
        for (int i = 0; i < blkH; i++) {
            final int offsetQ = (y + i) * stride + x;
            final int p2Idx = offsetQ - 3;
            final int p1Idx = offsetQ - 2;
            final int p0Idx = offsetQ - 1;
            final int q0Idx = offsetQ;
            final int q1Idx = offsetQ + 1;
            final int q2Idx = offsetQ + 2;

            if (bs == 4) {
                final int p3Idx = offsetQ - 4;
                final int q3Idx = offsetQ + 3;
                filterBs4(indexAlpha, indexBeta, pic.getPlaneData(comp), pic.getPlaneData(comp), p3Idx, p2Idx, p1Idx,
                    p0Idx, q0Idx, q1Idx, q2Idx, q3Idx, comp != 0);
            } else if (bs > 0) {
                filterBs(bs, indexAlpha, indexBeta, pic.getPlaneData(comp), pic.getPlaneData(comp), p2Idx, p1Idx,
                    p0Idx, q0Idx, q1Idx, q2Idx, comp != 0);
            }
        }
    }

    public static void filterBs(final int bs, final int indexAlpha, final int indexBeta, final byte[] pelsP, final byte[] pelsQ, final int p2Idx, final int p1Idx,
                                final int p0Idx, final int q0Idx, final int q1Idx, final int q2Idx, final boolean isChroma) {

        final int p1 = pelsP[p1Idx];
        final int p0 = pelsP[p0Idx];
        final int q0 = pelsQ[q0Idx];
        final int q1 = pelsQ[q1Idx];

        final int alphaThresh = alphaTab[indexAlpha];
        final int betaThresh = betaTab[indexBeta];

        final boolean filterEnabled = abs(p0 - q0) < alphaThresh && abs(p1 - p0) < betaThresh && abs(q1 - q0) < betaThresh;

        if (!filterEnabled)
            return;

        final int tC0 = tcs[bs - 1][indexAlpha];

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
            pelsP[p1Idx] = (byte) clip(p1n, -128, 127);
        }

        if (conditionQ) {
            final int q2 = pelsQ[q2Idx];
            int diff = (q2 + ((p0 + q0 + 1) >> 1) - (q1 << 1)) >> 1;
            diff = diff < -tC0 ? -tC0 : Math.min(diff, tC0);
            final int q1n = q1 + diff;
            pelsQ[q1Idx] = (byte) clip(q1n, -128, 127);
        }

        pelsQ[q0Idx] = (byte) clip(q0n, -128, 127);
        pelsP[p0Idx] = (byte) clip(p0n, -128, 127);

    }

    public static void filterBs4(final int indexAlpha, final int indexBeta, final byte[] pelsP, final byte[] pelsQ, final int p3Idx, final int p2Idx,
                                 final int p1Idx, final int p0Idx, final int q0Idx, final int q1Idx, final int q2Idx, final int q3Idx, final boolean isChroma) {
        final int p0 = pelsP[p0Idx];
        final int q0 = pelsQ[q0Idx];
        final int p1 = pelsP[p1Idx];
        final int q1 = pelsQ[q1Idx];

        final int alphaThresh = alphaTab[indexAlpha];
        final int betaThresh = betaTab[indexBeta];

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
            pelsP[p0Idx] = (byte) clip(p0n, -128, 127);
            pelsP[p1Idx] = (byte) clip(p1n, -128, 127);
            pelsP[p2Idx] = (byte) clip(p2n, -128, 127);
        } else {
            final int p0n = (2 * p1 + p0 + q1 + 2) >> 2;
            pelsP[p0Idx] = (byte) clip(p0n, -128, 127);
        }

        if (conditionQ) {
            final int q2 = pelsQ[q2Idx];
            final int q3 = pelsQ[q3Idx];
            final int q0n = (p1 + 2 * p0 + 2 * q0 + 2 * q1 + q2 + 4) >> 3;
            final int q1n = (p0 + q0 + q1 + q2 + 2) >> 2;
            final int q2n = (2 * q3 + 3 * q2 + q1 + q0 + p0 + 4) >> 3;
            pelsQ[q0Idx] = (byte) clip(q0n, -128, 127);
            pelsQ[q1Idx] = (byte) clip(q1n, -128, 127);
            pelsQ[q2Idx] = (byte) clip(q2n, -128, 127);
        } else {
            final int q0n = (2 * q1 + q0 + p1 + 2) >> 2;
            pelsQ[q0Idx] = (byte) clip(q0n, -128, 127);
        }
    }
}
