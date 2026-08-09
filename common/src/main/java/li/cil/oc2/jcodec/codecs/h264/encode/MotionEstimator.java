/* SPDX-License-Identifier: BSD 2-Clause "Simplified" License */

package li.cil.oc2.jcodec.codecs.h264.encode;

import li.cil.oc2.jcodec.codecs.h264.io.model.SeqParameterSet;
import li.cil.oc2.jcodec.common.model.Picture;
import li.cil.oc2.jcodec.common.tools.MathUtil;

import static java.lang.Math.min;
import static li.cil.oc2.jcodec.codecs.h264.encode.H264EncoderUtils.median;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * <p>
 * Estimates motion using diagonal search
 *
 * @author Stanislav Vitvitskyy
 */
public final class MotionEstimator {
    private final int maxSearchRange;
    private final int[] mvTopX;
    private final int[] mvTopY;
    private final int[] mvTopR;
    private int mvLeftX;
    private int mvLeftY;
    private int mvLeftR;
    private int mvTopLeftX;
    private int mvTopLeftY;
    private int mvTopLeftR;
    private final SeqParameterSet sps;
    private final Picture ref;

    public MotionEstimator(final Picture ref, final SeqParameterSet sps, final int maxSearchRange) {
        this.sps = sps;
        this.ref = ref;
        mvTopX = new int[sps.picWidthInMbsMinus1 + 1];
        mvTopY = new int[sps.picWidthInMbsMinus1 + 1];
        mvTopR = new int[sps.picWidthInMbsMinus1 + 1];
        this.maxSearchRange = maxSearchRange;
    }

    public int[] mvEstimate(final Picture pic, final int mbX, final int mbY) {
        final int refIdx = 1;
        final byte[] patch = new byte[256];
        final boolean trAvb = mbY > 0 && mbX < sps.picWidthInMbsMinus1;
        final boolean tlAvb = mbX > 0 && mbY > 0;

        final int ax = mvLeftX;
        final int ay = mvLeftY;
        final boolean ar = mvLeftR == refIdx;

        final int bx = mvTopX[mbX];
        final int by = mvTopY[mbX];
        final boolean br = mvTopR[mbX] == refIdx;

        final int cx = trAvb ? mvTopX[mbX + 1] : 0;
        final int cy = trAvb ? mvTopY[mbX + 1] : 0;
        final boolean cr = trAvb && mvTopR[mbX + 1] == refIdx;

        final int dx = tlAvb ? mvTopLeftX : 0;
        final int dy = tlAvb ? mvTopLeftY : 0;
        final boolean dr = tlAvb && mvTopLeftR == refIdx;

        final int mvpx = median(ax, ar, bx, br, cx, cr, dx, dr, mbX > 0, mbY > 0, trAvb, tlAvb);
        final int mvpy = median(ay, ar, by, br, cy, cr, dy, dr, mbX > 0, mbY > 0, trAvb, tlAvb);
        MBEncoderHelper.take(pic.getPlaneData(0), pic.getPlaneWidth(0), pic.getPlaneHeight(0), mbX << 4, mbY << 4, patch, 16, 16);
        final int[] fullPix = estimateFullPix(ref, patch, mbX, mbY, mvpx, mvpy);
        return estimateQPix(ref, patch, fullPix, mbX, mbY);
    }

    private static final int[] SUB_X_OFF = {0, -2, 2, 0, 0, -2, -2, 2, 2, -1, 1, 0, 0, -1, -2, -1, -2, 1, 2, 1, 2, -1, 1, -1, 1};
    private static final int[] SUB_Y_OFF = {0, 0, 0, -2, 2, -2, 2, -2, 2, 0, 0, -1, 1, -2, -1, 2, 1, -2, -1, 2, 1, -1, -1, 1, 1};

    public static int[] estimateQPix(final Picture ref, final byte[] patch, final int[] fullPix, final int mbX, final int mbY) {
        final int fullX = (mbX << 4) + (fullPix[0] >> 2);
        final int fullY = (mbY << 4) + (fullPix[1] >> 2);
        if (fullX < 3 || fullY < 3)
            return fullPix;
        final byte[] sp = new byte[22 * 22];
        MBEncoderHelper.take(ref.getPlaneData(0), ref.getPlaneWidth(0), ref.getPlaneHeight(0), fullX - 3, fullY - 3, sp, 22, 22);
        // Calculating half pen
        final int[] pp = new int[352];
        final int[] pn = new int[352];
        final int[] scores = new int[25];
        for (int j = 0, dOff = 0, sOff = 0; j < 22; j++) {
            for (int i = 0; i < 16; i++, dOff++, sOff++) {
                {
                    final int a = sp[sOff] + sp[sOff + 5];
                    final int b = sp[sOff + 1] + sp[sOff + 4];
                    final int c = sp[sOff + 2] + sp[sOff + 3];

                    pn[dOff] = a + 5 * ((c << 2) - b);
                }
                {
                    final int a = sp[sOff + 1] + sp[sOff + 6];
                    final int b = sp[sOff + 2] + sp[sOff + 5];
                    final int c = sp[sOff + 3] + sp[sOff + 4];
                    pp[dOff] = a + 5 * ((c << 2) - b);
                }
            }
            sOff += 6;
        }
        for (int j = 0, sof = 0, off = 0; j < 16; j++) {
            for (int i = 0; i < 16; i++, off++, sof++) {
                scores[0] += MathUtil.abs(patch[off] - sp[sof + 69]);
                final int horN20 = MathUtil.clip((pn[off + 48] + 16) >> 5, -128, 127);
                final int horP20 = MathUtil.clip((pp[off + 48] + 16) >> 5, -128, 127);
                scores[1] += MathUtil.abs(patch[off] - horN20);
                scores[2] += MathUtil.abs(patch[off] - horP20);
                final int horN10 = (horN20 + sp[sof + 69] + 1) >> 1;
                final int horP10 = (horP20 + sp[sof + 69] + 1) >> 1;
                scores[9] += MathUtil.abs(patch[off] - horN10);
                scores[10] += MathUtil.abs(patch[off] - horP10);
                final int verN20;
                {
                    final int a = sp[3 + sof] + sp[3 + sof + 110];
                    final int b = sp[3 + sof + 22] + sp[3 + sof + 88];
                    final int c = sp[3 + sof + 44] + sp[3 + sof + 66];

                    final int verNeg = a + 5 * ((c << 2) - b);
                    verN20 = MathUtil.clip((verNeg + 16) >> 5, -128, 127);
                    final int verN10 = (verN20 + sp[sof + 69] + 1) >> 1;
                    final int dnn = (verN20 + horN20 + 1) >> 1;
                    final int dpn = (verN20 + horP20 + 1) >> 1;
                    scores[3] += MathUtil.abs(patch[off] - verN20);
                    scores[11] += MathUtil.abs(patch[off] - verN10);
                    scores[21] += MathUtil.abs(patch[off] - dnn);
                    scores[22] += MathUtil.abs(patch[off] - dpn);
                }
                final int verP20;
                {
                    final int a = sp[3 + sof + 22] + sp[3 + sof + 132];
                    final int b = sp[3 + sof + 44] + sp[3 + sof + 110];
                    final int c = sp[3 + sof + 66] + sp[3 + sof + 88];
                    final int verPos = a + 5 * ((c << 2) - b);

                    verP20 = MathUtil.clip((verPos + 16) >> 5, -128, 127);
                    final int verP10 = (verP20 + sp[sof + 69] + 1) >> 1;
                    final int dnp = (verP20 + horN20 + 1) >> 1;
                    final int dpp = (verP20 + horP20 + 1) >> 1;

                    scores[4] += MathUtil.abs(patch[off] - verP20);
                    scores[12] += MathUtil.abs(patch[off] - verP10);
                    scores[23] += MathUtil.abs(patch[off] - dnp);
                    scores[24] += MathUtil.abs(patch[off] - dpp);
                }
                {
                    final int a = pn[off] + pn[off + 80];
                    final int b = pn[off + 16] + pn[off + 64];
                    final int c = pn[off + 32] + pn[off + 48];

                    final int interpNeg = a + 5 * ((c << 2) - b);
                    final int diagNN = MathUtil.clip((interpNeg + 512) >> 10, -128, 127);
                    final int ver = (diagNN + verN20 + 1) >> 1;
                    final int hor = (diagNN + horN20 + 1) >> 1;

                    scores[5] += MathUtil.abs(patch[off] - diagNN);
                    scores[13] += MathUtil.abs(patch[off] - ver);
                    scores[14] += MathUtil.abs(patch[off] - hor);
                }
                {
                    final int a = pn[off + 16] + pn[off + 96];
                    final int b = pn[off + 32] + pn[off + 80];
                    final int c = pn[off + 48] + pn[off + 64];
                    final int interpPos = a + 5 * ((c << 2) - b);

                    final int diagNP = MathUtil.clip((interpPos + 512) >> 10, -128, 127);
                    final int ver = (diagNP + verP20 + 1) >> 1;
                    final int hor = (diagNP + horN20 + 1) >> 1;
                    scores[6] += MathUtil.abs(patch[off] - diagNP);
                    scores[15] += MathUtil.abs(patch[off] - ver);
                    scores[16] += MathUtil.abs(patch[off] - hor);
                }
                {
                    final int a = pp[off] + pp[off + 80];
                    final int b = pp[off + 16] + pp[off + 64];
                    final int c = pp[off + 32] + pp[off + 48];

                    final int interpNeg = a + 5 * ((c << 2) - b);
                    final int diagPN = MathUtil.clip((interpNeg + 512) >> 10, -128, 127);
                    final int ver = (diagPN + verN20 + 1) >> 1;
                    final int hor = (diagPN + horP20 + 1) >> 1;
                    scores[7] += MathUtil.abs(patch[off] - diagPN);
                    scores[17] += MathUtil.abs(patch[off] - ver);
                    scores[18] += MathUtil.abs(patch[off] - hor);
                }
                {
                    final int a = pp[off + 16] + pp[off + 96];
                    final int b = pp[off + 32] + pp[off + 80];
                    final int c = pp[off + 48] + pp[off + 64];
                    final int interpPos = a + 5 * ((c << 2) - b);

                    final int diagPP = MathUtil.clip((interpPos + 512) >> 10, -128, 127);
                    final int ver = (diagPP + verP20 + 1) >> 1;
                    final int hor = (diagPP + horP20 + 1) >> 1;
                    scores[8] += MathUtil.abs(patch[off] - diagPP);
                    scores[19] += MathUtil.abs(patch[off] - ver);
                    scores[20] += MathUtil.abs(patch[off] - hor);
                }
            }
            sof += 6;
        }


        int m0 = Math.min(scores[1], scores[2]);
        final int m1 = Math.min(scores[3], scores[4]);
        int m2 = Math.min(scores[5], scores[6]);

        final int m3 = Math.min(scores[7], scores[8]);
        int m4 = Math.min(scores[9], scores[10]);
        final int m5 = Math.min(scores[11], scores[12]);

        int m6 = Math.min(scores[13], scores[14]);
        final int m7 = Math.min(scores[15], scores[16]);
        int m8 = Math.min(scores[17], scores[18]);

        final int m9 = Math.min(scores[19], scores[20]);
        int m10 = Math.min(scores[21], scores[22]);
        final int m11 = Math.min(scores[23], scores[24]);

        m0 = Math.min(m0, m1);
        m2 = Math.min(m2, m3);
        m4 = Math.min(m4, m5);
        m6 = Math.min(m6, m7);
        m8 = Math.min(m8, m9);
        m10 = Math.min(m10, m11);

        m0 = Math.min(m0, m2);
        m4 = Math.min(m4, m6);
        m8 = Math.min(m8, m10);

        final int mf0 = Math.min(scores[0], m0);
        final int mf1 = Math.min(m4, m8);
        final int mf2 = Math.min(mf0, mf1);

        int sel = 0;
        for (int i = 0; i < 25; i++) {
            if (mf2 == scores[i]) {
                sel = i;
                break;
            }
        }

        return new int[]{fullPix[0] + SUB_X_OFF[sel], fullPix[1] + SUB_Y_OFF[sel]};
    }

    public void mvSave(final int mbX, final int[] mv) {
        mvTopLeftX = mvTopX[mbX];
        mvTopLeftY = mvTopY[mbX];
        mvTopLeftR = mvTopR[mbX];
        mvTopX[mbX] = mv[0];
        mvTopY[mbX] = mv[1];
        mvTopR[mbX] = mv[2];
        mvLeftX = mv[0];
        mvLeftY = mv[1];
        mvLeftR = mv[2];
    }

    private int[] estimateFullPix(final Picture ref, final byte[] patch, final int mbX, final int mbY, final int mvpx, final int mvpy) {
        final byte[] searchPatch = new byte[(maxSearchRange * 2 + 16) * (maxSearchRange * 2 + 16)];

        int mvX0 = 0;
        int mvX1 = 0;
        int mvY0 = 0;
        int mvY1 = 0;
        int mvS0 = Integer.MAX_VALUE;
        int mvS1 = Integer.MAX_VALUE;
        // Search area 0: mb position
        int startX = (mbX << 4);
        int startY = (mbY << 4);
        for (int area = 0; area < 2; area++) {
            final int patchTlX = Math.max(startX - maxSearchRange, 0);
            final int patchTlY = Math.max(startY - maxSearchRange, 0);
            final int patchBrX = Math.min(startX + maxSearchRange + 16, ref.getPlaneWidth(0));
            final int patchBrY = Math.min(startY + maxSearchRange + 16, ref.getPlaneHeight(0));

            final int inPatchX = startX - patchTlX;
            final int inPatchY = startY - patchTlY;
            if (inPatchX < 0 || inPatchY < 0)
                continue;
            final int patchW = patchBrX - patchTlX;
            final int patchH = patchBrY - patchTlY;
            // TODO: border fill?
            MBEncoderHelper.takeSafe(ref.getPlaneData(0), ref.getPlaneWidth(0), patchTlX, patchTlY, searchPatch, patchW, patchH);

            int bestMvX = inPatchX;
            int bestMvY = inPatchY;
            int bestScore = sad(searchPatch, patchW, patch, bestMvX, bestMvY);
            // Diagonal search
            for (int i = 0; i < maxSearchRange; i++) {
                final int score1 = bestMvX > 0 ? sad(searchPatch, patchW, patch, bestMvX - 1, bestMvY) : Integer.MAX_VALUE;
                final int score2 = bestMvX < patchW - 1
                    ? sad(searchPatch, patchW, patch, bestMvX + 1, bestMvY)
                    : Integer.MAX_VALUE;
                final int score3 = bestMvY > 0 ? sad(searchPatch, patchW, patch, bestMvX, bestMvY - 1) : Integer.MAX_VALUE;
                final int score4 = bestMvY < patchH - 1
                    ? sad(searchPatch, patchW, patch, bestMvX, bestMvY + 1)
                    : Integer.MAX_VALUE;
                final int min = min(min(min(score1, score2), score3), score4);
                if (min > bestScore)
                    break;
                bestScore = min;
                if (score1 == min) {
                    --bestMvX;
                } else if (score2 == min) {
                    ++bestMvX;
                } else if (score3 == min) {
                    --bestMvY;
                } else {
                    ++bestMvY;
                }
            }
            if (area == 0) {
                mvX0 = ((bestMvX - inPatchX) << 2);
                mvY0 = ((bestMvY - inPatchY) << 2);
                mvS0 = bestScore;
                // Search area 1: mb predictor
                startX = (mbX << 4) + (mvpx >> 2);
                startY = (mbY << 4) + (mvpy >> 2);
            } else {
                mvX1 = (bestMvX - inPatchX + startX - (mbX << 4)) << 2;
                mvY1 = (bestMvY - inPatchY + startY - (mbY << 4)) << 2;
                mvS1 = bestScore;
            }
        }

        return new int[]{mvS0 < mvS1 ? mvX0 : mvX1, mvS0 < mvS1 ? mvY0 : mvY1};
    }

    private int sad(final byte[] big, final int bigStride, final byte[] small, final int offX, final int offY) {
        int score = 0, bigOff = offY * bigStride + offX, smallOff = 0;
        for (int i = 0; i < 16; i++) {
            for (int j = 0; j < 16; j++, ++bigOff, ++smallOff) {
                score += MathUtil.abs(big[bigOff] - small[smallOff]);
            }
            bigOff += bigStride - 16;
        }
        return score;
    }
}
