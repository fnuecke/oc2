/* SPDX-License-Identifier: BSD 2-Clause "Simplified" License */

package li.cil.oc2.jcodec.codecs.h264.decode;

import li.cil.oc2.jcodec.codecs.h264.H264Const;
import li.cil.oc2.jcodec.codecs.h264.H264Utils.MvList;
import li.cil.oc2.jcodec.codecs.h264.decode.aso.Mapper;
import li.cil.oc2.jcodec.codecs.h264.io.model.Frame;
import li.cil.oc2.jcodec.codecs.h264.io.model.SliceHeader;
import li.cil.oc2.jcodec.codecs.h264.io.model.SliceType;
import li.cil.oc2.jcodec.common.model.Picture;

import java.util.Arrays;

import static li.cil.oc2.jcodec.codecs.h264.H264Const.*;
import static li.cil.oc2.jcodec.codecs.h264.H264Const.PartPred.Direct;
import static li.cil.oc2.jcodec.codecs.h264.H264Const.PartPred.L0;
import static li.cil.oc2.jcodec.codecs.h264.H264Utils.Mv.*;

/**
 * A decoder for Inter 16x16, 16x8 and 8x16 macroblocks
 *
 * @author The JCodec project
 */
public final class MBlockDecoderInter8x8 extends MBlockDecoderBase {
    private final Mapper mapper;
    private final MBlockDecoderBDirect bDirectDecoder;

    public MBlockDecoderInter8x8(final Mapper mapper, final MBlockDecoderBDirect bDirectDecoder, final SliceHeader sh, final DeblockerInput di,
                                 final int poc, final DecoderState decoderState) {
        super(sh, di, poc, decoderState);
        this.mapper = mapper;
        this.bDirectDecoder = bDirectDecoder;
    }

    public void decode(final MBlock mBlock, final Frame[][] references, final Picture mb, final SliceType sliceType) {
        final int mbX = mapper.getMbX(mBlock.mbIdx);
        final int mbY = mapper.getMbY(mBlock.mbIdx);
        final boolean leftAvailable = mapper.leftAvailable(mBlock.mbIdx);
        final boolean topAvailable = mapper.topAvailable(mBlock.mbIdx);
        final int mbAddr = mapper.getAddress(mBlock.mbIdx);
        final boolean topLeftAvailable = mapper.topLeftAvailable(mBlock.mbIdx);
        final boolean topRightAvailable = mapper.topRightAvailable(mBlock.mbIdx);

        if (sliceType == SliceType.P) {
            predict8x8P(mBlock, references[0], mb, mbX, mbY, leftAvailable, topAvailable, topLeftAvailable,
                topRightAvailable, mBlock.x, mBlock.partPreds);
        } else {
            predict8x8B(mBlock, references, mb, mbX, mbY, leftAvailable, topAvailable, topLeftAvailable,
                topRightAvailable, mBlock.x, mBlock.partPreds);
        }

        predictChromaInter(references, mBlock.x, mbX << 3, mbY << 3, 1, mb, mBlock.partPreds);
        predictChromaInter(references, mBlock.x, mbX << 3, mbY << 3, 2, mb, mBlock.partPreds);

        if (mBlock.cbpLuma() > 0 || mBlock.cbpChroma() > 0) {
            s.qp = (s.qp + mBlock.mbQPDelta + 52) % 52;
        }
        di.mbQps[0][mbAddr] = s.qp;

        residualLuma(mBlock);

        MBlockDecoderUtils.saveMvs(di, mBlock.x, mbX, mbY);

        final int qp1 = calcQpChroma(s.qp, s.chromaQpOffset[0]);
        final int qp2 = calcQpChroma(s.qp, s.chromaQpOffset[1]);

        decodeChromaResidual(mBlock, qp1, qp2);

        di.mbQps[1][mbAddr] = qp1;
        di.mbQps[2][mbAddr] = qp2;

        MBlockDecoderUtils.mergeResidual(mb, mBlock.ac, mBlock.transform8x8Used ? COMP_BLOCK_8x8_LUT : COMP_BLOCK_4x4_LUT,
            mBlock.transform8x8Used ? COMP_POS_8x8_LUT : COMP_POS_4x4_LUT);

        MBlockDecoderUtils.collectPredictors(s, mb, mbX);

        di.mbTypes[mbAddr] = mBlock.curMbType;
        di.tr8x8Used[mbAddr] = mBlock.transform8x8Used;
    }

    private void predict8x8P(final MBlock mBlock, final Picture[] references, final Picture mb, final int mbX, final int mbY,
                             final boolean leftAvailable, final boolean topAvailable, final boolean tlAvailable, final boolean topRightAvailable, final MvList x,
                             final PartPred[] pp) {
        decodeSubMb8x8(mBlock, 0, mBlock.pb8x8.subMbTypes[0], references, mbX << 6, mbY << 6, s.mvTopLeft.getMv(0, 0),
            s.mvTop.getMv(mbX << 2, 0), s.mvTop.getMv((mbX << 2) + 1, 0), s.mvTop.getMv((mbX << 2) + 2, 0),
            s.mvLeft.getMv(0, 0), s.mvLeft.getMv(1, 0), tlAvailable, topAvailable, topAvailable, leftAvailable,
            mBlock.x, 0, 1, 4, 5, mBlock.pb8x8.refIdx[0][0], mb, 0, 0);

        decodeSubMb8x8(mBlock, 1, mBlock.pb8x8.subMbTypes[1], references, (mbX << 6) + 32, mbY << 6,
            s.mvTop.getMv((mbX << 2) + 1, 0), s.mvTop.getMv((mbX << 2) + 2, 0), s.mvTop.getMv((mbX << 2) + 3, 0),
            s.mvTop.getMv((mbX << 2) + 4, 0), x.getMv(1, 0), x.getMv(5, 0), topAvailable, topAvailable,
            topRightAvailable, true, x, 2, 3, 6, 7, mBlock.pb8x8.refIdx[0][1], mb, 8, 0);

        decodeSubMb8x8(mBlock, 2, mBlock.pb8x8.subMbTypes[2], references, mbX << 6, (mbY << 6) + 32,
            s.mvLeft.getMv(1, 0), x.getMv(4, 0), x.getMv(5, 0), x.getMv(6, 0), s.mvLeft.getMv(2, 0),
            s.mvLeft.getMv(3, 0), leftAvailable, true, true, leftAvailable, x, 8, 9, 12, 13,
            mBlock.pb8x8.refIdx[0][2], mb, 128, 0);

        decodeSubMb8x8(mBlock, 3, mBlock.pb8x8.subMbTypes[3], references, (mbX << 6) + 32, (mbY << 6) + 32,
            x.getMv(5, 0), x.getMv(6, 0), x.getMv(7, 0), MBlockDecoderUtils.NULL_VECTOR, x.getMv(9, 0), x.getMv(13, 0), true, true,
            false, true, x, 10, 11, 14, 15, mBlock.pb8x8.refIdx[0][3], mb, 136, 0);

        for (int i = 0; i < 4; i++) {
            // TODO(stan): refactor this
            final int blk4x4 = BLK8x8_BLOCKS[i][0];
            PredictionMerger.weightPrediction(sh, x.mv0R(blk4x4), 0, mb.getPlaneData(0), BLK_8x8_MB_OFF_LUMA[i], 16, 8, 8, mb.getPlaneData(0));
        }

        MBlockDecoderUtils.savePrediction8x8(s, mbX, x);
        Arrays.fill(pp, L0);
    }

    private void predict8x8B(final MBlock mBlock, final Frame[][] refs, final Picture mb, final int mbX, final int mbY,
                             final boolean leftAvailable, final boolean topAvailable, final boolean tlAvailable, final boolean topRightAvailable, final MvList x,
                             final PartPred[] p) {
        for (int i = 0; i < 4; i++) {
            p[i] = bPartPredModes[mBlock.pb8x8.subMbTypes[i]];
        }

        for (int i = 0; i < 4; i++) {
            if (p[i] == Direct)
                bDirectDecoder.predictBDirect(refs, mbX, mbY, leftAvailable, topAvailable, tlAvailable,
                    topRightAvailable, x, p, mb, ARRAY[i]);
        }

        for (int list = 0; list < 2; list++) {
            if (H264Const.usesList(bPartPredModes[mBlock.pb8x8.subMbTypes[0]], list)) {
                decodeSubMb8x8(mBlock, 0, bSubMbTypes[mBlock.pb8x8.subMbTypes[0]], refs[list], mbX << 6, mbY << 6,
                    s.mvTopLeft.getMv(0, list), s.mvTop.getMv(mbX << 2, list), s.mvTop.getMv((mbX << 2) + 1, list),
                    s.mvTop.getMv((mbX << 2) + 2, list), s.mvLeft.getMv(0, list), s.mvLeft.getMv(1, list),
                    tlAvailable, topAvailable, topAvailable, leftAvailable, x, 0, 1, 4, 5,
                    mBlock.pb8x8.refIdx[list][0], mbb[list], 0, list);
            }
            if (H264Const.usesList(bPartPredModes[mBlock.pb8x8.subMbTypes[1]], list)) {
                decodeSubMb8x8(mBlock, 1, bSubMbTypes[mBlock.pb8x8.subMbTypes[1]], refs[list], (mbX << 6) + 32,
                    mbY << 6, s.mvTop.getMv((mbX << 2) + 1, list), s.mvTop.getMv((mbX << 2) + 2, list),
                    s.mvTop.getMv((mbX << 2) + 3, list), s.mvTop.getMv((mbX << 2) + 4, list), x.getMv(1, list),
                    x.getMv(5, list), topAvailable, topAvailable, topRightAvailable, true, x, 2, 3, 6, 7,
                    mBlock.pb8x8.refIdx[list][1], mbb[list], 8, list);
            }

            if (H264Const.usesList(bPartPredModes[mBlock.pb8x8.subMbTypes[2]], list)) {
                decodeSubMb8x8(mBlock, 2, bSubMbTypes[mBlock.pb8x8.subMbTypes[2]], refs[list], mbX << 6,
                    (mbY << 6) + 32, s.mvLeft.getMv(1, list), x.getMv(4, list), x.getMv(5, list), x.getMv(6, list),
                    s.mvLeft.getMv(2, list), s.mvLeft.getMv(3, list), leftAvailable, true, true, leftAvailable, x,
                    8, 9, 12, 13, mBlock.pb8x8.refIdx[list][2], mbb[list], 128, list);
            }

            if (H264Const.usesList(bPartPredModes[mBlock.pb8x8.subMbTypes[3]], list)) {
                decodeSubMb8x8(mBlock, 3, bSubMbTypes[mBlock.pb8x8.subMbTypes[3]], refs[list], (mbX << 6) + 32,
                    (mbY << 6) + 32, x.getMv(5, list), x.getMv(6, list), x.getMv(7, list), MBlockDecoderUtils.NULL_VECTOR,
                    x.getMv(9, list), x.getMv(13, list), true, true, false, true, x, 10, 11, 14, 15,
                    mBlock.pb8x8.refIdx[list][3], mbb[list], 136, list);
            }
        }

        for (int i = 0; i < 4; i++) {
            final int blk4x4 = BLK8x8_BLOCKS[i][0];
            PredictionMerger.mergePrediction(sh, x.mv0R(blk4x4), x.mv1R(blk4x4), bPartPredModes[mBlock.pb8x8.subMbTypes[i]], 0,
                mbb[0].getPlaneData(0), mbb[1].getPlaneData(0), BLK_8x8_MB_OFF_LUMA[i], 16, 8, 8,
                mb.getPlaneData(0), refs, poc);
        }

        MBlockDecoderUtils.savePrediction8x8(s, mbX, x);
    }

    private void decodeSubMb8x8(final MBlock mBlock, final int partNo, final int subMbType, final Picture[] references, final int offX, final int offY,
                                final int tl, final int t0, final int t1, final int tr, final int l0, final int l1, final boolean tlAvb, final boolean tAvb, final boolean trAvb, final boolean lAvb,
                                final MvList x, final int i00, final int i01, final int i10, final int i11, final int refIdx, final Picture mb, final int off, final int list) {
        switch (subMbType) {
            case 3 -> decodeSub4x4(mBlock, partNo, references, offX, offY, tl, t0, t1, tr, l0, l1, tlAvb, tAvb, trAvb, lAvb, x, i00, i01, i10, i11, refIdx, mb, off, list);
            case 2 -> decodeSub4x8(mBlock, partNo, references, offX, offY, tl, t0, t1, tr, l0, tlAvb, tAvb, trAvb, lAvb, x, i00, i01, i10, i11, refIdx, mb, off, list);
            case 1 -> decodeSub8x4(mBlock, partNo, references, offX, offY, tl, t0, tr, l0, l1, tlAvb, tAvb, trAvb, lAvb, x, i00, i01, i10, i11, refIdx, mb, off, list);
            case 0 -> decodeSub8x8(mBlock, partNo, references, offX, offY, tl, t0, tr, l0, tlAvb, tAvb, trAvb, lAvb, x, i00, i01, i10, i11, refIdx, mb, off, list);
        }
    }

    private void decodeSub8x8(final MBlock mBlock, final int partNo, final Picture[] references, final int offX, final int offY, final int tl,
                              final int t0, final int tr, final int l0, final boolean tlAvb, final boolean tAvb, final boolean trAvb, final boolean lAvb, final MvList x, final int i00,
                              final int i01, final int i10, final int i11, final int refIdx, final Picture mb, final int off, final int list) {
        final int mvpX = MBlockDecoderUtils.calcMVPredictionMedian(l0, t0, tr, tl, lAvb, tAvb, trAvb, tlAvb, refIdx, 0);
        final int mvpY = MBlockDecoderUtils.calcMVPredictionMedian(l0, t0, tr, tl, lAvb, tAvb, trAvb, tlAvb, refIdx, 1);

        final int mv = packMv(mBlock.pb8x8.mvdX1[list][partNo] + mvpX, mBlock.pb8x8.mvdY1[list][partNo] + mvpY, refIdx);

        x.setMv(i00, list, mv);
        x.setMv(i01, list, mv);
        x.setMv(i10, list, mv);
        x.setMv(i11, list, mv);

        interpolator.getBlockLuma(references[refIdx], mb, off, offX + mvX(mv), offY + mvY(mv), 8, 8);
    }

    private void decodeSub8x4(final MBlock mBlock, final int partNo, final Picture[] references, final int offX, final int offY, final int tl,
                              final int t0, final int tr, final int l0, final int l1, final boolean tlAvb, final boolean tAvb, final boolean trAvb, final boolean lAvb, final MvList x,
                              final int i00, final int i01, final int i10, final int i11, final int refIdx, final Picture mb, final int off, final int list) {
        final int mvpX1 = MBlockDecoderUtils.calcMVPredictionMedian(l0, t0, tr, tl, lAvb, tAvb, trAvb, tlAvb, refIdx, 0);
        final int mvpY1 = MBlockDecoderUtils.calcMVPredictionMedian(l0, t0, tr, tl, lAvb, tAvb, trAvb, tlAvb, refIdx, 1);

        // TODO(stan): check if MVs need to be clipped
        final int mv1 = packMv(mBlock.pb8x8.mvdX1[list][partNo] + mvpX1, mBlock.pb8x8.mvdY1[list][partNo] + mvpY1, refIdx);
        x.setMv(i00, list, mv1);
        x.setMv(i01, list, mv1);

        final int mvpX2 = MBlockDecoderUtils.calcMVPredictionMedian(l1, mv1, MBlockDecoderUtils.NULL_VECTOR, l0, lAvb, true, false, lAvb, refIdx, 0);
        final int mvpY2 = MBlockDecoderUtils.calcMVPredictionMedian(l1, mv1, MBlockDecoderUtils.NULL_VECTOR, l0, lAvb, true, false, lAvb, refIdx, 1);

        final int mv2 = packMv(mBlock.pb8x8.mvdX2[list][partNo] + mvpX2, mBlock.pb8x8.mvdY2[list][partNo] + mvpY2, refIdx);
        x.setMv(i10, list, mv2);
        x.setMv(i11, list, mv2);

        interpolator.getBlockLuma(references[refIdx], mb, off, offX + mvX(mv1), offY + mvY(mv1), 8, 4);
        interpolator.getBlockLuma(references[refIdx], mb, off + mb.getWidth() * 4, offX + mvX(mv2),
            offY + mvY(mv2) + 16, 8, 4);
    }

    private void decodeSub4x8(final MBlock mBlock, final int partNo, final Picture[] references, final int offX, final int offY, final int tl, final int t0,
                              final int t1, final int tr, final int l0, final boolean tlAvb, final boolean tAvb, final boolean trAvb, final boolean lAvb, final MvList x, final int i00,
                              final int i01, final int i10, final int i11, final int refIdx, final Picture mb, final int off, final int list) {
        final int mvpX1 = MBlockDecoderUtils.calcMVPredictionMedian(l0, t0, t1, tl, lAvb, tAvb, tAvb, tlAvb, refIdx, 0);
        final int mvpY1 = MBlockDecoderUtils.calcMVPredictionMedian(l0, t0, t1, tl, lAvb, tAvb, tAvb, tlAvb, refIdx, 1);

        final int mv1 = packMv(mBlock.pb8x8.mvdX1[list][partNo] + mvpX1, mBlock.pb8x8.mvdY1[list][partNo] + mvpY1, refIdx);
        x.setMv(i00, list, mv1);
        x.setMv(i10, list, mv1);

        final int mvpX2 = MBlockDecoderUtils.calcMVPredictionMedian(mv1, t1, tr, t0, true, tAvb, trAvb, tAvb, refIdx, 0);
        final int mvpY2 = MBlockDecoderUtils.calcMVPredictionMedian(mv1, t1, tr, t0, true, tAvb, trAvb, tAvb, refIdx, 1);

        final int mv2 = packMv(mBlock.pb8x8.mvdX2[list][partNo] + mvpX2, mBlock.pb8x8.mvdY2[list][partNo] + mvpY2, refIdx);

        x.setMv(i01, list, mv2);
        x.setMv(i11, list, mv2);

        interpolator.getBlockLuma(references[refIdx], mb, off, offX + mvX(mv1), offY + mvY(mv1), 4, 8);
        interpolator.getBlockLuma(references[refIdx], mb, off + 4, offX + mvX(mv2) + 16, offY + mvY(mv2), 4, 8);
    }

    private void decodeSub4x4(final MBlock mBlock, final int partNo, final Picture[] references, final int offX, final int offY, final int tl,
                              final int t0, final int t1, final int tr, final int l0, final int l1, final boolean tlAvb, final boolean tAvb, final boolean trAvb, final boolean lAvb, final MvList x,
                              final int i00, final int i01, final int i10, final int i11, final int refIdx, final Picture mb, final int off, final int list) {
        final int mvpX1 = MBlockDecoderUtils.calcMVPredictionMedian(l0, t0, t1, tl, lAvb, tAvb, tAvb, tlAvb, refIdx, 0);
        final int mvpY1 = MBlockDecoderUtils.calcMVPredictionMedian(l0, t0, t1, tl, lAvb, tAvb, tAvb, tlAvb, refIdx, 1);

        final int mv1 = packMv(mBlock.pb8x8.mvdX1[list][partNo] + mvpX1, mBlock.pb8x8.mvdY1[list][partNo] + mvpY1, refIdx);
        x.setMv(i00, list, mv1);

        final int mvpX2 = MBlockDecoderUtils.calcMVPredictionMedian(mv1, t1, tr, t0, true, tAvb, trAvb, tAvb, refIdx, 0);
        final int mvpY2 = MBlockDecoderUtils.calcMVPredictionMedian(mv1, t1, tr, t0, true, tAvb, trAvb, tAvb, refIdx, 1);

        final int mv2 = packMv(mBlock.pb8x8.mvdX2[list][partNo] + mvpX2, mBlock.pb8x8.mvdY2[list][partNo] + mvpY2, refIdx);
        x.setMv(i01, list, mv2);

        final int mvpX3 = MBlockDecoderUtils.calcMVPredictionMedian(l1, mv1, mv2, l0, lAvb, true, true, lAvb, refIdx, 0);
        final int mvpY3 = MBlockDecoderUtils.calcMVPredictionMedian(l1, mv1, mv2, l0, lAvb, true, true, lAvb, refIdx, 1);

        final int mv3 = packMv(mBlock.pb8x8.mvdX3[list][partNo] + mvpX3, mBlock.pb8x8.mvdY3[list][partNo] + mvpY3, refIdx);
        x.setMv(i10, list, mv3);

        final int mvpX4 = MBlockDecoderUtils.calcMVPredictionMedian(mv3, mv2, MBlockDecoderUtils.NULL_VECTOR, mv1, true, true, false, true, refIdx, 0);
        final int mvpY4 = MBlockDecoderUtils.calcMVPredictionMedian(mv3, mv2, MBlockDecoderUtils.NULL_VECTOR, mv1, true, true, false, true, refIdx, 1);

        final int mv4 = packMv(mBlock.pb8x8.mvdX4[list][partNo] + mvpX4, mBlock.pb8x8.mvdY4[list][partNo] + mvpY4, refIdx);
        x.setMv(i11, list, mv4);

        interpolator.getBlockLuma(references[refIdx], mb, off, offX + mvX(mv1), offY + mvY(mv1), 4, 4);
        interpolator.getBlockLuma(references[refIdx], mb, off + 4, offX + mvX(mv2) + 16, offY + mvY(mv2), 4, 4);
        interpolator.getBlockLuma(references[refIdx], mb, off + mb.getWidth() * 4, offX + mvX(mv3), offY + mvY(mv3)
            + 16, 4, 4);
        interpolator.getBlockLuma(references[refIdx], mb, off + mb.getWidth() * 4 + 4, offX + mvX(mv4) + 16, offY
            + mvY(mv4) + 16, 4, 4);
    }
}
