/* SPDX-License-Identifier: BSD 2-Clause "Simplified" License */

package li.cil.oc2.jcodec.codecs.h264.encode;

import li.cil.oc2.jcodec.codecs.h264.H264Const;
import li.cil.oc2.jcodec.codecs.h264.H264Encoder.NonRdVector;
import li.cil.oc2.jcodec.codecs.h264.decode.BlockInterpolator;
import li.cil.oc2.jcodec.codecs.h264.decode.CoeffTransformer;
import li.cil.oc2.jcodec.codecs.h264.io.model.MBType;
import li.cil.oc2.jcodec.codecs.h264.io.model.SeqParameterSet;
import li.cil.oc2.jcodec.codecs.h264.io.write.CAVLCWriter;
import li.cil.oc2.jcodec.common.io.BitWriter;
import li.cil.oc2.jcodec.common.model.Picture;

import java.util.Arrays;

import static li.cil.oc2.jcodec.codecs.h264.H264Const.MB_DISP_OFF_LEFT;
import static li.cil.oc2.jcodec.codecs.h264.H264Const.MB_DISP_OFF_TOP;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * <p>
 * Encodes macroblock as P16x16
 *
 * @author Stanislav Vitvitskyy
 */
public final class MBWriterP16x16 {
    private final SeqParameterSet sps;
    private final Picture ref;

    private final BlockInterpolator interpolator;

    public MBWriterP16x16(final SeqParameterSet sps, final Picture ref) {
        this.sps = sps;
        this.ref = ref;
        interpolator = new BlockInterpolator();
    }

    public void encodeMacroblock(final EncodingContext ctx, final Picture pic, final int mbX, final int mbY, final BitWriter out, final EncodedMB outMB,
                                 final int qp, final NonRdVector params) {
        if (sps.numRefFrames > 1) {
            final int refIdx = decideRef();
            CAVLCWriter.writeTE(out, refIdx, sps.numRefFrames - 1);
        }
        final int partBlkSize = 4; // 16x16
        final int refIdx = 1;

        final boolean trAvb = mbY > 0 && mbX < sps.picWidthInMbsMinus1;
        final boolean tlAvb = mbX > 0 && mbY > 0;
        final int ax = ctx.mvLeftX[0];
        final int ay = ctx.mvLeftY[0];
        final boolean ar = ctx.mvLeftR[0] == refIdx;

        final int bx = ctx.mvTopX[mbX << 2];
        final int by = ctx.mvTopY[mbX << 2];
        final boolean br = ctx.mvTopR[mbX << 2] == refIdx;

        final int cx = trAvb ? ctx.mvTopX[(mbX << 2) + partBlkSize] : 0;
        final int cy = trAvb ? ctx.mvTopY[(mbX << 2) + partBlkSize] : 0;
        final boolean cr = trAvb && ctx.mvTopR[(mbX << 2) + partBlkSize] == refIdx;

        final int dx = tlAvb ? ctx.mvTopLeftX : 0;
        final int dy = tlAvb ? ctx.mvTopLeftY : 0;
        final boolean dr = tlAvb && (ctx.mvTopLeftR == refIdx);

        final int mvpx = H264EncoderUtils.median(ax, ar, bx, br, cx, cr, dx, dr, mbX > 0, mbY > 0, trAvb, tlAvb);
        final int mvpy = H264EncoderUtils.median(ay, ar, by, br, cy, cr, dy, dr, mbX > 0, mbY > 0, trAvb, tlAvb);

        // Motion estimation for the current macroblock
        CAVLCWriter.writeSE(out, params.mv[0] - mvpx); // mvdx
        CAVLCWriter.writeSE(out, params.mv[1] - mvpy); // mvdy

        final Picture mbRef = Picture.create(16, 16, sps.chromaFormatIdc);
        final int[][] mb = new int[][]{new int[256], new int[64], new int[64]};

        interpolator.getBlockLuma(ref, mbRef, 0, (mbX << 6) + params.mv[0], (mbY << 6) + params.mv[1], 16, 16);

        BlockInterpolator.getBlockChroma(ref.getPlaneData(1), ref.getPlaneWidth(1), ref.getPlaneHeight(1), mbRef.getPlaneData(1), 0, mbRef.getPlaneWidth(1), (mbX << 6) + params.mv[0], (mbY << 6) + params.mv[1], 8, 8);
        BlockInterpolator.getBlockChroma(ref.getPlaneData(2), ref.getPlaneWidth(2), ref.getPlaneHeight(2), mbRef.getPlaneData(2), 0, mbRef.getPlaneWidth(2), (mbX << 6) + params.mv[0], (mbY << 6) + params.mv[1], 8, 8);

        MBEncoderHelper.takeSubtract(pic.getPlaneData(0), pic.getPlaneWidth(0), pic.getPlaneHeight(0), mbX << 4, mbY << 4, mb[0], mbRef.getPlaneData(0), 16, 16);
        MBEncoderHelper.takeSubtract(pic.getPlaneData(1), pic.getPlaneWidth(1), pic.getPlaneHeight(1), mbX << 3, mbY << 3, mb[1], mbRef.getPlaneData(1), 8, 8);
        MBEncoderHelper.takeSubtract(pic.getPlaneData(2), pic.getPlaneWidth(2), pic.getPlaneHeight(2), mbX << 3, mbY << 3, mb[2], mbRef.getPlaneData(2), 8, 8);

        final int codedBlockPattern = getCodedBlockPattern();
        CAVLCWriter.writeUE(out, H264Const.CODED_BLOCK_PATTERN_INTER_COLOR_INV[codedBlockPattern]);

        CAVLCWriter.writeSE(out, qp - ctx.prevQp);

        luma(ctx, mb[0], mbX, mbY, out, qp, outMB.getNc());
        chroma(ctx, mb[1], mb[2], mbX, mbY, out, qp);

        MBEncoderHelper.putBlk(outMB.getPixels().getPlaneData(0), mb[0], mbRef.getPlaneData(0), 4, 0, 0, 16, 16);
        MBEncoderHelper.putBlk(outMB.getPixels().getPlaneData(1), mb[1], mbRef.getPlaneData(1), 3, 0, 0, 8, 8);
        MBEncoderHelper.putBlk(outMB.getPixels().getPlaneData(2), mb[2], mbRef.getPlaneData(2), 3, 0, 0, 8, 8);

        Arrays.fill(outMB.getMx(), params.mv[0]);
        Arrays.fill(outMB.getMy(), params.mv[1]);
        Arrays.fill(outMB.getMr(), refIdx);
        outMB.setType(MBType.P_16x16);
        outMB.setQp(qp);
        ctx.prevQp = qp;
    }

    private int getCodedBlockPattern() {
        return 47;
    }

    /**
     * Decides which reference to use
     */
    private int decideRef() {
        return 0;
    }

    private static void luma(final EncodingContext ctx, final int[] pix, final int mbX, final int mbY, final BitWriter out, final int qp, final int[] nc) {
        final int[][] ac = new int[16][16];
        for (int i = 0; i < ac.length; i++) {
            for (int j = 0; j < H264Const.PIX_MAP_SPLIT_4x4[i].length; j++) {
                ac[i][j] = pix[H264Const.PIX_MAP_SPLIT_4x4[i][j]];
            }
            CoeffTransformer.fdct4x4(ac[i]);
        }

        writeAC(ctx, mbX, out, mbX << 2, mbY << 2, ac, qp, nc);

        for (int i = 0; i < ac.length; i++) {
            CoeffTransformer.dequantizeAC(ac[i], qp, null);
            CoeffTransformer.idct4x4(ac[i]);
            for (int j = 0; j < H264Const.PIX_MAP_SPLIT_4x4[i].length; j++)
                pix[H264Const.PIX_MAP_SPLIT_4x4[i][j]] = ac[i][j];
        }
    }

    private static void chroma(final EncodingContext ctx, final int[] pix1, final int[] pix2, final int mbX, final int mbY, final BitWriter out,
                               final int qp) {
        final int[][] ac1 = new int[4][16];
        final int[][] ac2 = new int[4][16];
        for (int i = 0; i < ac1.length; i++) {
            for (int j = 0; j < H264Const.PIX_MAP_SPLIT_2x2[i].length; j++)
                ac1[i][j] = pix1[H264Const.PIX_MAP_SPLIT_2x2[i][j]];
        }
        for (int i = 0; i < ac2.length; i++) {
            for (int j = 0; j < H264Const.PIX_MAP_SPLIT_2x2[i].length; j++)
                ac2[i][j] = pix2[H264Const.PIX_MAP_SPLIT_2x2[i][j]];
        }
        MBWriterI16x16.chromaResidual(mbX, mbY, out, qp, ac1, ac2, ctx.cavlc[1], ctx.cavlc[2], ctx.leftMBType, ctx.topMBType[mbX], MBType.P_16x16);

        for (int i = 0; i < ac1.length; i++) {
            for (int j = 0; j < H264Const.PIX_MAP_SPLIT_2x2[i].length; j++)
                pix1[H264Const.PIX_MAP_SPLIT_2x2[i][j]] = ac1[i][j];
        }
        for (int i = 0; i < ac2.length; i++) {
            for (int j = 0; j < H264Const.PIX_MAP_SPLIT_2x2[i].length; j++)
                pix2[H264Const.PIX_MAP_SPLIT_2x2[i][j]] = ac2[i][j];
        }
    }

    private static void writeAC(final EncodingContext ctx, final int mbX,
                                final BitWriter out, final int mbLeftBlk, final int mbTopBlk,
                                final int[][] ac, final int qp, final int[] nc) {
        for (int bIndx = 0; bIndx < ac.length; bIndx++) {
            final int dIdx = H264Const.BLK_DISP_MAP[bIndx];
            CoeffTransformer.quantizeAC(ac[dIdx], qp);
            final int blkOffLeft = MB_DISP_OFF_LEFT[bIndx];
            final int blkOffTop = MB_DISP_OFF_TOP[bIndx];
            final int coeffToken = ctx.cavlc[0].writeACBlock(out, mbLeftBlk + blkOffLeft, mbTopBlk + blkOffTop,
                blkOffLeft == 0 ? ctx.leftMBType : MBType.P_16x16, blkOffTop == 0 ? ctx.topMBType[mbX] : MBType.P_16x16, ac[dIdx],
                H264Const.totalZeros16, 0, 16, CoeffTransformer.zigzag4x4);
            nc[dIdx] = coeffToken >> 4; // total coeff
        }
    }
}
