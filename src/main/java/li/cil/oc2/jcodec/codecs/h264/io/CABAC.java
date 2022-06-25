/* SPDX-License-Identifier: BSD 2-Clause "Simplified" License */

package li.cil.oc2.jcodec.codecs.h264.io;

import li.cil.oc2.jcodec.codecs.common.biari.MDecoder;
import li.cil.oc2.jcodec.codecs.h264.H264Const;
import li.cil.oc2.jcodec.codecs.h264.H264Utils;
import li.cil.oc2.jcodec.codecs.h264.decode.CABACContst;
import li.cil.oc2.jcodec.codecs.h264.io.model.MBType;
import li.cil.oc2.jcodec.codecs.h264.io.model.SliceType;
import li.cil.oc2.jcodec.common.tools.MathUtil;

import static li.cil.oc2.jcodec.codecs.h264.io.CABAC.BlockType.*;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author JCodec project
 */
public final class CABAC {
    public final static class BlockType {
        public final static BlockType LUMA_16_DC = new BlockType(85, 105, 166, 277, 227, 0);
        public final static BlockType LUMA_15_AC = new BlockType(89, 120, 181, 292, 237, 0);
        public final static BlockType LUMA_16 = new BlockType(93, 134, 195, 306, 247, 0);
        public final static BlockType CHROMA_DC = new BlockType(97, 149, 210, 321, 257, 1);
        public final static BlockType CHROMA_AC = new BlockType(101, 152, 213, 324, 266, 0);
        public final static BlockType LUMA_64 = new BlockType(1012, 402, 417, 436, 426, 0);
        public final static BlockType CB_16_DC = new BlockType(460, 484, 572, 776, 952, 0);
        public final static BlockType CB_15x16_AC = new BlockType(464, 499, 587, 791, 962, 0);
        public final static BlockType CB_16 = new BlockType(468, 513, 601, 805, 972, 0);
        public final static BlockType CB_64 = new BlockType(1016, 660, 690, 675, 708, 0);
        public final static BlockType CR_16_DC = new BlockType(472, 528, 616, 820, 982, 0);
        public final static BlockType CR_15x16_AC = new BlockType(476, 543, 631, 835, 992, 0);
        public final static BlockType CR_16 = new BlockType(480, 557, 645, 849, 1002, 0);
        public final static BlockType CR_64 = new BlockType(1020, 718, 748, 733, 766, 0);

        public final int codedBlockCtxOff;
        public final int sigCoeffFlagCtxOff;
        public final int lastSigCoeffCtxOff;
        public final int sigCoeffFlagFldCtxOff;
        public final int lastSigCoeffFldCtxOff;
        public final int coeffAbsLevelCtxOff;
        public final int coeffAbsLevelAdjust;

        private BlockType(final int codecBlockCtxOff, final int sigCoeffCtxOff, final int lastSigCoeffCtxOff,
                          final int sigCoeffFlagFldCtxOff, final int coeffAbsLevelCtxOff, final int coeffAbsLevelAdjust) {
            this.codedBlockCtxOff = codecBlockCtxOff;
            this.sigCoeffFlagCtxOff = sigCoeffCtxOff;
            this.lastSigCoeffCtxOff = lastSigCoeffCtxOff;
            this.sigCoeffFlagFldCtxOff = sigCoeffFlagFldCtxOff;
            this.lastSigCoeffFldCtxOff = sigCoeffFlagFldCtxOff;
            this.coeffAbsLevelCtxOff = coeffAbsLevelCtxOff;
            this.coeffAbsLevelAdjust = coeffAbsLevelAdjust;
        }
    }

    private int chromaPredModeLeft;
    private final int[] chromaPredModeTop;
    private int prevMbQpDelta;
    private int prevCBP;

    private final int[][] codedBlkLeft;
    private final int[][] codedBlkTop;

    private final int[] codedBlkDCLeft;
    private final int[][] codedBlkDCTop;

    private final int[][] refIdxLeft;
    private final int[][] refIdxTop;

    private boolean skipFlagLeft;
    private final boolean[] skipFlagsTop;

    private final int[][][] mvdTop;
    private final int[][][] mvdLeft;

    public final int[] tmp;

    public CABAC(final int mbWidth) {
        this.tmp = new int[16];
        this.chromaPredModeLeft = 0;
        this.chromaPredModeTop = new int[mbWidth];
        this.codedBlkLeft = new int[][]{new int[4], new int[2], new int[2]};
        this.codedBlkTop = new int[][]{new int[mbWidth << 2], new int[mbWidth << 1], new int[mbWidth << 1]};

        this.codedBlkDCLeft = new int[3];
        this.codedBlkDCTop = new int[3][mbWidth];

        this.refIdxLeft = new int[2][4];
        this.refIdxTop = new int[2][mbWidth << 2];

        this.skipFlagsTop = new boolean[mbWidth];

        this.mvdTop = new int[2][2][mbWidth << 2];
        this.mvdLeft = new int[2][2][4];
    }

    public int readCoeffs(final MDecoder decoder, final BlockType blockType, final int[] out, final int first, final int num, final int[] reorder,
                          final int[] scMapping, final int[] lscMapping) {
        final boolean[] sigCoeff = new boolean[num];
        int numCoeff;
        for (numCoeff = 0; numCoeff < num - 1; numCoeff++) {
            sigCoeff[numCoeff] = decoder.decodeBin(blockType.sigCoeffFlagCtxOff + scMapping[numCoeff]) == 1;
            if (sigCoeff[numCoeff] && decoder.decodeBin(blockType.lastSigCoeffCtxOff + lscMapping[numCoeff]) == 1)
                break;
        }
        sigCoeff[numCoeff++] = true;

        int numGt1 = 0, numEq1 = 0;
        for (int j = numCoeff - 1; j >= 0; j--) {
            if (!sigCoeff[j])
                continue;
            final int absLev = readCoeffAbsLevel(decoder, blockType, numGt1, numEq1);
            if (absLev == 0)
                ++numEq1;
            else
                ++numGt1;
            out[reorder[j + first]] = MathUtil.toSigned(absLev + 1, -decoder.decodeBinBypass());
        }

        return numGt1 + numEq1;
    }

    private int readCoeffAbsLevel(final MDecoder decoder, final BlockType blockType, final int numDecodAbsLevelGt1,
                                  final int numDecodAbsLevelEq1) {
        final int incB0 = ((numDecodAbsLevelGt1 != 0) ? 0 : Math.min(4, 1 + numDecodAbsLevelEq1));
        final int incBN = 5 + Math.min(4 - blockType.coeffAbsLevelAdjust, numDecodAbsLevelGt1);

        int val, b = decoder.decodeBin(blockType.coeffAbsLevelCtxOff + incB0);
        for (val = 0; b != 0 && val < 13; val++)
            b = decoder.decodeBin(blockType.coeffAbsLevelCtxOff + incBN);
        val += b;

        if (val == 14) {
            int log = -2, add = 0, sum = 0;
            do {
                log++;
                b = decoder.decodeBinBypass();
            } while (b != 0);

            for (; log >= 0; log--) {
                add |= decoder.decodeBinBypass() << log;
                sum += 1 << log;
            }

            val += add + sum;
        }

        return val;
    }

    public void initModels(final int[][] cm, final SliceType sliceType, final int cabacIdc, final int sliceQp) {
        final int[] tabA = sliceType.isIntra() ? CABACContst.cabac_context_init_I_A
            : CABACContst.cabac_context_init_PB_A[cabacIdc];
        final int[] tabB = sliceType.isIntra() ? CABACContst.cabac_context_init_I_B
            : CABACContst.cabac_context_init_PB_B[cabacIdc];

        for (int i = 0; i < 1024; i++) {
            final int preCtxState = MathUtil.clip(((tabA[i] * MathUtil.clip(sliceQp, 0, 51)) >> 4) + tabB[i], 1, 126);
            if (preCtxState <= 63) {
                cm[0][i] = 63 - preCtxState;
                cm[1][i] = 0;
            } else {
                cm[0][i] = preCtxState - 64;
                cm[1][i] = 1;
            }
        }
    }

    public int readMBTypeI(final MDecoder decoder, final MBType left, final MBType top, final boolean leftAvailable, final boolean topAvailable) {
        int ctx = 3;
        ctx += !leftAvailable || left == MBType.I_NxN ? 0 : 1;
        ctx += !topAvailable || top == MBType.I_NxN ? 0 : 1;

        if (decoder.decodeBin(ctx) == 0) {
            return 0;
        } else {
            return decoder.decodeFinalBin() == 1 ? 25 : 1 + readMBType16x16(decoder);
        }
    }

    private int readMBType16x16(final MDecoder decoder) {
        final int type = decoder.decodeBin(6) * 12;
        if (decoder.decodeBin(7) == 0) {
            return type + (decoder.decodeBin(9) << 1) + decoder.decodeBin(10);
        } else {
            return type + (decoder.decodeBin(8) << 2) + (decoder.decodeBin(9) << 1) + decoder.decodeBin(10) + 4;
        }
    }

    public int readMBTypeP(final MDecoder decoder) {

        if (decoder.decodeBin(14) == 1) {
            return 5 + readIntraP(decoder, 17);
        } else {
            if (decoder.decodeBin(15) == 0) {
                return decoder.decodeBin(16) == 0 ? 0 : 3;
            } else {
                return decoder.decodeBin(17) == 0 ? 2 : 1;
            }
        }
    }

    private int readIntraP(final MDecoder decoder, final int ctxOff) {
        if (decoder.decodeBin(ctxOff) == 0) {
            return 0;
        } else {
            return decoder.decodeFinalBin() == 1 ? 25 : 1 + readMBType16x16P(decoder, ctxOff);
        }
    }

    private int readMBType16x16P(final MDecoder decoder, int ctxOff) {
        ctxOff++;
        final int type = decoder.decodeBin(ctxOff) * 12;
        ctxOff++;
        if (decoder.decodeBin(ctxOff) == 0) {
            ctxOff++;
            return type + (decoder.decodeBin(ctxOff) << 1) + decoder.decodeBin(ctxOff);
        } else {
            return type + (decoder.decodeBin(ctxOff) << 2) + (decoder.decodeBin(ctxOff + 1) << 1)
                + decoder.decodeBin(ctxOff + 1) + 4;
        }
    }

    public int readMBTypeB(final MDecoder mDecoder, final MBType left, final MBType top, final boolean leftAvailable, final boolean topAvailable) {
        int ctx = 27;
        ctx += !leftAvailable || left == null || left == MBType.B_Direct_16x16 ? 0 : 1;
        ctx += !topAvailable || top == null || top == MBType.B_Direct_16x16 ? 0 : 1;

        if (mDecoder.decodeBin(ctx) == 0)
            return 0; // B Direct
        if (mDecoder.decodeBin(30) == 0)
            return 1 + mDecoder.decodeBin(32);

        final int b1 = mDecoder.decodeBin(31);
        if (b1 == 0) {
            return 3 + ((mDecoder.decodeBin(32) << 2) | (mDecoder.decodeBin(32) << 1) | mDecoder.decodeBin(32));
        } else {
            if (mDecoder.decodeBin(32) == 0) {
                return 12 + ((mDecoder.decodeBin(32) << 2) | (mDecoder.decodeBin(32) << 1) | mDecoder.decodeBin(32));
            } else {
                switch ((mDecoder.decodeBin(32) << 1) + mDecoder.decodeBin(32)) {
                    case 0:
                        return 20 + mDecoder.decodeBin(32);
                    case 1:
                        return 23 + readIntraP(mDecoder, 32);
                    case 2:
                        return 11;
                    case 3:
                        return 22;
                }
            }
        }

        return 0;
    }

    public int readMBQpDelta(final MDecoder decoder, final MBType prevMbType) {
        int ctx = 60;
        ctx += prevMbType == null || prevMbType == MBType.I_PCM || (prevMbType != MBType.I_16x16 && prevCBP == 0)
            || prevMbQpDelta == 0 ? 0 : 1;

        int val = 0;
        if (decoder.decodeBin(ctx) == 1) {
            val++;
            if (decoder.decodeBin(62) == 1) {
                val++;
                while (decoder.decodeBin(63) == 1)
                    val++;
            }
        }
        prevMbQpDelta = H264Utils.golomb2Signed(val);

        return prevMbQpDelta;
    }

    public int readIntraChromaPredMode(final MDecoder decoder, final int mbX, final MBType left, final MBType top, final boolean leftAvailable,
                                       final boolean topAvailable) {
        int ctx = 64;
        ctx += !leftAvailable || left == null || !left.isIntra() || chromaPredModeLeft == 0 ? 0 : 1;
        ctx += !topAvailable || top == null || !top.isIntra() || chromaPredModeTop[mbX] == 0 ? 0 : 1;
        final int mode;
        if (decoder.decodeBin(ctx) == 0)
            mode = 0;
        else if (decoder.decodeBin(67) == 0)
            mode = 1;
        else if (decoder.decodeBin(67) == 0)
            mode = 2;
        else
            mode = 3;
        chromaPredModeLeft = chromaPredModeTop[mbX] = mode;

        return mode;
    }

    public int condTerm(final MBType mbCur, final boolean nAvb, final MBType mbN, final boolean nBlkAvb, final int cbpN) {
        if (!nAvb)
            return mbCur.isIntra() ? 1 : 0;
        if (mbN == MBType.I_PCM)
            return 1;
        if (!nBlkAvb)
            return 0;
        return cbpN;
    }

    public int readCodedBlockFlagLumaDC(final MDecoder decoder, final int mbX, final MBType left, final MBType top, final boolean leftAvailable,
                                        final boolean topAvailable, final MBType cur) {
        final int tLeft = condTerm(cur, leftAvailable, left, left == MBType.I_16x16, codedBlkDCLeft[0]);
        final int tTop = condTerm(cur, topAvailable, top, top == MBType.I_16x16, codedBlkDCTop[0][mbX]);

        final int decoded = decoder.decodeBin(LUMA_16_DC.codedBlockCtxOff + tLeft + 2 * tTop);

        codedBlkDCLeft[0] = decoded;
        codedBlkDCTop[0][mbX] = decoded;

        return decoded;
    }

    public int readCodedBlockFlagChromaDC(final MDecoder decoder, final int mbX, final int comp, final MBType left, final MBType top,
                                          final boolean leftAvailable, final boolean topAvailable, final int leftCBPChroma, final int topCBPChroma, final MBType cur) {
        final int tLeft = condTerm(cur, leftAvailable, left, left != null && leftCBPChroma != 0, codedBlkDCLeft[comp]);
        final int tTop = condTerm(cur, topAvailable, top, top != null && topCBPChroma != 0, codedBlkDCTop[comp][mbX]);

        final int decoded = decoder.decodeBin(CHROMA_DC.codedBlockCtxOff + tLeft + 2 * tTop);

        codedBlkDCLeft[comp] = decoded;
        codedBlkDCTop[comp][mbX] = decoded;

        return decoded;
    }

    public int readCodedBlockFlagLumaAC(final MDecoder decoder, final BlockType blkType, final int blkX, final int blkY, final int comp, final MBType left,
                                        final MBType top, final boolean leftAvailable, final boolean topAvailable, final int leftCBPLuma, final int topCBPLuma, final int curCBPLuma,
                                        final MBType cur) {
        final int blkOffLeft = blkX & 3;
        final int blkOffTop = blkY & 3;

        final int tLeft;
        if (blkOffLeft == 0)
            tLeft = condTerm(cur, leftAvailable, left, left != null && left != MBType.I_PCM && cbp(leftCBPLuma, 3, blkOffTop),
                codedBlkLeft[comp][blkOffTop]);
        else
            tLeft = condTerm(cur, true, cur, cbp(curCBPLuma, blkOffLeft - 1, blkOffTop), codedBlkLeft[comp][blkOffTop]);

        final int tTop;
        if (blkOffTop == 0)
            tTop = condTerm(cur, topAvailable, top, top != null && top != MBType.I_PCM && cbp(topCBPLuma, blkOffLeft, 3),
                codedBlkTop[comp][blkX]);
        else
            tTop = condTerm(cur, true, cur, cbp(curCBPLuma, blkOffLeft, blkOffTop - 1), codedBlkTop[comp][blkX]);

        final int decoded = decoder.decodeBin(blkType.codedBlockCtxOff + tLeft + 2 * tTop);

        codedBlkLeft[comp][blkOffTop] = decoded;
        codedBlkTop[comp][blkX] = decoded;

        return decoded;
    }

    private boolean cbp(final int cbpLuma, final int blkX, final int blkY) {
        final int x8x8 = (blkY & 2) + (blkX >> 1);

        return ((cbpLuma >> x8x8) & 1) == 1;
    }

    public int readCodedBlockFlagChromaAC(final MDecoder decoder, final int blkX, final int blkY, final int comp, final MBType left, final MBType top,
                                          final boolean leftAvailable, final boolean topAvailable, final int leftCBPChroma, final int topCBPChroma, final MBType cur) {
        final int blkOffLeft = blkX & 1;
        final int blkOffTop = blkY & 1;

        final int tLeft;
        if (blkOffLeft == 0)
            tLeft = condTerm(cur, leftAvailable, left, left != null && left != MBType.I_PCM && (leftCBPChroma & 2) != 0,
                codedBlkLeft[comp][blkOffTop]);
        else
            tLeft = condTerm(cur, true, cur, true, codedBlkLeft[comp][blkOffTop]);
        final int tTop;
        if (blkOffTop == 0)
            tTop = condTerm(cur, topAvailable, top, top != null && top != MBType.I_PCM && (topCBPChroma & 2) != 0,
                codedBlkTop[comp][blkX]);
        else
            tTop = condTerm(cur, true, cur, true, codedBlkTop[comp][blkX]);

        final int decoded = decoder.decodeBin(CHROMA_AC.codedBlockCtxOff + tLeft + 2 * tTop);

        codedBlkLeft[comp][blkOffTop] = decoded;
        codedBlkTop[comp][blkX] = decoded;

        return decoded;
    }

    public boolean prev4x4PredModeFlag(final MDecoder decoder) {
        return decoder.decodeBin(68) == 1;
    }

    public int rem4x4PredMode(final MDecoder decoder) {
        return decoder.decodeBin(69) | (decoder.decodeBin(69) << 1) | (decoder.decodeBin(69) << 2);
    }

    public int codedBlockPatternIntra(final MDecoder mDecoder, final boolean leftAvailable, final boolean topAvailable, final int cbpLeft,
                                      final int cbpTop, final MBType mbLeft, final MBType mbTop) {
        final int cbp0 = mDecoder.decodeBin(73 + _condTerm(leftAvailable, mbLeft, (cbpLeft >> 1) & 1) + 2
            * _condTerm(topAvailable, mbTop, (cbpTop >> 2) & 1));
        final int cbp1 = mDecoder.decodeBin(73 + (1 - cbp0) + 2 * _condTerm(topAvailable, mbTop, (cbpTop >> 3) & 1));
        final int cbp2 = mDecoder.decodeBin(73 + _condTerm(leftAvailable, mbLeft, (cbpLeft >> 3) & 1) + 2 * (1 - cbp0));
        final int cbp3 = mDecoder.decodeBin(73 + (1 - cbp2) + 2 * (1 - cbp1));

        final int cr0 = mDecoder.decodeBin(77 + condTermCr0(leftAvailable, mbLeft, cbpLeft >> 4) + 2
            * condTermCr0(topAvailable, mbTop, cbpTop >> 4));
        final int cr1 = cr0 != 0 ? mDecoder.decodeBin(81 + condTermCr1(leftAvailable, mbLeft, cbpLeft >> 4) + 2
            * condTermCr1(topAvailable, mbTop, cbpTop >> 4)) : 0;

        return cbp0 | (cbp1 << 1) | (cbp2 << 2) | (cbp3 << 3) | (cr0 << 4) | (cr1 << 5);
    }

    private int condTermCr0(final boolean avb, final MBType mbt, final int cbpChroma) {
        return avb && (mbt == MBType.I_PCM || mbt != null && cbpChroma != 0) ? 1 : 0;
    }

    private int condTermCr1(final boolean avb, final MBType mbt, final int cbpChroma) {
        return avb && (mbt == MBType.I_PCM || mbt != null && (cbpChroma & 2) != 0) ? 1 : 0;
    }

    private int _condTerm(final boolean avb, final MBType mbt, final int cbp) {
        return !avb || mbt == MBType.I_PCM || (mbt != null && cbp == 1) ? 0 : 1;
    }

    public void setPrevCBP(final int prevCBP) {
        this.prevCBP = prevCBP;
    }

    public int readMVD(final MDecoder decoder, final int comp, final boolean leftAvailable, final boolean topAvailable, final MBType leftType,
                       final MBType topType, final H264Const.PartPred leftPred, final H264Const.PartPred topPred, final H264Const.PartPred curPred, final int mbX, final int partX, final int partY,
                       final int partW, final int partH, final int list) {
        final int ctx = comp == 0 ? 40 : 47;

        final int partAbsX = (mbX << 2) + partX;

        final boolean predEqA = leftPred != null && leftPred != H264Const.PartPred.Direct
            && (leftPred == H264Const.PartPred.Bi || leftPred == curPred || (curPred == H264Const.PartPred.Bi && H264Const.usesList(leftPred, list)));
        final boolean predEqB = topPred != null && topPred != H264Const.PartPred.Direct
            && (topPred == H264Const.PartPred.Bi || topPred == curPred || (curPred == H264Const.PartPred.Bi && H264Const.usesList(topPred, list)));

        // prefix and suffix as given by UEG3 with signedValFlag=1, uCoff=9
        int absMvdComp = !leftAvailable || leftType == null || leftType.isIntra() || !predEqA ? 0 : Math
            .abs(mvdLeft[list][comp][partY]);
        absMvdComp += !topAvailable || topType == null || topType.isIntra() || !predEqB ? 0 : Math
            .abs(mvdTop[list][comp][partAbsX]);

        int val, b = decoder.decodeBin(ctx + (absMvdComp < 3 ? 0 : (absMvdComp > 32 ? 2 : 1)));
        for (val = 0; b != 0 && val < 8; val++)
            b = decoder.decodeBin(Math.min(ctx + val + 3, ctx + 6));
        val += b;

        if (val != 0) {
            if (val == 9) {
                int log = 2, add = 0, sum = 0, leftover = 0;
                do {
                    sum += leftover;
                    log++;
                    b = decoder.decodeBinBypass();
                    leftover = 1 << log;
                } while (b != 0);

                --log;

                for (; log >= 0; log--) {
                    add |= decoder.decodeBinBypass() << log;
                }
                val += add + sum;
            }

            val = MathUtil.toSigned(val, -decoder.decodeBinBypass());
        }

        for (int i = 0; i < partW; i++) {
            mvdTop[list][comp][partAbsX + i] = val;
        }
        for (int i = 0; i < partH; i++) {
            mvdLeft[list][comp][partY + i] = val;
        }

        return val;
    }

    public int readRefIdx(final MDecoder mDecoder, final boolean leftAvailable, final boolean topAvailable, final MBType leftType,
                          final MBType topType, final H264Const.PartPred leftPred, final H264Const.PartPred topPred, final H264Const.PartPred curPred, final int mbX, final int partX, final int partY,
                          final int partW, final int partH, final int list) {
        final int partAbsX = (mbX << 2) + partX;

        final boolean predEqA = leftPred != null && leftPred != H264Const.PartPred.Direct
            && (leftPred == H264Const.PartPred.Bi || leftPred == curPred || (curPred == H264Const.PartPred.Bi && H264Const.usesList(leftPred, list)));
        final boolean predEqB = topPred != null && topPred != H264Const.PartPred.Direct
            && (topPred == H264Const.PartPred.Bi || topPred == curPred || (curPred == H264Const.PartPred.Bi && H264Const.usesList(topPred, list)));

        final int ctA = !leftAvailable || leftType == null || leftType.isIntra() || !predEqA || refIdxLeft[list][partY] == 0 ? 0
            : 1;
        final int ctB = !topAvailable || topType == null || topType.isIntra() || !predEqB || refIdxTop[list][partAbsX] == 0 ? 0
            : 1;
        final int b0 = mDecoder.decodeBin(54 + ctA + 2 * ctB);
        int val;
        if (b0 == 0)
            val = 0;
        else {
            final int b1 = mDecoder.decodeBin(58);
            if (b1 == 0)
                val = 1;
            else {
                for (val = 2; mDecoder.decodeBin(59) == 1; val++) { }
            }
        }

        for (int i = 0; i < partW; i++) {
            refIdxTop[list][partAbsX + i] = val;
        }

        for (int i = 0; i < partH; i++) {
            refIdxLeft[list][partY + i] = val;
        }

        return val;
    }

    public boolean readMBSkipFlag(final MDecoder mDecoder, final SliceType slType, final boolean leftAvailable, final boolean topAvailable,
                                  final int mbX) {
        final int base = slType == SliceType.P ? 11 : 24;

        final boolean ret = mDecoder.decodeBin(base + (leftAvailable && !skipFlagLeft ? 1 : 0)
            + (topAvailable && !skipFlagsTop[mbX] ? 1 : 0)) == 1;

        skipFlagLeft = skipFlagsTop[mbX] = ret;

        return ret;
    }

    public int readSubMbTypeP(final MDecoder mDecoder) {
        if (mDecoder.decodeBin(21) == 1)
            return 0;
        else if (mDecoder.decodeBin(22) == 0)
            return 1;
        else if (mDecoder.decodeBin(23) == 1)
            return 2;
        else
            return 3;
    }

    public int readSubMbTypeB(final MDecoder mDecoder) {
        if (mDecoder.decodeBin(36) == 0)
            return 0; // direct
        if (mDecoder.decodeBin(37) == 0)
            return 1 + mDecoder.decodeBin(39);
        if (mDecoder.decodeBin(38) == 0)
            return 3 + (mDecoder.decodeBin(39) << 1) + mDecoder.decodeBin(39);

        if (mDecoder.decodeBin(39) == 0)
            return 7 + (mDecoder.decodeBin(39) << 1) + mDecoder.decodeBin(39);

        return 11 + mDecoder.decodeBin(39);
    }

    public boolean readTransform8x8Flag(final MDecoder mDecoder, final boolean leftAvailable, final boolean topAvailable,
                                        final MBType leftType, final MBType topType, final boolean is8x8Left, final boolean is8x8Top) {
        final int ctx = 399 + (leftAvailable && leftType != null && is8x8Left ? 1 : 0)
            + (topAvailable && topType != null && is8x8Top ? 1 : 0);
        return mDecoder.decodeBin(ctx) == 1;
    }

    public void setCodedBlock(final int blkX, final int blkY) {
        codedBlkLeft[0][blkY & 0x3] = codedBlkTop[0][blkX] = 1;
    }
}
