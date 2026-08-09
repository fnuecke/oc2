/* SPDX-License-Identifier: BSD 2-Clause "Simplified" License */

package li.cil.oc2.jcodec.codecs.h264.decode;

import li.cil.oc2.jcodec.codecs.h264.H264Const;
import li.cil.oc2.jcodec.codecs.h264.H264Utils.MvList;
import li.cil.oc2.jcodec.codecs.h264.io.model.Frame;
import li.cil.oc2.jcodec.codecs.h264.io.model.MBType;
import li.cil.oc2.jcodec.codecs.h264.io.model.SliceHeader;
import li.cil.oc2.jcodec.common.model.ColorSpace;
import li.cil.oc2.jcodec.common.model.Picture;
import li.cil.oc2.jcodec.common.tools.MathUtil;

import java.util.Arrays;

import static li.cil.oc2.jcodec.codecs.h264.H264Const.*;
import static li.cil.oc2.jcodec.codecs.h264.H264Utils.Mv.*;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * <p>
 * Base macroblock decoder that contains routines shared by many decoders
 *
 * @author The JCodec project
 */
public class MBlockDecoderBase {
    protected final DecoderState s;
    protected final SliceHeader sh;
    protected final DeblockerInput di;
    protected final int poc;
    protected final BlockInterpolator interpolator = new BlockInterpolator();
    protected final Picture[] mbb;
    protected final int[][] scalingMatrix;

    public MBlockDecoderBase(final SliceHeader sh, final DeblockerInput di, final int poc, final DecoderState decoderState) {
        this.s = decoderState;
        this.sh = sh;
        this.di = di;
        this.poc = poc;
        this.mbb = new Picture[]{Picture.create(16, 16, s.chromaFormat), Picture.create(16, 16, s.chromaFormat)};
        scalingMatrix = initScalingMatrix(sh);
    }

    void residualLuma(final MBlock mBlock) {
        if (!mBlock.transform8x8Used) {
            residualLuma4x4(mBlock);
        } else if (sh.pps.entropyCodingModeFlag) {
            residualLuma8x8CABAC(mBlock);
        } else {
            residualLuma8x8CAVLC(mBlock);
        }
    }

    private void residualLuma4x4(final MBlock mBlock) {

        for (int i = 0; i < 16; i++) {
            if ((mBlock.cbpLuma() & (1 << (i >> 2))) == 0) {
                continue;
            }

            CoeffTransformer.dequantizeAC(mBlock.ac[0][i], s.qp, getScalingList(mBlock.curMbType.intra ? 0 : 3));
            CoeffTransformer.idct4x4(mBlock.ac[0][i]);
        }
    }

    protected int[] getScalingList(final int which) {
        if (scalingMatrix == null)
            return null;
        return scalingMatrix[which];
    }

    protected static int[][] initScalingMatrix(final SliceHeader sh2) {
        if (sh2.sps.scalingMatrix == null && (sh2.pps.extended == null || sh2.pps.extended.scalingMatrix == null))
            return null;
        final int[][] merged = new int[][]{H264Const.defaultScalingList4x4Intra, null, null,
            H264Const.defaultScalingList4x4Inter, null, null, H264Const.defaultScalingList8x8Intra,
            H264Const.defaultScalingList8x8Inter, null, null, null, null};
        for (int i = 0; i < 8; i++) {
            if (sh2.sps.scalingMatrix != null && sh2.sps.scalingMatrix[i] != null)
                merged[i] = sh2.sps.scalingMatrix[i];
            if (sh2.pps.extended != null && sh2.pps.extended.scalingMatrix != null
                && sh2.pps.extended.scalingMatrix[i] != null)
                merged[i] = sh2.pps.extended.scalingMatrix[i];
        }
        if (merged[1] == null)
            merged[1] = merged[0];
        if (merged[2] == null)
            merged[2] = merged[0];
        if (merged[4] == null)
            merged[4] = merged[3];
        if (merged[5] == null)
            merged[5] = merged[3];
        if (merged[8] == null)
            merged[8] = merged[6];
        if (merged[10] == null)
            merged[10] = merged[6];
        if (merged[9] == null)
            merged[9] = merged[7];
        if (merged[11] == null)
            merged[11] = merged[7];
        return merged;
    }

    private void residualLuma8x8CABAC(final MBlock mBlock) {

        for (int i = 0; i < 4; i++) {
            if ((mBlock.cbpLuma() & (1 << i)) == 0) {
                continue;
            }

            CoeffTransformer.dequantizeAC8x8(mBlock.ac[0][i], s.qp, getScalingList(mBlock.curMbType.intra ? 6 : 7));
            CoeffTransformer.idct8x8(mBlock.ac[0][i]);
        }
    }

    private void residualLuma8x8CAVLC(final MBlock mBlock) {

        for (int i = 0; i < 4; i++) {
            if ((mBlock.cbpLuma() & (1 << i)) == 0) {
                continue;
            }

            CoeffTransformer.dequantizeAC8x8(mBlock.ac[0][i], s.qp, getScalingList(mBlock.curMbType.intra ? 6 : 7));
            CoeffTransformer.idct8x8(mBlock.ac[0][i]);
        }
    }

    public void decodeChroma(final MBlock mBlock, final int mbX, final int mbY,
                             final boolean leftAvailable, final boolean topAvailable,
                             final Picture mb, final int qp) {

        if (s.chromaFormat == ColorSpace.MONO) {
            Arrays.fill(mb.getPlaneData(1), (byte) 0);
            Arrays.fill(mb.getPlaneData(2), (byte) 0);
            return;
        }

        final int qp1 = calcQpChroma(qp, s.chromaQpOffset[0]);
        final int qp2 = calcQpChroma(qp, s.chromaQpOffset[1]);

        if (mBlock.cbpChroma() != 0) {
            decodeChromaResidual(mBlock, qp1, qp2);
        }

        final int addr = mbY * (sh.sps.picWidthInMbsMinus1 + 1) + mbX;
        di.mbQps[1][addr] = qp1;
        di.mbQps[2][addr] = qp2;
        ChromaPredictionBuilder.predictWithMode(mBlock.ac[1], mBlock.chromaPredictionMode, mbX, leftAvailable,
            topAvailable, s.leftRow[1], s.topLine[1], s.topLeft[1], mb.getPlaneData(1));
        ChromaPredictionBuilder.predictWithMode(mBlock.ac[2], mBlock.chromaPredictionMode, mbX, leftAvailable,
            topAvailable, s.leftRow[2], s.topLine[2], s.topLeft[2], mb.getPlaneData(2));
    }

    void decodeChromaResidual(final MBlock mBlock, final int crQp1, final int crQp2) {
        if (mBlock.cbpChroma() != 0) {
            if ((mBlock.cbpChroma() & 3) > 0) {
                chromaDC(mBlock.dc1, 1, crQp1, mBlock.curMbType);
                chromaDC(mBlock.dc2, 2, crQp2, mBlock.curMbType);
            }

            chromaAC(mBlock.dc1, 1, crQp1, mBlock.curMbType,
                (mBlock.cbpChroma() & 2) > 0, mBlock.ac[1]);
            chromaAC(mBlock.dc2, 2, crQp2, mBlock.curMbType,
                (mBlock.cbpChroma() & 2) > 0, mBlock.ac[2]);
        }
    }

    private void chromaDC(final int[] dc, final int comp, final int crQp, final MBType curMbType) {
        CoeffTransformer.invDC2x2(dc);
        CoeffTransformer.dequantizeDC2x2(dc, crQp, getScalingList((curMbType.intra ? 6 : 7) + comp * 2));
    }

    private void chromaAC(final int[] dc, final int comp, final int crQp, final MBType curMbType,
                          final boolean codedAC, final int[][] residualOut) {
        for (int i = 0; i < dc.length; i++) {
            final int[] ac = residualOut[i];

            if (codedAC) {
                CoeffTransformer.dequantizeAC(ac, crQp, getScalingList((curMbType.intra ? 0 : 3) + comp));
            }
            ac[0] = dc[i];

            CoeffTransformer.idct4x4(ac);
        }
    }

    static int calcQpChroma(final int qp, final int crQpOffset) {
        return QP_SCALE_CR[MathUtil.clip(qp + crQpOffset, 0, 51)];
    }

    public void predictChromaInter(final Frame[][] refs, final MvList vectors, final int x, final int y, final int comp,
                                   final Picture mb, final PartPred[] predType) {
        for (int blk8x8 = 0; blk8x8 < 4; blk8x8++) {
            for (int list = 0; list < 2; list++) {
                if (!H264Const.usesList(predType[blk8x8], list))
                    continue;
                for (int blk4x4 = 0; blk4x4 < 4; blk4x4++) {
                    final int i = BLK_DISP_MAP[(blk8x8 << 2) + blk4x4];
                    final int mv = vectors.getMv(i, list);
                    final Picture ref = refs[list][mvRef(mv)];

                    final int blkPox = (i & 3) << 1;
                    final int blkPoy = (i >> 2) << 1;

                    final int xx = ((x + blkPox) << 3) + mvX(mv);
                    final int yy = ((y + blkPoy) << 3) + mvY(mv);

                    BlockInterpolator.getBlockChroma(ref.getPlaneData(comp), ref.getPlaneWidth(comp),
                        ref.getPlaneHeight(comp), mbb[list].getPlaneData(comp), blkPoy * mb.getPlaneWidth(comp)
                            + blkPox, mb.getPlaneWidth(comp), xx, yy, 2, 2);
                }
            }

            final int blk4x4 = BLK8x8_BLOCKS[blk8x8][0];
            PredictionMerger.mergePrediction(sh, vectors.mv0R(blk4x4), vectors.mv1R(blk4x4), predType[blk8x8], comp,
                mbb[0].getPlaneData(comp), mbb[1].getPlaneData(comp), BLK_8x8_MB_OFF_CHROMA[blk8x8],
                mb.getPlaneWidth(comp), 4, 4, mb.getPlaneData(comp), refs, poc);
        }
    }
}
