/* SPDX-License-Identifier: BSD 2-Clause "Simplified" License */

package li.cil.oc2.jcodec.codecs.h264.encode;

import li.cil.oc2.jcodec.codecs.h264.decode.ChromaPredictionBuilder;
import li.cil.oc2.jcodec.codecs.h264.decode.CoeffTransformer;
import li.cil.oc2.jcodec.codecs.h264.decode.Intra16x16PredictionBuilder;
import li.cil.oc2.jcodec.codecs.h264.decode.Intra4x4PredictionBuilder;
import li.cil.oc2.jcodec.common.model.Picture;
import li.cil.oc2.jcodec.common.tools.MathUtil;

import java.util.Arrays;

import static li.cil.oc2.jcodec.codecs.h264.H264Const.BLK_DISP_MAP;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
public final class IntraPredEstimator {
    public static int[] getLumaPred4x4(final Picture pic, final EncodingContext ctx, final int mbX, final int mbY, final int qp) {
        final byte[] patch = new byte[256];
        MBEncoderHelper.take(pic.getPlaneData(0), pic.getPlaneWidth(0), pic.getPlaneHeight(0), mbX << 4, mbY << 4, patch, 16, 16);
        final int[] predModes = new int[16];
        final byte[] predLeft = Arrays.copyOf(ctx.leftRow[0], 16);
        final byte[] predTop = Arrays.copyOfRange(ctx.topLine[0], mbX << 4, (mbX << 4) + 16 + (mbX < ctx.mbWidth - 1 ? 4 : 0));
        final byte[] predTopLeft = new byte[]{ctx.topLeft[0], ctx.leftRow[0][3], ctx.leftRow[0][7], ctx.leftRow[0][11]};
        final int[] resi = new int[16];
        final byte[] pred = new byte[16];
        final int[] bresi = new int[16];
        final byte[] bpred = new byte[16];

        for (int bInd = 0; bInd < 16; bInd++) {
            int minSad = Integer.MAX_VALUE;

            final int dInd = BLK_DISP_MAP[bInd];
            final boolean hasLeft = (dInd & 0x3) != 0 || mbX != 0;
            final boolean hasTop = dInd >= 4 || mbY != 0;
            final boolean hasTr = ((bInd == 0 || bInd == 1 || bInd == 4) && mbY != 0) || (bInd == 5 && mbX < ctx.mbWidth - 1)
                || bInd == 2 || bInd == 6 || bInd == 8 || bInd == 9 || bInd == 10 || bInd == 12 || bInd == 14;
            predModes[bInd] = 2;
            final int blkX = (dInd & 0x3) << 2;
            final int blkY = (dInd >> 2) << 2;

            for (int predType = 0; predType < 9; predType++) {
                final boolean available = Intra4x4PredictionBuilder.lumaPred(predType, hasLeft, hasTop, hasTr, predLeft, predTop, predTopLeft[dInd >> 2], blkX, blkY, pred);

                if (available) {
                    int sad = 0;
                    for (int i = 0; i < 16; i++) {
                        final int x = blkX + (i & 0x3);
                        final int y = blkY + (i >> 2);
                        resi[i] = patch[(y << 4) + x] - pred[i];
                        sad += MathUtil.abs(resi[i]);
                    }

                    if (sad < minSad) {
                        minSad = sad;
                        predModes[bInd] = predType;

                        // Distort coeffs
                        CoeffTransformer.fdct4x4(resi);
                        CoeffTransformer.quantizeAC(resi, qp);
                        CoeffTransformer.dequantizeAC(resi, qp, null);
                        CoeffTransformer.idct4x4(resi);
                        System.arraycopy(pred, 0, bpred, 0, 16);
                        System.arraycopy(resi, 0, bresi, 0, 16);
                    }
                }
            }
            predTopLeft[dInd >> 2] = predTop[blkX + 3];
            for (int p = 0; p < 4; p++) {
                predLeft[blkY + p] = (byte) MathUtil.clip(bresi[3 + (p << 2)] + bpred[3 + (p << 2)], -128, 127);
                predTop[blkX + p] = (byte) MathUtil.clip(bresi[12 + p] + bpred[12 + p], -128, 127);
            }
        }
        return predModes;
    }

    public static int getLumaMode(final Picture pic, final EncodingContext ctx, final int mbX, final int mbY) {
        final byte[] patch = new byte[256];
        MBEncoderHelper.take(pic.getPlaneData(0), pic.getPlaneWidth(0), pic.getPlaneHeight(0), mbX << 4, mbY << 4, patch, 16, 16);
        int minSad = Integer.MAX_VALUE;
        int predMode = -1;
        for (int predType = 0; predType < 4; predType++) {
            final int sad = Intra16x16PredictionBuilder.lumaPredSAD(predType, mbX != 0, mbY != 0, ctx.leftRow[0], ctx.topLine[0], ctx.topLeft[0], mbX << 4, patch);
            if (sad < minSad) {
                minSad = sad;
                predMode = predType;
            }
        }
        return predMode;
    }

    public static int getChromaMode(final Picture pic, final EncodingContext ctx, final int mbX, final int mbY) {
        final byte[] patch0 = new byte[64];
        final byte[] patch1 = new byte[64];
        MBEncoderHelper.take(pic.getPlaneData(1), pic.getPlaneWidth(1), pic.getPlaneHeight(1), mbX << 3, mbY << 3, patch0, 8, 8);
        MBEncoderHelper.take(pic.getPlaneData(2), pic.getPlaneWidth(2), pic.getPlaneHeight(2), mbX << 3, mbY << 3, patch1, 8, 8);
        int minSad = Integer.MAX_VALUE;
        int predMode = -1;
        for (int predType = 0; predType < 4; predType++) {
            if (!ChromaPredictionBuilder.predAvb(predType, mbX != 0, mbY != 0))
                continue;
            final int sad0 = ChromaPredictionBuilder.predSAD(predType, mbX, mbX != 0, mbY != 0, ctx.leftRow[1], ctx.topLine[1], ctx.topLeft[1], patch0);
            final int sad1 = ChromaPredictionBuilder.predSAD(predType, mbX, mbX != 0, mbY != 0, ctx.leftRow[2], ctx.topLine[2], ctx.topLeft[2], patch1);
            if (sad0 + sad1 < minSad) {
                minSad = sad0 + sad1;
                predMode = predType;
            }
        }
        return predMode;
    }
}
