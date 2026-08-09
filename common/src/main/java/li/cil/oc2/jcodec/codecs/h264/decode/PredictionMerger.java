/* SPDX-License-Identifier: BSD 2-Clause "Simplified" License */

package li.cil.oc2.jcodec.codecs.h264.decode;

import li.cil.oc2.jcodec.codecs.h264.H264Const.PartPred;
import li.cil.oc2.jcodec.codecs.h264.io.model.*;
import li.cil.oc2.jcodec.common.tools.MathUtil;

import static li.cil.oc2.jcodec.codecs.h264.H264Const.PartPred.*;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * <p>
 * Prediction merge and weight routines
 *
 * @author The JCodec project
 */
public final class PredictionMerger {
    public static void mergePrediction(final SliceHeader sh, final int refIdxL0, final int refIdxL1, final PartPred predType, final int comp,
                                       final byte[] pred0, final byte[] pred1, final int off, final int stride, final int blkW, final int blkH, final byte[] out, final Frame[][] refs, final int thisPoc) {
        final PictureParameterSet pps = sh.pps;
        if (sh.sliceType == SliceType.P) {
            weightPrediction(sh, refIdxL0, comp, pred0, off, stride, blkW, blkH, out);
        } else {
            if (!pps.weightedPredFlag || sh.pps.weightedBipredIdc == 0
                || (sh.pps.weightedBipredIdc == 2 && predType != Bi)) {
                mergeAvg(pred0, pred1, stride, predType, off, blkW, blkH, out);
            } else if (sh.pps.weightedBipredIdc == 1) {
                final PredictionWeightTable w = sh.predWeightTable;
                final int w0 = refIdxL0 == -1 ? 0 : (comp == 0 ? w.lumaWeight[0][refIdxL0]
                    : w.chromaWeight[0][comp - 1][refIdxL0]);
                final int w1 = refIdxL1 == -1 ? 0 : (comp == 0 ? w.lumaWeight[1][refIdxL1]
                    : w.chromaWeight[1][comp - 1][refIdxL1]);
                final int o0 = refIdxL0 == -1 ? 0 : (comp == 0 ? w.lumaOffset[0][refIdxL0]
                    : w.chromaOffset[0][comp - 1][refIdxL0]);
                final int o1 = refIdxL1 == -1 ? 0 : (comp == 0 ? w.lumaOffset[1][refIdxL1]
                    : w.chromaOffset[1][comp - 1][refIdxL1]);
                mergeWeight(pred0, pred1, stride, predType, off, blkW, blkH, comp == 0 ? w.lumaLog2WeightDenom
                    : w.chromaLog2WeightDenom, w0, w1, o0, o1, out);
            } else {
                final int tb = MathUtil.clip(thisPoc - refs[0][refIdxL0].getPOC(), -128, 127);
                final int td = MathUtil.clip(refs[1][refIdxL1].getPOC() - refs[0][refIdxL0].getPOC(), -128, 127);
                int w0 = 32, w1 = 32;
                if (td != 0 && refs[0][refIdxL0].isShortTerm() && refs[1][refIdxL1].isShortTerm()) {
                    final int tx = (16384 + Math.abs(td / 2)) / td;
                    final int dsf = MathUtil.clip((tb * tx + 32) >> 6, -1024, 1023) >> 2;

                    if (dsf >= -64 && dsf <= 128) {
                        w1 = dsf;
                        w0 = 64 - dsf;
                    }
                }

                mergeWeight(pred0, pred1, stride, predType, off, blkW, blkH, 5, w0, w1, 0, 0, out);
            }
        }
    }

    public static void weightPrediction(final SliceHeader sh, final int refIdxL0, final int comp, final byte[] pred0, final int off, final int stride,
                                        final int blkW, final int blkH, final byte[] out) {
        final PictureParameterSet pps = sh.pps;
        if (pps.weightedPredFlag && sh.predWeightTable != null) {
            final PredictionWeightTable w = sh.predWeightTable;
            weight(pred0, stride, off, blkW, blkH, comp == 0 ? w.lumaLog2WeightDenom
                : w.chromaLog2WeightDenom, comp == 0 ? w.lumaWeight[0][refIdxL0]
                : w.chromaWeight[0][comp - 1][refIdxL0], comp == 0 ? w.lumaOffset[0][refIdxL0]
                : w.chromaOffset[0][comp - 1][refIdxL0], out);
        } else {
            copyPrediction(pred0, stride, off, blkW, blkH, out);
        }
    }

    private static void mergeAvg(final byte[] blk0, final byte[] blk1, final int stride, final PartPred p0, final int off, final int blkW, final int blkH,
                                 final byte[] out) {
        if (p0 == Bi)
            _mergePrediction(blk0, blk1, stride, off, blkW, blkH, out);
        else if (p0 == L0)
            copyPrediction(blk0, stride, off, blkW, blkH, out);
        else if (p0 == L1)
            copyPrediction(blk1, stride, off, blkW, blkH, out);

    }

    private static void mergeWeight(final byte[] blk0, final byte[] blk1, final int stride, final PartPred partPred, final int off, final int blkW,
                                    final int blkH, final int logWD, final int w0, final int w1, final int o0, final int o1, final byte[] out) {
        if (partPred == L0) {
            weight(blk0, stride, off, blkW, blkH, logWD, w0, o0, out);
        } else if (partPred == L1) {
            weight(blk1, stride, off, blkW, blkH, logWD, w1, o1, out);
        } else if (partPred == Bi) {
            _weightPrediction(blk0, blk1, stride, off, blkW, blkH, logWD, w0, w1, o0, o1, out);
        }
    }

    private static void copyPrediction(final byte[] _in, final int stride, int off, final int blkW, final int blkH, final byte[] out) {

        for (int i = 0; i < blkH; i++, off += stride - blkW)
            for (int j = 0; j < blkW; j++, off++)
                out[off] = _in[off];
    }

    private static void _mergePrediction(final byte[] blk0, final byte[] blk1, final int stride, int off, final int blkW, final int blkH,
                                         final byte[] out) {

        for (int i = 0; i < blkH; i++, off += stride - blkW)
            for (int j = 0; j < blkW; j++, off++)
                out[off] = (byte) ((blk0[off] + blk1[off] + 1) >> 1);
    }

    private static void _weightPrediction(final byte[] blk0, final byte[] blk1, final int stride, int off, final int blkW, final int blkH, final int logWD,
                                          final int w0, final int w1, final int o0, final int o1, final byte[] out) {
        // Necessary to correctly scale in [-128, 127] range
        final int round = (1 << logWD) + ((w0 + w1) << 7);
        final int sum = ((o0 + o1 + 1) >> 1) - 128;
        final int logWDCP1 = logWD + 1;
        for (int i = 0; i < blkH; i++, off += stride - blkW)
            for (int j = 0; j < blkW; j++, off++) {
                out[off] = (byte) MathUtil.clip(((blk0[off] * w0 + blk1[off] * w1 + round) >> logWDCP1) + sum, -128, 127);
            }
    }

    private static void weight(final byte[] blk0, final int stride, int off, final int blkW, final int blkH, final int logWD, final int w, int o, final byte[] out) {
        int round = 1 << (logWD - 1);

        if (logWD >= 1) {
            // Necessary to correctly scale _in [-128, 127] range,
            // i.e. x = ay / b; x,y _in [0, 255] is
            // x = a * (y + 128) / b - 128; x,y _in [-128, 127]
            o -= 128;
            round += w << 7;
            for (int i = 0; i < blkH; i++, off += stride - blkW)
                for (int j = 0; j < blkW; j++, off++)
                    out[off] = (byte) MathUtil.clip(((blk0[off] * w + round) >> logWD) + o, -128, 127);
        } else {
            // Necessary to correctly scale in [-128, 127] range
            o += (w << 7) - 128;
            for (int i = 0; i < blkH; i++, off += stride - blkW)
                for (int j = 0; j < blkW; j++, off++)
                    out[off] = (byte) MathUtil.clip(blk0[off] * w + o, -128, 127);
        }
    }
}
