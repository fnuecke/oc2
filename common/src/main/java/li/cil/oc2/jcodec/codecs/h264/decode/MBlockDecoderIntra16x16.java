/* SPDX-License-Identifier: BSD 2-Clause "Simplified" License */

package li.cil.oc2.jcodec.codecs.h264.decode;

import li.cil.oc2.jcodec.codecs.h264.decode.aso.Mapper;
import li.cil.oc2.jcodec.codecs.h264.io.model.SliceHeader;
import li.cil.oc2.jcodec.common.model.Picture;

import static li.cil.oc2.jcodec.codecs.h264.decode.MBlockDecoderUtils.*;

/**
 * A decoder for I16x16 macroblocks
 *
 * @author The JCodec project
 */
public final class MBlockDecoderIntra16x16 extends MBlockDecoderBase {
    private final Mapper mapper;

    public MBlockDecoderIntra16x16(final Mapper mapper, final SliceHeader sh, final DeblockerInput di, final int poc,
                                   final DecoderState decoderState) {
        super(sh, di, poc, decoderState);
        this.mapper = mapper;
    }

    public void decode(final MBlock mBlock, final Picture mb) {
        final int mbX = mapper.getMbX(mBlock.mbIdx);
        final int mbY = mapper.getMbY(mBlock.mbIdx);
        final int address = mapper.getAddress(mBlock.mbIdx);
        final boolean leftAvailable = mapper.leftAvailable(mBlock.mbIdx);
        final boolean topAvailable = mapper.topAvailable(mBlock.mbIdx);
        s.qp = (s.qp + mBlock.mbQPDelta + 52) % 52;
        di.mbQps[0][address] = s.qp;

        residualLumaI16x16(mBlock);

        Intra16x16PredictionBuilder.predictWithMode(mBlock.luma16x16Mode, mBlock.ac[0], leftAvailable, topAvailable,
            s.leftRow[0], s.topLine[0], s.topLeft[0], mbX << 4, mb.getPlaneData(0));

        decodeChroma(mBlock, mbX, mbY, leftAvailable, topAvailable, mb, s.qp);
        di.mbTypes[address] = mBlock.curMbType;

        collectPredictors(s, mb, mbX);
        saveMvsIntra(di, mbX, mbY);
        saveVectIntra(s, mapper.getMbX(mBlock.mbIdx));
    }

    private void residualLumaI16x16(final MBlock mBlock) {
        CoeffTransformer.invDC4x4(mBlock.dc);
        final int[] scalingList = getScalingList(0);
        CoeffTransformer.dequantizeDC4x4(mBlock.dc, s.qp, scalingList);
        CoeffTransformer.reorderDC4x4(mBlock.dc);

        for (int bInd = 0; bInd < 16; bInd++) {
            final int ind8x8 = bInd >> 2;
            final int mask = 1 << ind8x8;
            if ((mBlock.cbpLuma() & mask) != 0) {
                CoeffTransformer.dequantizeAC(mBlock.ac[0][bInd], s.qp, scalingList);
            }
            mBlock.ac[0][bInd][0] = mBlock.dc[bInd];
            CoeffTransformer.idct4x4(mBlock.ac[0][bInd]);
        }
    }
}
