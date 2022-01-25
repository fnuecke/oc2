/* SPDX-License-Identifier: BSD 2-Clause "Simplified" License */

package li.cil.oc2.jcodec.codecs.h264.decode;

import li.cil.oc2.jcodec.codecs.h264.H264Const;
import li.cil.oc2.jcodec.codecs.h264.decode.aso.Mapper;
import li.cil.oc2.jcodec.codecs.h264.io.model.SliceHeader;
import li.cil.oc2.jcodec.common.model.Picture;

/**
 * A decoder for I16x16 macroblocks
 *
 * @author The JCodec project
 */
public final class MBlockDecoderIntraNxN extends MBlockDecoderBase {
    private final Mapper mapper;
    private final Intra8x8PredictionBuilder prediction8x8Builder;

    public MBlockDecoderIntraNxN(final Mapper mapper, final SliceHeader sh, final DeblockerInput di, final int poc,
                                 final DecoderState decoderState) {
        super(sh, di, poc, decoderState);
        this.mapper = mapper;
        this.prediction8x8Builder = new Intra8x8PredictionBuilder();
    }

    public void decode(final MBlock mBlock, final Picture mb) {
        final int mbX = mapper.getMbX(mBlock.mbIdx);
        final int mbY = mapper.getMbY(mBlock.mbIdx);

        final int mbAddr = mapper.getAddress(mBlock.mbIdx);
        final boolean leftAvailable = mapper.leftAvailable(mBlock.mbIdx);
        final boolean topAvailable = mapper.topAvailable(mBlock.mbIdx);
        final boolean topLeftAvailable = mapper.topLeftAvailable(mBlock.mbIdx);
        final boolean topRightAvailable = mapper.topRightAvailable(mBlock.mbIdx);

        if (mBlock.cbpLuma() > 0 || mBlock.cbpChroma() > 0) {
            s.qp = (s.qp + mBlock.mbQPDelta + 52) % 52;
        }
        di.mbQps[0][mbAddr] = s.qp;

        residualLuma(mBlock);

        if (!mBlock.transform8x8Used) {
            for (int bInd = 0; bInd < 16; bInd++) {
                final int dInd = H264Const.BLK_DISP_MAP[bInd];
                final int blkX = (dInd & 3) << 2;
                final int blkY = dInd & ~3;

                final boolean trAvailable = ((bInd == 0 || bInd == 1 || bInd == 4) && topAvailable)
                    || (bInd == 5 && topRightAvailable) || bInd == 2 || bInd == 6 || bInd == 8 || bInd == 9 || bInd == 10
                    || bInd == 12 || bInd == 14;

                Intra4x4PredictionBuilder.predictWithMode(mBlock.lumaModes[bInd], mBlock.ac[0][bInd],
                    blkX != 0 || leftAvailable, blkY != 0 || topAvailable, trAvailable, s.leftRow[0],
                    s.topLine[0], s.topLeft[0], (mbX << 4), blkX, blkY, mb.getPlaneData(0));
            }
        } else {
            for (int i = 0; i < 4; i++) {
                final int blkX = (i & 1) << 1;
                final int blkY = i & 2;

                final boolean trAvailable = (i == 0 && topAvailable) || (i == 1 && topRightAvailable) || i == 2;
                final boolean tlAvailable = i == 0 ? topLeftAvailable : (i == 1 ? topAvailable : (i != 2 || leftAvailable));

                prediction8x8Builder.predictWithMode(mBlock.lumaModes[i], mBlock.ac[0][i],
                    blkX != 0 || leftAvailable, blkY != 0 || topAvailable, tlAvailable, trAvailable,
                    s.leftRow[0], s.topLine[0], s.topLeft[0], (mbX << 4), blkX << 2, blkY << 2, mb.getPlaneData(0));
            }
        }

        decodeChroma(mBlock, mbX, mbY, leftAvailable, topAvailable, mb, s.qp);

        di.mbTypes[mbAddr] = mBlock.curMbType;
        di.tr8x8Used[mbAddr] = mBlock.transform8x8Used;

        MBlockDecoderUtils.collectChromaPredictors(s, mb, mbX);

        MBlockDecoderUtils.saveMvsIntra(di, mbX, mbY);
        MBlockDecoderUtils.saveVectIntra(s, mapper.getMbX(mBlock.mbIdx));
    }
}
