/* SPDX-License-Identifier: BSD 2-Clause "Simplified" License */

package li.cil.oc2.jcodec.codecs.h264.decode;

import li.cil.oc2.jcodec.codecs.h264.H264Utils.MvList;
import li.cil.oc2.jcodec.common.model.Picture;
import li.cil.oc2.jcodec.common.tools.MathUtil;

import static java.lang.System.arraycopy;
import static li.cil.oc2.jcodec.codecs.h264.H264Utils.Mv.*;

public final class MBlockDecoderUtils {
    public static final int NULL_VECTOR = packMv(0, 0, -1);

    static void collectPredictors(final DecoderState sharedState, final Picture outMB, final int mbX) {
        sharedState.topLeft[0][0] = sharedState.topLine[0][(mbX << 4) + 15];
        sharedState.topLeft[0][1] = outMB.getPlaneData(0)[63];
        sharedState.topLeft[0][2] = outMB.getPlaneData(0)[127];
        sharedState.topLeft[0][3] = outMB.getPlaneData(0)[191];
        arraycopy(outMB.getPlaneData(0), 240, sharedState.topLine[0], mbX << 4, 16);
        copyCol(outMB.getPlaneData(0), 16, 15, 16, sharedState.leftRow[0]);

        collectChromaPredictors(sharedState, outMB, mbX);
    }

    static void collectChromaPredictors(final DecoderState sharedState, final Picture outMB, final int mbX) {
        sharedState.topLeft[1][0] = sharedState.topLine[1][(mbX << 3) + 7];
        sharedState.topLeft[2][0] = sharedState.topLine[2][(mbX << 3) + 7];

        arraycopy(outMB.getPlaneData(1), 56, sharedState.topLine[1], mbX << 3, 8);
        arraycopy(outMB.getPlaneData(2), 56, sharedState.topLine[2], mbX << 3, 8);

        copyCol(outMB.getPlaneData(1), 8, 7, 8, sharedState.leftRow[1]);
        copyCol(outMB.getPlaneData(2), 8, 7, 8, sharedState.leftRow[2]);
    }

    private static void copyCol(final byte[] planeData, final int n, int off, final int stride, final byte[] out) {
        for (int i = 0; i < n; i++, off += stride) {
            out[i] = planeData[off];
        }
    }

    static void saveMvsIntra(final DeblockerInput di, final int mbX, final int mbY) {
        for (int j = 0, blkOffY = mbY << 2, blkInd = 0; j < 4; j++, blkOffY++) {
            for (int i = 0, blkOffX = mbX << 2; i < 4; i++, blkOffX++, blkInd++) {
                di.mvs.setMv(blkOffX, blkOffY, 0, NULL_VECTOR);
                di.mvs.setMv(blkOffX, blkOffY, 1, NULL_VECTOR);
            }
        }
    }

    static void mergeResidual(final Picture mb, final int[][][] residual, final int[][] blockLUT, final int[][] posLUT) {
        for (int comp = 0; comp < 3; comp++) {
            final byte[] to = mb.getPlaneData(comp);
            for (int i = 0; i < to.length; i++) {
                to[i] = (byte) MathUtil.clip(to[i] + residual[comp][blockLUT[comp][i]][posLUT[comp][i]], -128, 127);
            }
        }
    }

    static void saveVect(final MvList mv, final int list, final int from, final int to, final int vect) {
        for (int i = from; i < to; i++) {
            mv.setMv(i, list, vect);
        }
    }

    /**
     * Calculates median prediction
     *
     * @param a, b, c and d are packed motion vectors
     */
    public static int calcMVPredictionMedian(int a, int b, int c, final int d, final boolean aAvb, boolean bAvb, boolean cAvb,
                                             final boolean dAvb, final int ref, final int comp) {

        if (!cAvb) {
            c = d;
            cAvb = dAvb;
        }

        if (aAvb && !bAvb && !cAvb) {
            b = c = a;
            bAvb = cAvb = aAvb;
        }

        a = aAvb ? a : NULL_VECTOR;
        b = bAvb ? b : NULL_VECTOR;
        c = cAvb ? c : NULL_VECTOR;

        if (mvRef(a) == ref && mvRef(b) != ref && mvRef(c) != ref)
            return mvC(a, comp);
        else if (mvRef(b) == ref && mvRef(a) != ref && mvRef(c) != ref)
            return mvC(b, comp);
        else if (mvRef(c) == ref && mvRef(a) != ref && mvRef(b) != ref)
            return mvC(c, comp);

        return mvC(a, comp) + mvC(b, comp) + mvC(c, comp)
            - MathUtil.min3(mvC(a, comp), mvC(b, comp), mvC(c, comp))
            - MathUtil.max3(mvC(a, comp), mvC(b, comp), mvC(c, comp));
    }

    static void saveMvs(final DeblockerInput di, final MvList x, final int mbX, final int mbY) {
        for (int j = 0, blkOffY = mbY << 2, blkInd = 0; j < 4; j++, blkOffY++) {
            for (int i = 0, blkOffX = mbX << 2; i < 4; i++, blkOffX++, blkInd++) {
                di.mvs.setMv(blkOffX, blkOffY, 0, x.getMv(blkInd, 0));
                di.mvs.setMv(blkOffX, blkOffY, 1, x.getMv(blkInd, 1));
            }
        }
    }

    static void savePrediction8x8(final DecoderState sharedState, final int mbX, final MvList x) {
        sharedState.mvTopLeft.copyPair(0, sharedState.mvTop, (mbX << 2) + 3);
        sharedState.mvLeft.copyPair(0, x, 3);
        sharedState.mvLeft.copyPair(1, x, 7);
        sharedState.mvLeft.copyPair(2, x, 11);
        sharedState.mvLeft.copyPair(3, x, 15);
        sharedState.mvTop.copyPair(mbX << 2, x, 12);
        sharedState.mvTop.copyPair((mbX << 2) + 1, x, 13);
        sharedState.mvTop.copyPair((mbX << 2) + 2, x, 14);
        sharedState.mvTop.copyPair((mbX << 2) + 3, x, 15);
    }

    public static void saveVectIntra(final DecoderState sharedState, final int mbX) {
        final int xx = mbX << 2;

        sharedState.mvTopLeft.copyPair(0, sharedState.mvTop, xx + 3);

        saveVect(sharedState.mvTop, 0, xx, xx + 4, NULL_VECTOR);
        saveVect(sharedState.mvLeft, 0, 0, 4, NULL_VECTOR);
        saveVect(sharedState.mvTop, 1, xx, xx + 4, NULL_VECTOR);
        saveVect(sharedState.mvLeft, 1, 0, 4, NULL_VECTOR);
    }
}
