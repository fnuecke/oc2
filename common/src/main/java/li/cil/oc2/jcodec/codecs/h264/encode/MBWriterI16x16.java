/* SPDX-License-Identifier: BSD 2-Clause "Simplified" License */

package li.cil.oc2.jcodec.codecs.h264.encode;

import li.cil.oc2.jcodec.codecs.h264.H264Const;
import li.cil.oc2.jcodec.codecs.h264.H264Encoder.NonRdVector;
import li.cil.oc2.jcodec.codecs.h264.decode.ChromaPredictionBuilder;
import li.cil.oc2.jcodec.codecs.h264.decode.CoeffTransformer;
import li.cil.oc2.jcodec.codecs.h264.decode.Intra16x16PredictionBuilder;
import li.cil.oc2.jcodec.codecs.h264.io.CAVLC;
import li.cil.oc2.jcodec.codecs.h264.io.model.MBType;
import li.cil.oc2.jcodec.codecs.h264.io.write.CAVLCWriter;
import li.cil.oc2.jcodec.common.io.BitWriter;
import li.cil.oc2.jcodec.common.model.Picture;
import li.cil.oc2.jcodec.common.tools.MathUtil;

import static li.cil.oc2.jcodec.codecs.h264.H264Const.*;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * <p>
 * Encodes macroblock as I16x16
 *
 * @author Stanislav Vitvitskyy
 */
public final class MBWriterI16x16 {
    public boolean encodeMacroblock(final EncodingContext ctx, final Picture pic, final int mbX, final int mbY,
                                    final BitWriter out, final EncodedMB outMB,
                                    final int qp, final NonRdVector params) {
        CAVLCWriter.writeUE(out, params.chrPred);
        CAVLCWriter.writeSE(out, qp - ctx.prevQp); // MB QP delta

        outMB.setType(MBType.I_16x16);
        outMB.setQp(qp);

        boolean cbp = false;
        final int[] nc = new int[16];
        luma(ctx, pic, mbX, mbY, out, qp, outMB.getPixels(), params.lumaPred16x16, nc);
        for (int dInd = 0; dInd < 16; dInd++) {
            cbp |= nc[dInd] != 0;
        }
        chroma(ctx, pic, mbX, mbY, MBType.I_16x16, out, qp, outMB.getPixels(), params.chrPred);
        ctx.prevQp = qp;
        return cbp;
    }

    private static int calcQpChroma(final int qp) {
        return QP_SCALE_CR[MathUtil.clip(qp, 0, 51)];
    }

    public static void chroma(final EncodingContext ctx, final Picture pic, final int mbX, final int mbY, final MBType curMBType, final BitWriter out,
                              final int qp, final Picture outMB, final int chrPred) {
        final int x = mbX << 3;
        final int y = mbY << 3;
        final int[][] ac1 = new int[4][16];
        final int[][] ac2 = new int[4][16];
        final byte[][] pred1 = new byte[4][16];
        final byte[][] pred2 = new byte[4][16];

        predictChroma(ctx, pic, ac1, pred1, 1, x, y, chrPred);
        predictChroma(ctx, pic, ac2, pred2, 2, x, y, chrPred);

        chromaResidual(mbX, mbY, out, qp, ac1, ac2, ctx.cavlc[1], ctx.cavlc[2], ctx.leftMBType, ctx.topMBType[mbX],
            curMBType);

        putChroma(outMB.getData()[1], ac1, pred1);
        putChroma(outMB.getData()[2], ac2, pred2);
    }

    public static void chromaResidual(final int mbX, final int mbY, final BitWriter out, final int qp, final int[][] ac1, final int[][] ac2, final CAVLC cavlc1,
                                      final CAVLC cavlc2, final MBType leftMBType, final MBType topMBType, final MBType curMBType) {
        final int chrQp = calcQpChroma(qp);

        transformChroma(ac1);
        transformChroma(ac2);

        final int[] dc1 = extractDC(ac1);
        final int[] dc2 = extractDC(ac2);

        writeDC(cavlc1, out, chrQp, mbX << 1, mbY << 1, dc1, leftMBType, topMBType);
        writeDC(cavlc2, out, chrQp, mbX << 1, mbY << 1, dc2, leftMBType, topMBType);

        writeAC(cavlc1, out, mbX << 1, mbY << 1, ac1, chrQp, leftMBType, topMBType, curMBType);
        writeAC(cavlc2, out, mbX << 1, mbY << 1, ac2, chrQp, leftMBType, topMBType, curMBType);

        restorePlane(dc1, ac1, chrQp);
        restorePlane(dc2, ac2, chrQp);
    }

    private void luma(final EncodingContext ctx, final Picture pic, final int mbX, final int mbY, final BitWriter out, final int qp, final Picture outMB,
                      final int predType, final int[] nc) {
        final int x = mbX << 4;
        final int y = mbY << 4;
        final int[][] ac = new int[16][16];
        final byte[][] pred = new byte[16][16];

        Intra16x16PredictionBuilder.lumaPred(predType, x != 0, y != 0, ctx.leftRow[0], ctx.topLine[0], ctx.topLeft[0],
            x, pred);

        transform(pic, ac, pred, x, y);
        final int[] dc = extractDC(ac);
        writeDC(ctx.cavlc[0], out, qp, mbX << 2, mbY << 2, dc, ctx.leftMBType, ctx.topMBType[mbX]);
        writeACLum(ctx.cavlc[0], out, mbX << 2, mbY << 2, ac, qp, ctx.leftMBType, ctx.topMBType[mbX],
            nc);

        restorePlane(dc, ac, qp);

        for (int blk = 0; blk < ac.length; blk++) {
            MBEncoderHelper.putBlk(outMB.getPlaneData(0), ac[blk], pred[blk], 4, BLK_X[blk], BLK_Y[blk], 4, 4);
        }
    }

    private static void putChroma(final byte[] mb, final int[][] ac, final byte[][] pred) {
        MBEncoderHelper.putBlk(mb, ac[0], pred[0], 3, 0, 0, 4, 4);

        MBEncoderHelper.putBlk(mb, ac[1], pred[1], 3, 4, 0, 4, 4);

        MBEncoderHelper.putBlk(mb, ac[2], pred[2], 3, 0, 4, 4, 4);

        MBEncoderHelper.putBlk(mb, ac[3], pred[3], 3, 4, 4, 4, 4);
    }

    private static void restorePlane(final int[] dc, final int[][] ac, final int qp) {
        if (dc.length == 4) {
            CoeffTransformer.invDC2x2(dc);
            CoeffTransformer.dequantizeDC2x2(dc, qp, null);
        } else if (dc.length == 8) {
            CoeffTransformer.invDC4x2();
            CoeffTransformer.dequantizeDC4x2();
        } else {
            CoeffTransformer.invDC4x4(dc);
            CoeffTransformer.dequantizeDC4x4(dc, qp, null);
            CoeffTransformer.reorderDC4x4(dc);
        }
        for (int i = 0; i < ac.length; i++) {
            CoeffTransformer.dequantizeAC(ac[i], qp, null);
            ac[i][0] = dc[i];
            CoeffTransformer.idct4x4(ac[i]);
        }
    }

    private static int[] extractDC(final int[][] ac) {
        final int[] dc = new int[ac.length];
        for (int i = 0; i < ac.length; i++) {
            dc[i] = ac[i][0];
            ac[i][0] = 0;
        }
        return dc;
    }

    private static void writeAC(final CAVLC cavlc, final BitWriter out, final int mbLeftBlk, final int mbTopBlk, final int[][] ac,
                                final int qp, final MBType leftMBType, final MBType topMBType, final MBType curMBType) {
        for (int bInd = 0; bInd < ac.length; bInd++) {
            CoeffTransformer.quantizeAC(ac[bInd], qp);
            final int blkOffLeft = MB_DISP_OFF_LEFT[bInd];
            final int blkOffTop = MB_DISP_OFF_TOP[bInd];
            cavlc.writeACBlock(out, mbLeftBlk + blkOffLeft, mbTopBlk + blkOffTop,
                blkOffLeft == 0 ? leftMBType : curMBType, blkOffTop == 0 ? topMBType : curMBType, ac[bInd],
                H264Const.totalZeros16, 1, 15, CoeffTransformer.zigzag4x4);
        }
    }

    private static void writeACLum(final CAVLC cavlc, final BitWriter out, final int mbLeftBlk, final int mbTopBlk,
                                   final int[][] ac, final int qp, final MBType leftMBType, final MBType topMBType, final int[] nc) {
        boolean code = false;
        for (int bInd = 0; bInd < 16; bInd++) {
            CoeffTransformer.quantizeAC(ac[bInd], qp);
            code |= hasNz(ac[bInd]);
        }
        if (code) {
            for (int bInd = 0; bInd < 16; bInd++) {
                final int blkOffLeft = MB_DISP_OFF_LEFT[bInd];
                final int blkOffTop = MB_DISP_OFF_TOP[bInd];
                nc[BLK_DISP_MAP[bInd]] = CAVLC
                    .totalCoeff(cavlc.writeACBlock(out, mbLeftBlk + blkOffLeft, mbTopBlk + blkOffTop,
                        blkOffLeft == 0 ? leftMBType : MBType.I_16x16, blkOffTop == 0 ? topMBType : MBType.I_16x16,
                        ac[bInd], H264Const.totalZeros16, 1, 15, CoeffTransformer.zigzag4x4));
            }
        } else {
            for (int bInd = 0; bInd < 16; bInd++) {
                final int blkOffLeft = MB_DISP_OFF_LEFT[bInd];
                final int blkOffTop = MB_DISP_OFF_TOP[bInd];
                cavlc.setZeroCoeff(mbLeftBlk + blkOffLeft, mbTopBlk + blkOffTop);
            }
        }
    }

    public static boolean hasNz(final int[] ac) {
        int val = 0;
        for (int i = 0; i < 16; i++)
            val |= ac[i];
        return val != 0;
    }

    private static void writeDC(final CAVLC cavlc, final BitWriter out, final int qp, final int mbLeftBlk, final int mbTopBlk,
                                final int[] dc, final MBType leftMBType, final MBType topMBType) {
        if (dc.length == 4) {
            CoeffTransformer.quantizeDC2x2(dc, qp);
            CoeffTransformer.fvdDC2x2(dc);
            cavlc.writeChrDCBlock(out, dc, H264Const.totalZeros4, 0, dc.length, new int[]{0, 1, 2, 3});
        } else if (dc.length == 8) {
            CoeffTransformer.quantizeDC4x2();
            CoeffTransformer.fvdDC4x2();
            cavlc.writeChrDCBlock(out, dc, H264Const.totalZeros8, 0, dc.length, new int[]{0, 1, 2, 3, 4, 5, 6, 7});
        } else {
            CoeffTransformer.reorderDC4x4(dc);
            CoeffTransformer.quantizeDC4x4(dc, qp);
            CoeffTransformer.fvdDC4x4(dc);
            // TODO: calc here
            cavlc.writeLumaDCBlock(out, mbLeftBlk, mbTopBlk, leftMBType, topMBType, dc, H264Const.totalZeros16, 0, 16,
                CoeffTransformer.zigzag4x4);
        }
    }

    private static void transformChroma(final int[][] ac) {
        for (int i = 0; i < 4; i++) {
            CoeffTransformer.fdct4x4(ac[i]);
        }
    }

    private static void predictChroma(final EncodingContext ctx, final Picture pic, final int[][] ac, final byte[][] pred, final int comp, final int x,
                                      final int y, final int mode) {
        ChromaPredictionBuilder.buildPred(mode, x >> 3, x != 0, y != 0, ctx.leftRow[comp], ctx.topLine[comp], ctx.topLeft[comp], pred);

        MBEncoderHelper.takeSubtract(pic.getPlaneData(comp), pic.getPlaneWidth(comp), pic.getPlaneHeight(comp), x, y, ac[0], pred[0], 4, 4);
        MBEncoderHelper.takeSubtract(pic.getPlaneData(comp), pic.getPlaneWidth(comp), pic.getPlaneHeight(comp), x + 4, y, ac[1], pred[1], 4, 4);
        MBEncoderHelper.takeSubtract(pic.getPlaneData(comp), pic.getPlaneWidth(comp), pic.getPlaneHeight(comp), x, y + 4, ac[2], pred[2], 4, 4);
        MBEncoderHelper.takeSubtract(pic.getPlaneData(comp), pic.getPlaneWidth(comp), pic.getPlaneHeight(comp), x + 4, y + 4, ac[3], pred[3], 4, 4);
    }

    private void transform(final Picture pic, final int[][] ac, final byte[][] pred, final int x, final int y) {
        for (int i = 0; i < ac.length; i++) {
            final int[] coeff = ac[i];
            MBEncoderHelper.takeSubtract(pic.getPlaneData(0), pic.getPlaneWidth(0), pic.getPlaneHeight(0), x + BLK_X[i], y + BLK_Y[i], coeff, pred[i], 4, 4);
            CoeffTransformer.fdct4x4(coeff);
        }
    }

    public int getCbpChroma() {
        return 2;
    }
}
