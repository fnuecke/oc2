/* SPDX-License-Identifier: BSD 2-Clause "Simplified" License */

package li.cil.oc2.jcodec.codecs.h264.decode;

import li.cil.oc2.jcodec.codecs.h264.H264Const;
import li.cil.oc2.jcodec.codecs.h264.H264Utils.MvList;
import li.cil.oc2.jcodec.codecs.h264.decode.aso.Mapper;
import li.cil.oc2.jcodec.codecs.h264.io.model.Frame;
import li.cil.oc2.jcodec.codecs.h264.io.model.SliceHeader;
import li.cil.oc2.jcodec.common.model.ColorSpace;
import li.cil.oc2.jcodec.common.model.Picture;

import static li.cil.oc2.jcodec.codecs.h264.H264Const.*;
import static li.cil.oc2.jcodec.codecs.h264.H264Utils.Mv.*;
import static li.cil.oc2.jcodec.codecs.h264.decode.MBlockDecoderUtils.*;
import static li.cil.oc2.jcodec.codecs.h264.decode.PredictionMerger.mergePrediction;

/**
 * A decoder for Inter 16x16, 16x8 and 8x16 macroblocks
 *
 * @author The JCodec project
 */
public final class MBlockDecoderInter extends MBlockDecoderBase {
    private final Mapper mapper;

    public MBlockDecoderInter(final Mapper mapper, final SliceHeader sh, final DeblockerInput di, final int poc, final DecoderState decoderState) {
        super(sh, di, poc, decoderState);
        this.mapper = mapper;
    }

    public void decode16x16(final MBlock mBlock, final Picture mb, final Frame[][] refs, final PartPred p0) {
        final int mbX = mapper.getMbX(mBlock.mbIdx);
        final int mbY = mapper.getMbY(mBlock.mbIdx);
        final boolean leftAvailable = mapper.leftAvailable(mBlock.mbIdx);
        final boolean topAvailable = mapper.topAvailable(mBlock.mbIdx);

        final boolean topLeftAvailable = mapper.topLeftAvailable(mBlock.mbIdx);
        final boolean topRightAvailable = mapper.topRightAvailable(mBlock.mbIdx);
        final int address = mapper.getAddress(mBlock.mbIdx);
        final int xx = mbX << 2;

        for (int list = 0; list < 2; list++) {
            predictInter16x16(mBlock, mbb[list], refs, mbX, mbY, leftAvailable, topAvailable, topLeftAvailable,
                topRightAvailable, mBlock.x, xx, list, p0);
        }

        PredictionMerger.mergePrediction(sh, mBlock.x.mv0R(0), mBlock.x.mv1R(0), p0, 0, mbb[0].getPlaneData(0),
            mbb[1].getPlaneData(0), 0, 16, 16, 16, mb.getPlaneData(0), refs, poc);

        mBlock.partPreds[0] = mBlock.partPreds[1] = mBlock.partPreds[2] = mBlock.partPreds[3] = p0;
        predictChromaInter(refs, mBlock.x, mbX << 3, mbY << 3, 1, mb, mBlock.partPreds);
        predictChromaInter(refs, mBlock.x, mbX << 3, mbY << 3, 2, mb, mBlock.partPreds);

        residualInter(mBlock, mapper.getAddress(mBlock.mbIdx));

        saveMvs(di, mBlock.x, mbX, mbY);

        mergeResidual(mb, mBlock.ac, mBlock.transform8x8Used ? COMP_BLOCK_8x8_LUT : COMP_BLOCK_4x4_LUT,
            mBlock.transform8x8Used ? COMP_POS_8x8_LUT : COMP_POS_4x4_LUT);

        collectPredictors(s, mb, mbX);

        di.mbTypes[address] = mBlock.curMbType;
    }

    private void predictInter8x16(final MBlock mBlock, final Picture mb, final Picture[][] references, final int mbX, final int mbY,
                                  final boolean leftAvailable, final boolean topAvailable, final boolean tlAvailable, final boolean trAvailable, final MvList x,
                                  final int list, final PartPred p0, final PartPred p1) {
        final int xx = mbX << 2;

        int mvX1 = 0, mvY1 = 0, r1 = -1, mvX2 = 0, mvY2 = 0, r2 = -1;
        if (H264Const.usesList(p0, list)) {
            final int mvpX1 = calcMVPrediction8x16Left(s.mvLeft.getMv(0, list), s.mvTop.getMv(mbX << 2, list),
                s.mvTop.getMv((mbX << 2) + 2, list), s.mvTopLeft.getMv(0, list), leftAvailable, topAvailable,
                topAvailable, tlAvailable, mBlock.pb168x168.refIdx1[list], 0);
            final int mvpY1 = calcMVPrediction8x16Left(s.mvLeft.getMv(0, list), s.mvTop.getMv(mbX << 2, list),
                s.mvTop.getMv((mbX << 2) + 2, list), s.mvTopLeft.getMv(0, list), leftAvailable, topAvailable,
                topAvailable, tlAvailable, mBlock.pb168x168.refIdx1[list], 1);

            mvX1 = mBlock.pb168x168.mvdX1[list] + mvpX1;
            mvY1 = mBlock.pb168x168.mvdY1[list] + mvpY1;

            interpolator.getBlockLuma(references[list][mBlock.pb168x168.refIdx1[list]], mb, 0, (mbX << 6) + mvX1,
                (mbY << 6) + mvY1, 8, 16);
            r1 = mBlock.pb168x168.refIdx1[list];
        }

        // Horizontal motion vector range does not exceed the range of -2048 to 2047.75, inclusive, in units of luma
        // samples. Vertical MV [-512,+511.75]. I.e. 14 + 12 bits = 26 bits. Ref Idx 6 bit ?
        final int v1 = packMv(mvX1, mvY1, r1);
        if (H264Const.usesList(p1, list)) {
            final int mvpX2 = calcMVPrediction8x16Right(v1, s.mvTop.getMv((mbX << 2) + 2, list),
                s.mvTop.getMv((mbX << 2) + 4, list), s.mvTop.getMv((mbX << 2) + 1, list), true, topAvailable,
                trAvailable, topAvailable, mBlock.pb168x168.refIdx2[list], 0);
            final int mvpY2 = calcMVPrediction8x16Right(v1, s.mvTop.getMv((mbX << 2) + 2, list),
                s.mvTop.getMv((mbX << 2) + 4, list), s.mvTop.getMv((mbX << 2) + 1, list), true, topAvailable,
                trAvailable, topAvailable, mBlock.pb168x168.refIdx2[list], 1);

            mvX2 = mBlock.pb168x168.mvdX2[list] + mvpX2;
            mvY2 = mBlock.pb168x168.mvdY2[list] + mvpY2;

            interpolator.getBlockLuma(references[list][mBlock.pb168x168.refIdx2[list]], mb, 8, (mbX << 6) + 32
                + mvX2, (mbY << 6) + mvY2, 8, 16);
            r2 = mBlock.pb168x168.refIdx2[list];
        }
        final int v2 = packMv(mvX2, mvY2, r2);

        s.mvTopLeft.setMv(0, list, s.mvTop.getMv(xx + 3, list));
        saveVect(s.mvTop, list, xx, xx + 2, v1);
        saveVect(s.mvTop, list, xx + 2, xx + 4, v2);
        saveVect(s.mvLeft, list, 0, 4, v2);

        for (int i = 0; i < 16; i += 4) {
            x.setMv(i, list, v1);
            x.setMv(i + 1, list, v1);
            x.setMv(i + 2, list, v2);
            x.setMv(i + 3, list, v2);
        }
    }

    private void predictInter16x8(final MBlock mBlock, final Picture mb, final Picture[][] references, final int mbX, final int mbY,
                                  final boolean leftAvailable, final boolean topAvailable, final boolean tlAvailable, final boolean trAvailable, final int xx, final MvList x,
                                  final PartPred p0, final PartPred p1, final int list) {
        int mvX1 = 0, mvY1 = 0, mvX2 = 0, mvY2 = 0, r1 = -1, r2 = -1;
        if (H264Const.usesList(p0, list)) {

            final int mvpX1 = calcMVPrediction16x8Top(s.mvLeft.getMv(0, list), s.mvTop.getMv(mbX << 2, list),
                s.mvTop.getMv((mbX << 2) + 4, list), s.mvTopLeft.getMv(0, list), leftAvailable, topAvailable,
                trAvailable, tlAvailable, mBlock.pb168x168.refIdx1[list], 0);
            final int mvpY1 = calcMVPrediction16x8Top(s.mvLeft.getMv(0, list), s.mvTop.getMv(mbX << 2, list),
                s.mvTop.getMv((mbX << 2) + 4, list), s.mvTopLeft.getMv(0, list), leftAvailable, topAvailable,
                trAvailable, tlAvailable, mBlock.pb168x168.refIdx1[list], 1);

            mvX1 = mBlock.pb168x168.mvdX1[list] + mvpX1;
            mvY1 = mBlock.pb168x168.mvdY1[list] + mvpY1;

            interpolator.getBlockLuma(references[list][mBlock.pb168x168.refIdx1[list]], mb, 0, (mbX << 6) + mvX1,
                (mbY << 6) + mvY1, 16, 8);
            r1 = mBlock.pb168x168.refIdx1[list];
        }
        final int v1 = packMv(mvX1, mvY1, r1);

        if (H264Const.usesList(p1, list)) {
            final int mvpX2 = calcMVPrediction16x8Bottom(s.mvLeft.getMv(2, list), v1, NULL_VECTOR, s.mvLeft.getMv(1, list),
                leftAvailable, true, false, leftAvailable, mBlock.pb168x168.refIdx2[list], 0);
            final int mvpY2 = calcMVPrediction16x8Bottom(s.mvLeft.getMv(2, list), v1, NULL_VECTOR, s.mvLeft.getMv(1, list),
                leftAvailable, true, false, leftAvailable, mBlock.pb168x168.refIdx2[list], 1);

            mvX2 = mBlock.pb168x168.mvdX2[list] + mvpX2;
            mvY2 = mBlock.pb168x168.mvdY2[list] + mvpY2;

            interpolator.getBlockLuma(references[list][mBlock.pb168x168.refIdx2[list]], mb, 128,
                (mbX << 6) + mvX2, (mbY << 6) + 32 + mvY2, 16, 8);
            r2 = mBlock.pb168x168.refIdx2[list];
        }
        final int v2 = packMv(mvX2, mvY2, r2);

        s.mvTopLeft.setMv(0, list, s.mvTop.getMv(xx + 3, list));
        saveVect(s.mvLeft, list, 0, 2, v1);
        saveVect(s.mvLeft, list, 2, 4, v2);
        saveVect(s.mvTop, list, xx, xx + 4, v2);

        for (int i = 0; i < 8; i++) {
            x.setMv(i, list, v1);
        }
        for (int i = 8; i < 16; i++) {
            x.setMv(i, list, v2);
        }
    }

    public void decode16x8(final MBlock mBlock, final Picture mb, final Frame[][] refs, final PartPred p0, final PartPred p1) {
        final int mbX = mapper.getMbX(mBlock.mbIdx);
        final int mbY = mapper.getMbY(mBlock.mbIdx);
        final boolean leftAvailable = mapper.leftAvailable(mBlock.mbIdx);
        final boolean topAvailable = mapper.topAvailable(mBlock.mbIdx);
        final boolean topLeftAvailable = mapper.topLeftAvailable(mBlock.mbIdx);
        final boolean topRightAvailable = mapper.topRightAvailable(mBlock.mbIdx);
        final int address = mapper.getAddress(mBlock.mbIdx);
        final int xx = mbX << 2;

        for (int list = 0; list < 2; list++) {
            predictInter16x8(mBlock, mbb[list], refs, mbX, mbY, leftAvailable, topAvailable, topLeftAvailable,
                topRightAvailable, xx, mBlock.x, p0, p1, list);
        }

        mergePrediction(sh, mBlock.x.mv0R(0), mBlock.x.mv1R(0), p0, 0, mbb[0].getPlaneData(0), mbb[1].getPlaneData(0),
            0, 16, 16, 8, mb.getPlaneData(0), refs, poc);
        mergePrediction(sh, mBlock.x.mv0R(8), mBlock.x.mv1R(8), p1, 0, mbb[0].getPlaneData(0), mbb[1].getPlaneData(0),
            128, 16, 16, 8, mb.getPlaneData(0), refs, poc);

        mBlock.partPreds[0] = mBlock.partPreds[1] = p0;
        mBlock.partPreds[2] = mBlock.partPreds[3] = p1;
        predictChromaInter(refs, mBlock.x, mbX << 3, mbY << 3, 1, mb, mBlock.partPreds);
        predictChromaInter(refs, mBlock.x, mbX << 3, mbY << 3, 2, mb, mBlock.partPreds);

        residualInter(mBlock, mapper.getAddress(mBlock.mbIdx));

        saveMvs(di, mBlock.x, mbX, mbY);

        mergeResidual(mb, mBlock.ac, mBlock.transform8x8Used ? COMP_BLOCK_8x8_LUT : COMP_BLOCK_4x4_LUT,
            mBlock.transform8x8Used ? COMP_POS_8x8_LUT : COMP_POS_4x4_LUT);

        collectPredictors(s, mb, mbX);

        di.mbTypes[address] = mBlock.curMbType;
    }

    public void decode8x16(final MBlock mBlock, final Picture mb, final Frame[][] refs, final PartPred p0, final PartPred p1) {
        final int mbX = mapper.getMbX(mBlock.mbIdx);
        final int mbY = mapper.getMbY(mBlock.mbIdx);
        final boolean leftAvailable = mapper.leftAvailable(mBlock.mbIdx);
        final boolean topAvailable = mapper.topAvailable(mBlock.mbIdx);
        final boolean topLeftAvailable = mapper.topLeftAvailable(mBlock.mbIdx);
        final boolean topRightAvailable = mapper.topRightAvailable(mBlock.mbIdx);
        final int address = mapper.getAddress(mBlock.mbIdx);

        for (int list = 0; list < 2; list++) {
            predictInter8x16(mBlock, mbb[list], refs, mbX, mbY, leftAvailable, topAvailable, topLeftAvailable,
                topRightAvailable, mBlock.x, list, p0, p1);
        }

        mergePrediction(sh, mBlock.x.mv0R(0), mBlock.x.mv1R(0), p0, 0, mbb[0].getPlaneData(0), mbb[1].getPlaneData(0),
            0, 16, 8, 16, mb.getPlaneData(0), refs, poc);
        mergePrediction(sh, mBlock.x.mv0R(2), mBlock.x.mv1R(2), p1, 0, mbb[0].getPlaneData(0), mbb[1].getPlaneData(0),
            8, 16, 8, 16, mb.getPlaneData(0), refs, poc);

        mBlock.partPreds[0] = mBlock.partPreds[2] = p0;
        mBlock.partPreds[1] = mBlock.partPreds[3] = p1;

        predictChromaInter(refs, mBlock.x, mbX << 3, mbY << 3, 1, mb, mBlock.partPreds);
        predictChromaInter(refs, mBlock.x, mbX << 3, mbY << 3, 2, mb, mBlock.partPreds);

        residualInter(mBlock, mapper.getAddress(mBlock.mbIdx));

        saveMvs(di, mBlock.x, mbX, mbY);

        mergeResidual(mb, mBlock.ac, mBlock.transform8x8Used ? COMP_BLOCK_8x8_LUT : COMP_BLOCK_4x4_LUT,
            mBlock.transform8x8Used ? COMP_POS_8x8_LUT : COMP_POS_4x4_LUT);

        collectPredictors(s, mb, mbX);

        di.mbTypes[address] = mBlock.curMbType;
    }

    void predictInter16x16(final MBlock mBlock, final Picture mb, final Picture[][] references, final int mbX, final int mbY,
                           final boolean leftAvailable, final boolean topAvailable, final boolean tlAvailable, final boolean trAvailable, final MvList x, final int xx,
                           final int list, final PartPred curPred) {
        int mvX = 0, mvY = 0, r = -1;
        if (H264Const.usesList(curPred, list)) {
            final int mvpX = calcMVPredictionMedian(s.mvLeft.getMv(0, list), s.mvTop.getMv(mbX << 2, list),
                s.mvTop.getMv((mbX << 2) + 4, list), s.mvTopLeft.getMv(0, list), leftAvailable, topAvailable,
                trAvailable, tlAvailable, mBlock.pb16x16.refIdx[list], 0);
            final int mvpY = calcMVPredictionMedian(s.mvLeft.getMv(0, list), s.mvTop.getMv(mbX << 2, list),
                s.mvTop.getMv((mbX << 2) + 4, list), s.mvTopLeft.getMv(0, list), leftAvailable, topAvailable,
                trAvailable, tlAvailable, mBlock.pb16x16.refIdx[list], 1);
            mvX = mBlock.pb16x16.mvdX[list] + mvpX;
            mvY = mBlock.pb16x16.mvdY[list] + mvpY;

            r = mBlock.pb16x16.refIdx[list];

            interpolator.getBlockLuma(references[list][r], mb, 0, (mbX << 6) + mvX, (mbY << 6) + mvY, 16, 16);
        }

        final int v = packMv(mvX, mvY, r);
        s.mvTopLeft.setMv(0, list, s.mvTop.getMv(xx + 3, list));
        saveVect(s.mvTop, list, xx, xx + 4, v);
        saveVect(s.mvLeft, list, 0, 4, v);

        for (int i = 0; i < 16; i++) {
            x.setMv(i, list, v);
        }
    }

    private void residualInter(final MBlock mBlock, final int mbAddr) {
        if (mBlock.cbpLuma() > 0 || mBlock.cbpChroma() > 0) {
            s.qp = (s.qp + mBlock.mbQPDelta + 52) % 52;
        }
        di.mbQps[0][mbAddr] = s.qp;

        residualLuma(mBlock);

        if (s.chromaFormat != ColorSpace.MONO) {
            final int qp1 = calcQpChroma(s.qp, s.chromaQpOffset[0]);
            final int qp2 = calcQpChroma(s.qp, s.chromaQpOffset[1]);

            decodeChromaResidual(mBlock, qp1, qp2);

            di.mbQps[1][mbAddr] = qp1;
            di.mbQps[2][mbAddr] = qp2;
        }

        di.tr8x8Used[mbAddr] = mBlock.transform8x8Used;
    }

    public int calcMVPrediction16x8Top(final int a, final int b, final int c, final int d, final boolean aAvb, final boolean bAvb, final boolean cAvb,
                                       final boolean dAvb, final int refIdx, final int comp) {
        if (bAvb && mvRef(b) == refIdx)
            return mvC(b, comp);
        else
            return calcMVPredictionMedian(a, b, c, d, aAvb, bAvb, cAvb, dAvb, refIdx, comp);
    }

    public int calcMVPrediction16x8Bottom(final int a, final int b, final int c, final int d, final boolean aAvb, final boolean bAvb, final boolean cAvb,
                                          final boolean dAvb, final int refIdx, final int comp) {

        if (aAvb && mvRef(a) == refIdx)
            return mvC(a, comp);
        else
            return calcMVPredictionMedian(a, b, c, d, aAvb, bAvb, cAvb, dAvb, refIdx, comp);
    }

    public int calcMVPrediction8x16Left(final int a, final int b, final int c, final int d, final boolean aAvb, final boolean bAvb, final boolean cAvb,
                                        final boolean dAvb, final int refIdx, final int comp) {

        if (aAvb && mvRef(a) == refIdx)
            return mvC(a, comp);
        else
            return calcMVPredictionMedian(a, b, c, d, aAvb, bAvb, cAvb, dAvb, refIdx, comp);
    }

    public int calcMVPrediction8x16Right(final int a, final int b, final int c, final int d, final boolean aAvb, final boolean bAvb, final boolean cAvb,
                                         final boolean dAvb, final int refIdx, final int comp) {
        final int lc = cAvb ? c : (dAvb ? d : NULL_VECTOR);

        if (mvRef(lc) == refIdx)
            return mvC(lc, comp);
        else
            return calcMVPredictionMedian(a, b, c, d, aAvb, bAvb, cAvb, dAvb, refIdx, comp);
    }
}
