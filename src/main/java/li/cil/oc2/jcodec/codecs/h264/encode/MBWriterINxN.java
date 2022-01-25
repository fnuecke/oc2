/* SPDX-License-Identifier: BSD 2-Clause "Simplified" License */

package li.cil.oc2.jcodec.codecs.h264.encode;

import li.cil.oc2.jcodec.codecs.h264.H264Const;
import li.cil.oc2.jcodec.codecs.h264.H264Encoder.NonRdVector;
import li.cil.oc2.jcodec.codecs.h264.decode.CoeffTransformer;
import li.cil.oc2.jcodec.codecs.h264.decode.Intra4x4PredictionBuilder;
import li.cil.oc2.jcodec.codecs.h264.io.CAVLC;
import li.cil.oc2.jcodec.codecs.h264.io.model.MBType;
import li.cil.oc2.jcodec.codecs.h264.io.write.CAVLCWriter;
import li.cil.oc2.jcodec.common.io.BitWriter;
import li.cil.oc2.jcodec.common.model.Picture;
import li.cil.oc2.jcodec.common.tools.MathUtil;

import static li.cil.oc2.jcodec.codecs.h264.H264Const.BLK_DISP_MAP;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * <p>
 * Encodes macroblock as I16x16
 *
 * @author Stanislav Vitvitskyy
 */
public final class MBWriterINxN {
    public void encodeMacroblock(final EncodingContext ctx, final Picture pic, final int mbX, final int mbY, final BitWriter out, final EncodedMB outMB,
                                 final int qp, final NonRdVector params) {
        for (int bInd = 0; bInd < 16; bInd++) {
            final int blkX = H264Const.MB_DISP_OFF_LEFT[bInd];
            final int blkY = H264Const.MB_DISP_OFF_TOP[bInd];
            writePredictionI4x4Block(out, mbX > 0, mbY > 0, ctx.leftMBType, ctx.topMBType[mbX], blkX, blkY, mbX,
                ctx.i4x4PredTop, ctx.i4x4PredLeft, params.lumaPred4x4[bInd]);
        }

        final int[][] coeff = new int[16][16];
        final int cbpLuma = lumaAnal(ctx, pic, mbX, mbY, qp, outMB, params.lumaPred4x4, coeff);
        final int cbpChroma = 2;
        final int cbp = cbpLuma | (cbpChroma << 4);
        CAVLCWriter.writeUE(out, params.chrPred);
        CAVLCWriter.writeUE(out, H264Const.CODED_BLOCK_PATTERN_INTRA_COLOR_INV[cbp]);
        if (cbp != 0)
            CAVLCWriter.writeSE(out, qp - ctx.prevQp); // MB QP delta

        outMB.setType(MBType.I_NxN);
        outMB.setQp(qp);

        lumaCode(ctx, mbX, mbY, out, outMB, coeff, cbpLuma);
        MBWriterI16x16.chroma(ctx, pic, mbX, mbY, MBType.I_NxN, out, qp, outMB.getPixels(), params.chrPred);
        ctx.prevQp = qp;
    }

    private void writePredictionI4x4Block(final BitWriter out, final boolean leftAvailable, final boolean topAvailable, final MBType leftMBType,
                                          final MBType topMBType, final int blkX, final int blkY, final int mbX, final int[] i4x4PredTop, final int[] i4x4PredLeft, final int mode) {
        int predMode = 2;
        if ((leftAvailable || blkX > 0) && (topAvailable || blkY > 0)) {
            final int predModeB = topMBType == MBType.I_NxN || blkY > 0 ? i4x4PredTop[(mbX << 2) + blkX] : 2;
            final int predModeA = leftMBType == MBType.I_NxN || blkX > 0 ? i4x4PredLeft[blkY] : 2;
            predMode = Math.min(predModeB, predModeA);
        }
        final boolean prev4x4PredMode = mode == predMode;
        out.write1Bit(prev4x4PredMode ? 1 : 0);
        if (!prev4x4PredMode) {
            final int wrMode = mode - (mode > predMode ? 1 : 0);
            out.writeNBit(wrMode, 3);
        }
        i4x4PredTop[(mbX << 2) + blkX] = i4x4PredLeft[blkY] = mode;
    }

    private int lumaAnal(final EncodingContext ctx, final Picture pic, final int mbX, final int mbY, final int qp, final EncodedMB outMB,
                         final int[] predType, final int[][] _coeff) {
        int cbp = 0;
        final byte[] pred = new byte[16];
        final int[] coeff = new int[16];
        final byte[] tl = new byte[]{ctx.topLeft[0], ctx.leftRow[0][3], ctx.leftRow[0][7], ctx.leftRow[0][11]};
        for (int i8x8 = 0; i8x8 < 4; i8x8++) {
            boolean hasNz = false;
            for (int i4x4 = 0; i4x4 < 4; i4x4++) {
                final int bIdx = (i8x8 << 2) + i4x4;
                final int blkOffLeft = H264Const.MB_DISP_OFF_LEFT[bIdx];
                final int blkOffTop = H264Const.MB_DISP_OFF_TOP[bIdx];
                final int blkX = (mbX << 2) + blkOffLeft;
                final int blkY = (mbY << 2) + blkOffTop;

                final int dIdx = BLK_DISP_MAP[bIdx];
                final boolean hasLeft = (dIdx & 0x3) != 0 || mbX != 0;
                final boolean hasTop = dIdx >= 4 || mbY != 0;
                final boolean hasTr = ((bIdx == 0 || bIdx == 1 || bIdx == 4) && mbY != 0)
                    || (bIdx == 5 && mbX < ctx.mbWidth - 1) || bIdx == 2 || bIdx == 6 || bIdx == 8 || bIdx == 9
                    || bIdx == 10 || bIdx == 12 || bIdx == 14;

                Intra4x4PredictionBuilder.lumaPred(predType[bIdx], hasLeft, hasTop, hasTr, ctx.leftRow[0],
                    ctx.topLine[0], tl[blkOffTop], blkX << 2, blkOffTop << 2, pred);
                MBEncoderHelper.takeSubtract(pic.getPlaneData(0), pic.getPlaneWidth(0), pic.getPlaneHeight(0),
                    blkX << 2, blkY << 2, coeff, pred, 4, 4);
                CoeffTransformer.fdct4x4(coeff);
                CoeffTransformer.quantizeAC(coeff, qp);
                System.arraycopy(coeff, 0, _coeff[bIdx], 0, 16);
                hasNz |= MBWriterI16x16.hasNz(coeff);
                CoeffTransformer.dequantizeAC(coeff, qp, null);
                CoeffTransformer.idct4x4(coeff);
                MBEncoderHelper.putBlk(outMB.pixels.getPlaneData(0), coeff, pred, 4, blkOffLeft << 2, blkOffTop << 2, 4,
                    4);

                tl[blkOffTop] = ctx.topLine[0][(blkX << 2) + 3];
                for (int p = 0; p < 4; p++) {
                    ctx.leftRow[0][(blkOffTop << 2) + p] = (byte) MathUtil.clip(coeff[3 + (p << 2)] + pred[3 + (p << 2)], -128,
                        127);
                    ctx.topLine[0][(blkX << 2) + p] = (byte) MathUtil.clip(coeff[12 + p] + pred[12 + p], -128, 127);
                }
            }
            cbp |= ((hasNz ? 1 : 0) << i8x8);
        }
        ctx.topLeft[0] = tl[0];
        return cbp;
    }

    private void lumaCode(final EncodingContext ctx, final int mbX, final int mbY,
                          final BitWriter out, final EncodedMB outMB,
                          final int[][] _coeff, final int cbpLuma) {
        for (int i8x8 = 0; i8x8 < 4; i8x8++) {
            final int bx = (mbX << 2) | ((i8x8 << 1) & 2);
            final int by = (mbY << 2) | (i8x8 & 2);

            if ((cbpLuma & (1 << i8x8)) != 0) {
                for (int i4x4 = 0; i4x4 < 4; i4x4++) {
                    final int blkX = bx | (i4x4 & 1);
                    final int blkY = by | ((i4x4 >> 1) & 1);
                    final int blkOffLeft = blkX & 0x3;
                    final int blkOffTop = blkY & 0x3;
                    final int bIdx = (i8x8 << 2) + i4x4;
                    final int dIdx = BLK_DISP_MAP[bIdx];

                    outMB.nc[dIdx] = CAVLC.totalCoeff(
                        ctx.cavlc[0].writeACBlock(out, blkX, blkY, blkOffLeft == 0 ? ctx.leftMBType : MBType.I_NxN,
                            blkOffTop == 0 ? ctx.topMBType[mbX] : MBType.I_NxN, _coeff[bIdx], H264Const.totalZeros16, 0,
                            16, CoeffTransformer.zigzag4x4));

                }
            } else {
                for (int i4x4 = 0; i4x4 < 4; i4x4++) {
                    final int blkX = bx | (i4x4 & 1);
                    final int blkY = by | ((i4x4 >> 1) & 1);
                    ctx.cavlc[0].setZeroCoeff(blkX, blkY);
                }
            }
        }
    }
}
