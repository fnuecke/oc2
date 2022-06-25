/* SPDX-License-Identifier: BSD 2-Clause "Simplified" License */

package li.cil.oc2.jcodec.codecs.h264.decode;

import li.cil.oc2.jcodec.codecs.common.biari.MDecoder;
import li.cil.oc2.jcodec.codecs.h264.H264Const;
import li.cil.oc2.jcodec.codecs.h264.decode.aso.Mapper;
import li.cil.oc2.jcodec.codecs.h264.io.CABAC;
import li.cil.oc2.jcodec.codecs.h264.io.CABAC.BlockType;
import li.cil.oc2.jcodec.codecs.h264.io.CAVLC;
import li.cil.oc2.jcodec.codecs.h264.io.model.*;
import li.cil.oc2.jcodec.common.io.BitReader;
import li.cil.oc2.jcodec.common.model.ColorSpace;

import static li.cil.oc2.jcodec.codecs.h264.H264Const.*;
import static li.cil.oc2.jcodec.codecs.h264.H264Const.PartPred.Direct;
import static li.cil.oc2.jcodec.codecs.h264.H264Const.PartPred.L0;
import static li.cil.oc2.jcodec.codecs.h264.io.model.MBType.*;

/**
 * Contains methods for reading high-level symbols out of H.264 bitstream
 *
 * @author The JCodec Project
 */
public final class SliceReader {
    private final PictureParameterSet activePps;
    private final CABAC cabac;
    private final MDecoder mDecoder;
    private final CAVLC[] cavlc;
    private final BitReader reader;
    private final Mapper mapper;
    private final SliceHeader sh;
    private final NALUnit nalUnit;

    private boolean prevMbSkipped = false;
    private int mbIdx;
    private MBType prevMBType = null;
    private int mbSkipRun;
    private boolean endOfData;

    // State
    final MBType[] topMBType;
    MBType leftMBType;
    int leftCBPLuma;
    final int[] topCBPLuma;
    int leftCBPChroma;
    final int[] topCBPChroma;
    final ColorSpace chromaFormat;
    final boolean transform8x8;
    final int[] numRef;
    boolean tf8x8Left;
    final boolean[] tf8x8Top;
    final int[] i4x4PredTop;
    final int[] i4x4PredLeft;
    final PartPred[] predModeLeft;
    final PartPred[] predModeTop;

    public SliceReader(final PictureParameterSet activePps, final CABAC cabac, final CAVLC[] cavlc, final MDecoder mDecoder, final BitReader reader,
                       final Mapper mapper, final SliceHeader sh, final NALUnit nalUnit) {
        this.activePps = activePps;
        this.cabac = cabac;
        this.mDecoder = mDecoder;
        this.cavlc = cavlc;
        this.reader = reader;
        this.mapper = mapper;
        this.sh = sh;
        this.nalUnit = nalUnit;

        final int mbWidth = sh.sps.picWidthInMbsMinus1 + 1;
        topMBType = new MBType[mbWidth];
        topCBPLuma = new int[mbWidth];
        topCBPChroma = new int[mbWidth];
        chromaFormat = sh.sps.chromaFormatIdc;
        transform8x8 = sh.pps.extended != null && sh.pps.extended.transform8x8ModeFlag;
        if (sh.numRefIdxActiveOverrideFlag)
            numRef = new int[]{sh.numRefIdxActiveMinus1[0] + 1, sh.numRefIdxActiveMinus1[1] + 1};
        else
            numRef = new int[]{sh.pps.numRefIdxActiveMinus1[0] + 1, sh.pps.numRefIdxActiveMinus1[1] + 1};

        tf8x8Top = new boolean[mbWidth];
        predModeLeft = new PartPred[2];
        predModeTop = new PartPred[mbWidth << 1];

        i4x4PredLeft = new int[4];
        i4x4PredTop = new int[mbWidth << 2];
    }

    public boolean readMacroblock(final MBlock mBlock) {
        final int mbWidth = sh.sps.picWidthInMbsMinus1 + 1;
        final int mbHeight = sh.sps.picHeightInMapUnitsMinus1 + 1;
        if (endOfData && mbSkipRun == 0 || mbIdx >= mbWidth * mbHeight)
            return false;

        mBlock.mbIdx = mbIdx;
        mBlock.prevMbType = prevMBType;

        final boolean mbaffFrameFlag = (sh.sps.mbAdaptiveFrameFieldFlag && !sh.fieldPicFlag);

        if (sh.sliceType.isInter() && !activePps.entropyCodingModeFlag) {
            if (!prevMbSkipped && mbSkipRun == 0) {
                mbSkipRun = CAVLCReader.readUE(reader); // mb_skip_run
                if (!CAVLCReader.moreRBSPData(reader)) {
                    endOfData = true;
                }
            }

            if (mbSkipRun > 0) {
                --mbSkipRun;
                prevMbSkipped = true;
                prevMBType = null;
                mBlock.skipped = true;
                final int mbX = mapper.getMbX(mBlock.mbIdx);
                topMBType[mbX] = leftMBType = null;
                final int blk8x8X = mbX << 1;
                predModeLeft[0] = predModeLeft[1] = predModeTop[blk8x8X] = predModeTop[blk8x8X + 1] = L0;
                ++mbIdx;
                return true;
            } else {
                prevMbSkipped = false;
            }
        }

        final int mbAddr = mapper.getAddress(mbIdx);
        final int mbX = mbAddr % mbWidth;

        if (sh.sliceType.isIntra()
            || (!activePps.entropyCodingModeFlag || !readMBSkipFlag(sh.sliceType, mapper.leftAvailable(mbIdx),
            mapper.topAvailable(mbIdx), mbX))) {

            boolean mb_field_decoding_flag = false;
            if (mbaffFrameFlag && (mbIdx % 2 == 0 || (mbIdx % 2 == 1 && prevMbSkipped))) {
                mb_field_decoding_flag = CAVLCReader.readBool(reader); // mb_field_decoding_flag
            }

            mBlock.fieldDecoding = mb_field_decoding_flag;
            readMBlock(mBlock, sh.sliceType);

            prevMBType = mBlock.curMbType;

        } else {
            prevMBType = null;
            prevMbSkipped = true;
            mBlock.skipped = true;
            final int blk8x8X = mbX << 1;
            predModeLeft[0] = predModeLeft[1] = predModeTop[blk8x8X] = predModeTop[blk8x8X + 1] = L0;
        }

        endOfData = (activePps.entropyCodingModeFlag && mDecoder.decodeFinalBin() == 1)
            || (!activePps.entropyCodingModeFlag && !CAVLCReader.moreRBSPData(reader));

        ++mbIdx;

        topMBType[mapper.getMbX(mBlock.mbIdx)] = leftMBType = mBlock.curMbType;

        return true;
    }

    int readMBQpDelta(final MBType prevMbType) {
        final int mbQPDelta;
        if (!activePps.entropyCodingModeFlag) {
            mbQPDelta = CAVLCReader.readSE(reader); // mb_qp_delta
        } else {
            mbQPDelta = cabac.readMBQpDelta(mDecoder, prevMbType);
        }
        return mbQPDelta;
    }

    int readChromaPredMode(final int mbX, final boolean leftAvailable, final boolean topAvailable) {
        final int chromaPredictionMode;
        if (!activePps.entropyCodingModeFlag) {
            chromaPredictionMode = CAVLCReader.readUE(reader); // MBP: intra_chroma_pred_mode
        } else {
            chromaPredictionMode = cabac.readIntraChromaPredMode(mDecoder, mbX, leftMBType, topMBType[mbX],
                leftAvailable, topAvailable);
        }
        return chromaPredictionMode;
    }

    boolean readTransform8x8Flag(final boolean leftAvailable, final boolean topAvailable, final MBType leftType, final MBType topType,
                                 final boolean is8x8Left, final boolean is8x8Top) {
        if (!activePps.entropyCodingModeFlag)
            return CAVLCReader.readBool(reader); // transform_size_8x8_flag
        else
            return cabac.readTransform8x8Flag(mDecoder, leftAvailable, topAvailable, leftType, topType, is8x8Left,
                is8x8Top);
    }

    private int readCodedBlockPatternIntra(final boolean leftAvailable, final boolean topAvailable, final int leftCBP, final int topCBP,
                                           final MBType leftMB, final MBType topMB) {

        if (!activePps.entropyCodingModeFlag)
            return H264Const.CODED_BLOCK_PATTERN_INTRA_COLOR[CAVLCReader.readUE(reader)]; // coded_block_pattern
        else
            return cabac.codedBlockPatternIntra(mDecoder, leftAvailable, topAvailable, leftCBP, topCBP, leftMB, topMB);
    }

    private int readCodedBlockPatternInter(final boolean leftAvailable, final boolean topAvailable, final int leftCBP, final int topCBP,
                                           final MBType leftMB, final MBType topMB) {
        if (!activePps.entropyCodingModeFlag) {
            final int code = CAVLCReader.readUE(reader); // coded_block_pattern
            return H264Const.CODED_BLOCK_PATTERN_INTER_COLOR[code];
        } else
            return cabac.codedBlockPatternIntra(mDecoder, leftAvailable, topAvailable, leftCBP, topCBP, leftMB, topMB);
    }

    int readRefIdx(final boolean leftAvailable, final boolean topAvailable, final MBType leftType, final MBType topType, final PartPred leftPred,
                   final PartPred topPred, final PartPred curPred, final int mbX, final int partX, final int partY, final int partW, final int partH, final int list) {
        if (!activePps.entropyCodingModeFlag)
            return CAVLCReader.readTE(reader, numRef[list] - 1);
        else
            return cabac.readRefIdx(mDecoder, leftAvailable, topAvailable, leftType, topType, leftPred, topPred,
                curPred, mbX, partX, partY, partW, partH, list);
    }

    int readMVD(final int comp, final boolean leftAvailable, final boolean topAvailable, final MBType leftType, final MBType topType,
                final PartPred leftPred, final PartPred topPred, final PartPred curPred, final int mbX, final int partX, final int partY, final int partW, final int partH,
                final int list) {
        if (!activePps.entropyCodingModeFlag)
            return CAVLCReader.readSE(reader); // mvd_l0_x
        else
            return cabac.readMVD(mDecoder, comp, leftAvailable, topAvailable, leftType, topType, leftPred, topPred,
                curPred, mbX, partX, partY, partW, partH, list);
    }

    int readPredictionI4x4Block(final boolean leftAvailable, final boolean topAvailable, final MBType leftMBType, final MBType topMBType,
                                final int blkX, final int blkY, final int mbX) {
        int mode = 2;
        if ((leftAvailable || blkX > 0) && (topAvailable || blkY > 0)) {
            final int predModeB = topMBType == MBType.I_NxN || blkY > 0 ? i4x4PredTop[(mbX << 2) + blkX] : 2;
            final int predModeA = leftMBType == MBType.I_NxN || blkX > 0 ? i4x4PredLeft[blkY] : 2;
            mode = Math.min(predModeB, predModeA);
        }
        if (!prev4x4PredMode()) {
            final int rem_intra4x4_pred_mode = rem4x4PredMode();
            mode = rem_intra4x4_pred_mode + (rem_intra4x4_pred_mode < mode ? 0 : 1);
        }
        i4x4PredTop[(mbX << 2) + blkX] = i4x4PredLeft[blkY] = mode;
        return mode;
    }

    int rem4x4PredMode() {
        if (!activePps.entropyCodingModeFlag)
            return CAVLCReader.readNBit(reader, 3); // MB: rem_intra4x4_pred_mode
        else
            return cabac.rem4x4PredMode(mDecoder);
    }

    boolean prev4x4PredMode() {
        if (!activePps.entropyCodingModeFlag)
            return CAVLCReader.readBool(reader); // MBP: prev_intra4x4_pred_mode_flag
        else
            return cabac.prev4x4PredModeFlag(mDecoder);
    }

    void read16x16DC(final boolean leftAvailable, final boolean topAvailable, final int mbX, final int[] dc) {
        if (!activePps.entropyCodingModeFlag)
            cavlc[0].readLumaDCBlock(reader, dc, mbX, leftAvailable, leftMBType, topAvailable, topMBType[mbX],
                CoeffTransformer.zigzag4x4);
        else {
            if (cabac.readCodedBlockFlagLumaDC(mDecoder, mbX, leftMBType, topMBType[mbX], leftAvailable, topAvailable,
                MBType.I_16x16) == 1)
                cabac.readCoeffs(mDecoder, BlockType.LUMA_16_DC, dc, 0, 16, CoeffTransformer.zigzag4x4,
                    identityMapping16, identityMapping16);
        }
    }

    /**
     * Reads AC block of macroblock encoded as I_16x16, returns number of
     * non-zero coefficients
     */
    int read16x16AC(final boolean leftAvailable, final boolean topAvailable, final int mbX, final int cbpLuma, final int[] ac, final int blkOffLeft,
                    final int blkOffTop, final int blkX) {
        if (!activePps.entropyCodingModeFlag) {
            return cavlc[0].readACBlock(reader, ac, blkX, blkOffTop, blkOffLeft != 0 || leftAvailable,
                blkOffLeft == 0 ? leftMBType : I_16x16, blkOffTop != 0 || topAvailable,
                blkOffTop == 0 ? topMBType[mbX] : I_16x16, 1, 15, CoeffTransformer.zigzag4x4);
        } else {
            if (cabac.readCodedBlockFlagLumaAC(mDecoder, BlockType.LUMA_15_AC, blkX, blkOffTop, 0, leftMBType,
                topMBType[mbX], leftAvailable, topAvailable, leftCBPLuma, topCBPLuma[mbX], cbpLuma, MBType.I_16x16) == 1)
                return cabac.readCoeffs(mDecoder, BlockType.LUMA_15_AC, ac, 1, 15, CoeffTransformer.zigzag4x4,
                    identityMapping16, identityMapping16);
        }
        return 0;
    }

    /**
     * Reads AC block of a macroblock, return number of non-zero coefficients
     */
    int readResidualAC(final boolean leftAvailable, final boolean topAvailable, final int mbX, final MBType curMbType, final int cbpLuma,
                       final int blkOffLeft, final int blkOffTop, final int blkX, final int[] ac) {
        if (!activePps.entropyCodingModeFlag) {
            if (reader.remaining() <= 0)
                return 0;
            return cavlc[0].readACBlock(reader, ac, blkX, blkOffTop, blkOffLeft != 0 || leftAvailable,
                blkOffLeft == 0 ? leftMBType : curMbType, blkOffTop != 0 || topAvailable,
                blkOffTop == 0 ? topMBType[mbX] : curMbType, 0, 16, CoeffTransformer.zigzag4x4);
        } else {
            if (cabac.readCodedBlockFlagLumaAC(mDecoder, BlockType.LUMA_16, blkX, blkOffTop, 0, leftMBType,
                topMBType[mbX], leftAvailable, topAvailable, leftCBPLuma, topCBPLuma[mbX], cbpLuma, curMbType) == 1)
                return cabac.readCoeffs(mDecoder, BlockType.LUMA_16, ac, 0, 16, CoeffTransformer.zigzag4x4,
                    identityMapping16, identityMapping16);
        }
        return 0;
    }

    public void setZeroCoeff(final int comp, final int blkX, final int blkOffTop) {
        cavlc[comp].setZeroCoeff(blkX, blkOffTop);
    }

    public void savePrevCBP(final int codedBlockPattern) {
        if (activePps.entropyCodingModeFlag)
            cabac.setPrevCBP(codedBlockPattern);
    }

    public int readLumaAC(final boolean leftAvailable, final boolean topAvailable, final int mbX, final MBType curMbType, final int blkX, final int j,
                          final int[] ac16, final int blkOffLeft, final int blkOffTop) {
        return cavlc[0].readACBlock(reader, ac16, blkX + (j & 1), blkOffTop, blkOffLeft != 0 || leftAvailable,
            blkOffLeft == 0 ? leftMBType : curMbType, blkOffTop != 0 || topAvailable,
            blkOffTop == 0 ? topMBType[mbX] : curMbType, 0, 16, H264Const.identityMapping16);
    }

    /**
     * Reads luma AC coeffiecients for 8x8 blocks, returns number of non-zero
     * coefficients
     */
    public int readLumaAC8x8(final int blkX, final int blkY, final int[] ac) {
        final int readCoeffs = cabac.readCoeffs(mDecoder, BlockType.LUMA_64, ac, 0, 64, CoeffTransformer.zigzag8x8,
            sig_coeff_map_8x8, last_sig_coeff_map_8x8);
        cabac.setCodedBlock(blkX, blkY);
        cabac.setCodedBlock(blkX + 1, blkY);
        cabac.setCodedBlock(blkX, blkY + 1);
        cabac.setCodedBlock(blkX + 1, blkY + 1);

        return readCoeffs;
    }

    public int readSubMBTypeP() {
        if (!activePps.entropyCodingModeFlag)
            return CAVLCReader.readUE(reader);
        else
            return cabac.readSubMbTypeP(mDecoder);
    }

    public int readSubMBTypeB() {
        if (!activePps.entropyCodingModeFlag)
            return CAVLCReader.readUE(reader);
        else
            return cabac.readSubMbTypeB(mDecoder);
    }

    public void readChromaDC(final int mbX, final boolean leftAvailable, final boolean topAvailable, final int[] dc, final int comp, final MBType curMbType) {
        if (!activePps.entropyCodingModeFlag)
            cavlc[comp].readChromaDCBlock(reader, dc);
        else {
            if (cabac.readCodedBlockFlagChromaDC(mDecoder, mbX, comp, leftMBType, topMBType[mbX], leftAvailable,
                topAvailable, leftCBPChroma, topCBPChroma[mbX], curMbType) == 1)
                cabac.readCoeffs(mDecoder, BlockType.CHROMA_DC, dc, 0, 4, identityMapping16, identityMapping16,
                    identityMapping16);
        }
    }

    public void readChromaAC(final boolean leftAvailable, final boolean topAvailable, final int mbX, final int comp, final MBType curMbType,
                             final int[] ac, final int blkOffLeft, final int blkOffTop, final int blkX) {
        if (!activePps.entropyCodingModeFlag) {
            if (reader.remaining() <= 0)
                return;
            cavlc[comp].readACBlock(reader, ac, blkX, blkOffTop, blkOffLeft != 0 || leftAvailable,
                blkOffLeft == 0 ? leftMBType : curMbType, blkOffTop != 0 || topAvailable,
                blkOffTop == 0 ? topMBType[mbX] : curMbType, 1, 15, CoeffTransformer.zigzag4x4);
        } else {
            if (cabac.readCodedBlockFlagChromaAC(mDecoder, blkX, blkOffTop, comp, leftMBType, topMBType[mbX],
                leftAvailable, topAvailable, leftCBPChroma, topCBPChroma[mbX], curMbType) == 1)
                cabac.readCoeffs(mDecoder, BlockType.CHROMA_AC, ac, 1, 15, CoeffTransformer.zigzag4x4,
                    identityMapping16, identityMapping16);
        }
    }

    public int decodeMBTypeI(final boolean leftAvailable, final boolean topAvailable, final MBType leftMBType, final MBType topMBType) {
        final int mbType;
        if (!activePps.entropyCodingModeFlag)
            mbType = CAVLCReader.readUE(reader); // MB: mb_type
        else
            mbType = cabac.readMBTypeI(mDecoder, leftMBType, topMBType, leftAvailable, topAvailable);
        return mbType;
    }

    public int readMBTypeP() {
        final int mbType;
        if (!activePps.entropyCodingModeFlag)
            mbType = CAVLCReader.readUE(reader); // MB: mb_type
        else
            mbType = cabac.readMBTypeP(mDecoder);
        return mbType;
    }

    public int readMBTypeB(final boolean leftAvailable, final boolean topAvailable, final MBType leftMBType, final MBType topMBType) {
        final int mbType;
        if (!activePps.entropyCodingModeFlag)
            mbType = CAVLCReader.readUE(reader); // MB: mb_type
        else
            mbType = cabac.readMBTypeB(mDecoder, leftMBType, topMBType, leftAvailable, topAvailable);
        return mbType;
    }

    public boolean readMBSkipFlag(final SliceType slType, final boolean leftAvailable, final boolean topAvailable, final int mbX) {
        return cabac.readMBSkipFlag(mDecoder, slType, leftAvailable, topAvailable, mbX);
    }

    public void readIntra16x16(final int mbType, final MBlock mBlock) {
        final int mbX = mapper.getMbX(mBlock.mbIdx);
        final boolean leftAvailable = mapper.leftAvailable(mBlock.mbIdx);
        final boolean topAvailable = mapper.topAvailable(mBlock.mbIdx);
        mBlock.cbp((mbType / 12) * 15, (mbType / 4) % 3);
        mBlock.luma16x16Mode = mbType % 4;
        mBlock.chromaPredictionMode = readChromaPredMode(mbX, leftAvailable, topAvailable);
        mBlock.mbQPDelta = readMBQpDelta(mBlock.prevMbType);
        read16x16DC(leftAvailable, topAvailable, mbX, mBlock.dc);
        for (int i = 0; i < 16; i++) {
            final int blkOffLeft = H264Const.MB_DISP_OFF_LEFT[i];
            final int blkOffTop = H264Const.MB_DISP_OFF_TOP[i];
            final int blkX = (mbX << 2) + blkOffLeft;

            if ((mBlock.cbpLuma() & (1 << (i >> 2))) != 0) {
                mBlock.nCoeff[i] = read16x16AC(leftAvailable, topAvailable, mbX, mBlock.cbpLuma(), mBlock.ac[0][i],
                    blkOffLeft, blkOffTop, blkX);
            } else {
                if (!sh.pps.entropyCodingModeFlag)
                    setZeroCoeff(0, blkX, blkOffTop);
            }
        }

        if (chromaFormat != ColorSpace.MONO) {
            readChromaResidual(mBlock, leftAvailable, topAvailable, mbX);
        }
    }

    public void readMBlockBDirect(final MBlock mBlock) {
        final int mbX = mapper.getMbX(mBlock.mbIdx);
        final int mbY = mapper.getMbY(mBlock.mbIdx);
        final boolean lAvb = mapper.leftAvailable(mBlock.mbIdx);
        final boolean tAvb = mapper.topAvailable(mBlock.mbIdx);
        mBlock._cbp = readCodedBlockPatternInter(lAvb, tAvb, leftCBPLuma | (leftCBPChroma << 4), topCBPLuma[mbX]
            | (topCBPChroma[mbX] << 4), leftMBType, topMBType[mbX]);

        mBlock.transform8x8Used = false;
        if (transform8x8 && mBlock.cbpLuma() != 0 && sh.sps.direct8x8InferenceFlag) {
            mBlock.transform8x8Used = readTransform8x8Flag(lAvb, tAvb, leftMBType, topMBType[mbX], tf8x8Left,
                tf8x8Top[mbX]);
        }

        if (mBlock.cbpLuma() > 0 || mBlock.cbpChroma() > 0) {
            mBlock.mbQPDelta = readMBQpDelta(mBlock.prevMbType);
        }
        readResidualLuma(mBlock, lAvb, tAvb, mbX, mbY);
        readChromaResidual(mBlock, lAvb, tAvb, mbX);

        predModeTop[mbX << 1] = predModeTop[(mbX << 1) + 1] = predModeLeft[0] = predModeLeft[1] = Direct;
    }

    public void readInter16x16(final PartPred p0, final MBlock mBlock) {
        final int mbX = mapper.getMbX(mBlock.mbIdx);
        final int mbY = mapper.getMbY(mBlock.mbIdx);
        final boolean leftAvailable = mapper.leftAvailable(mBlock.mbIdx);
        final boolean topAvailable = mapper.topAvailable(mBlock.mbIdx);

        for (int list = 0; list < 2; list++) {
            if (H264Const.usesList(p0, list) && numRef[list] > 1)
                mBlock.pb16x16.refIdx[list] = readRefIdx(leftAvailable, topAvailable, leftMBType, topMBType[mbX],
                    predModeLeft[0], predModeTop[(mbX << 1)], p0, mbX, 0, 0, 4, 4, list);
        }
        for (int list = 0; list < 2; list++) {
            readPredictionInter16x16(mBlock, mbX, leftAvailable, topAvailable, list, p0);
        }
        readResidualInter(mBlock, leftAvailable, topAvailable, mbX, mbY);

        predModeLeft[0] = predModeLeft[1] = predModeTop[mbX << 1] = predModeTop[(mbX << 1) + 1] = p0;
    }

    private void readPredInter8x16(final MBlock mBlock, final int mbX, final boolean leftAvailable, final boolean topAvailable, final int list,
                                   final PartPred p0, final PartPred p1) {
        final int blk8x8X = (mbX << 1);

        if (H264Const.usesList(p0, list)) {
            mBlock.pb168x168.mvdX1[list] = readMVD(0, leftAvailable, topAvailable, leftMBType, topMBType[mbX],
                predModeLeft[0], predModeTop[blk8x8X], p0, mbX, 0, 0, 2, 4, list);
            mBlock.pb168x168.mvdY1[list] = readMVD(1, leftAvailable, topAvailable, leftMBType, topMBType[mbX],
                predModeLeft[0], predModeTop[blk8x8X], p0, mbX, 0, 0, 2, 4, list);

        }

        if (H264Const.usesList(p1, list)) {
            mBlock.pb168x168.mvdX2[list] = readMVD(0, true, topAvailable, MBType.P_8x16, topMBType[mbX], p0,
                predModeTop[blk8x8X + 1], p1, mbX, 2, 0, 2, 4, list);
            mBlock.pb168x168.mvdY2[list] = readMVD(1, true, topAvailable, MBType.P_8x16, topMBType[mbX], p0,
                predModeTop[blk8x8X + 1], p1, mbX, 2, 0, 2, 4, list);

        }
    }

    private void readPredictionInter16x8(final MBlock mBlock, final int mbX, final boolean leftAvailable, final boolean topAvailable,
                                         final PartPred p0, final PartPred p1, final int list) {
        final int blk8x8X = mbX << 1;
        if (H264Const.usesList(p0, list)) {

            mBlock.pb168x168.mvdX1[list] = readMVD(0, leftAvailable, topAvailable, leftMBType, topMBType[mbX],
                predModeLeft[0], predModeTop[blk8x8X], p0, mbX, 0, 0, 4, 2, list);
            mBlock.pb168x168.mvdY1[list] = readMVD(1, leftAvailable, topAvailable, leftMBType, topMBType[mbX],
                predModeLeft[0], predModeTop[blk8x8X], p0, mbX, 0, 0, 4, 2, list);
        }

        if (H264Const.usesList(p1, list)) {
            mBlock.pb168x168.mvdX2[list] = readMVD(0, leftAvailable, true, leftMBType, MBType.P_16x8, predModeLeft[1],
                p0, p1, mbX, 0, 2, 4, 2, list);
            mBlock.pb168x168.mvdY2[list] = readMVD(1, leftAvailable, true, leftMBType, MBType.P_16x8, predModeLeft[1],
                p0, p1, mbX, 0, 2, 4, 2, list);
        }
    }

    public void readInter16x8(final PartPred p0, final PartPred p1, final MBlock mBlock) {
        final int mbX = mapper.getMbX(mBlock.mbIdx);
        final int mbY = mapper.getMbY(mBlock.mbIdx);
        final boolean leftAvailable = mapper.leftAvailable(mBlock.mbIdx);
        final boolean topAvailable = mapper.topAvailable(mBlock.mbIdx);

        for (int list = 0; list < 2; list++) {
            if (H264Const.usesList(p0, list) && numRef[list] > 1)
                mBlock.pb168x168.refIdx1[list] = readRefIdx(leftAvailable, topAvailable, leftMBType, topMBType[mbX],
                    predModeLeft[0], predModeTop[(mbX << 1)], p0, mbX, 0, 0, 4, 2, list);
            if (H264Const.usesList(p1, list) && numRef[list] > 1)
                mBlock.pb168x168.refIdx2[list] = readRefIdx(leftAvailable, true, leftMBType, mBlock.curMbType,
                    predModeLeft[1], p0, p1, mbX, 0, 2, 4, 2, list);
        }

        for (int list = 0; list < 2; list++) {
            readPredictionInter16x8(mBlock, mbX, leftAvailable, topAvailable, p0, p1, list);
        }
        readResidualInter(mBlock, leftAvailable, topAvailable, mbX, mbY);

        predModeLeft[0] = p0;
        predModeLeft[1] = predModeTop[mbX << 1] = predModeTop[(mbX << 1) + 1] = p1;
    }

    public void readIntra8x16(final PartPred p0, final PartPred p1, final MBlock mBlock) {
        final int mbX = mapper.getMbX(mBlock.mbIdx);
        final int mbY = mapper.getMbY(mBlock.mbIdx);
        final boolean leftAvailable = mapper.leftAvailable(mBlock.mbIdx);
        final boolean topAvailable = mapper.topAvailable(mBlock.mbIdx);
        for (int list = 0; list < 2; list++) {
            if (H264Const.usesList(p0, list) && numRef[list] > 1)
                mBlock.pb168x168.refIdx1[list] = readRefIdx(leftAvailable, topAvailable, leftMBType, topMBType[mbX],
                    predModeLeft[0], predModeTop[mbX << 1], p0, mbX, 0, 0, 2, 4, list);
            if (H264Const.usesList(p1, list) && numRef[list] > 1)
                mBlock.pb168x168.refIdx2[list] = readRefIdx(true, topAvailable, mBlock.curMbType, topMBType[mbX], p0,
                    predModeTop[(mbX << 1) + 1], p1, mbX, 2, 0, 2, 4, list);
        }

        for (int list = 0; list < 2; list++) {
            readPredInter8x16(mBlock, mbX, leftAvailable, topAvailable, list, p0, p1);
        }
        readResidualInter(mBlock, leftAvailable, topAvailable, mbX, mbY);
        predModeTop[mbX << 1] = p0;
        predModeTop[(mbX << 1) + 1] = predModeLeft[0] = predModeLeft[1] = p1;
    }

    private void readPredictionInter16x16(final MBlock mBlock, final int mbX, final boolean leftAvailable, final boolean topAvailable,
                                          final int list, final PartPred curPred) {
        final int blk8x8X = (mbX << 1);

        if (H264Const.usesList(curPred, list)) {
            mBlock.pb16x16.mvdX[list] = readMVD(0, leftAvailable, topAvailable, leftMBType, topMBType[mbX],
                predModeLeft[0], predModeTop[blk8x8X], curPred, mbX, 0, 0, 4, 4, list);
            mBlock.pb16x16.mvdY[list] = readMVD(1, leftAvailable, topAvailable, leftMBType, topMBType[mbX],
                predModeLeft[0], predModeTop[blk8x8X], curPred, mbX, 0, 0, 4, 4, list);
        }
    }

    private void readResidualInter(final MBlock mBlock, final boolean leftAvailable, final boolean topAvailable, final int mbX, final int mbY) {
        mBlock._cbp = readCodedBlockPatternInter(leftAvailable, topAvailable, leftCBPLuma | (leftCBPChroma << 4),
            topCBPLuma[mbX] | (topCBPChroma[mbX] << 4), leftMBType, topMBType[mbX]);

        mBlock.transform8x8Used = false;
        if (mBlock.cbpLuma() != 0 && transform8x8) {
            mBlock.transform8x8Used = readTransform8x8Flag(leftAvailable, topAvailable, leftMBType, topMBType[mbX],
                tf8x8Left, tf8x8Top[mbX]);
        }

        if (mBlock.cbpLuma() > 0 || mBlock.cbpChroma() > 0) {
            mBlock.mbQPDelta = readMBQpDelta(mBlock.prevMbType);
        }

        readResidualLuma(mBlock, leftAvailable, topAvailable, mbX, mbY);

        if (chromaFormat != ColorSpace.MONO) {
            readChromaResidual(mBlock, leftAvailable, topAvailable, mbX);
        }
    }

    public void readMBlock8x8(final MBlock mBlock) {
        final int mbX = mapper.getMbX(mBlock.mbIdx);
        final int mbY = mapper.getMbY(mBlock.mbIdx);
        final boolean leftAvailable = mapper.leftAvailable(mBlock.mbIdx);
        final boolean topAvailable = mapper.topAvailable(mBlock.mbIdx);

        final boolean noSubMBLessThen8x8;
        if (mBlock.curMbType == P_8x8 || mBlock.curMbType == MBType.P_8x8ref0) {
            readPrediction8x8P(mBlock, mbX, leftAvailable, topAvailable);

            noSubMBLessThen8x8 = mBlock.pb8x8.subMbTypes[0] == 0 && mBlock.pb8x8.subMbTypes[1] == 0
                && mBlock.pb8x8.subMbTypes[2] == 0 && mBlock.pb8x8.subMbTypes[3] == 0;
        } else {
            readPrediction8x8B(mBlock, mbX, leftAvailable, topAvailable);
            noSubMBLessThen8x8 = bSubMbTypes[mBlock.pb8x8.subMbTypes[0]] == 0
                && bSubMbTypes[mBlock.pb8x8.subMbTypes[1]] == 0 && bSubMbTypes[mBlock.pb8x8.subMbTypes[2]] == 0
                && bSubMbTypes[mBlock.pb8x8.subMbTypes[3]] == 0;
        }

        mBlock._cbp = readCodedBlockPatternInter(leftAvailable, topAvailable, leftCBPLuma | (leftCBPChroma << 4),
            topCBPLuma[mbX] | (topCBPChroma[mbX] << 4), leftMBType, topMBType[mbX]);

        mBlock.transform8x8Used = false;
        if (transform8x8 && mBlock.cbpLuma() != 0 && noSubMBLessThen8x8) {
            mBlock.transform8x8Used = readTransform8x8Flag(leftAvailable, topAvailable, leftMBType, topMBType[mbX],
                tf8x8Left, tf8x8Top[mbX]);
        }

        if (mBlock.cbpLuma() > 0 || mBlock.cbpChroma() > 0) {
            mBlock.mbQPDelta = readMBQpDelta(mBlock.prevMbType);
        }
        readResidualLuma(mBlock, leftAvailable, topAvailable, mbX, mbY);
        readChromaResidual(mBlock, leftAvailable, topAvailable, mbX);
    }

    private void readPrediction8x8P(final MBlock mBlock, final int mbX, final boolean leftAvailable, final boolean topAvailable) {
        for (int i = 0; i < 4; i++) {
            mBlock.pb8x8.subMbTypes[i] = readSubMBTypeP();
        }

        if (numRef[0] > 1 && mBlock.curMbType != MBType.P_8x8ref0) {
            mBlock.pb8x8.refIdx[0][0] = readRefIdx(leftAvailable, topAvailable, leftMBType, topMBType[mbX], L0, L0, L0,
                mbX, 0, 0, 2, 2, 0);
            mBlock.pb8x8.refIdx[0][1] = readRefIdx(true, topAvailable, P_8x8, topMBType[mbX], L0, L0, L0, mbX, 2, 0, 2,
                2, 0);
            mBlock.pb8x8.refIdx[0][2] = readRefIdx(leftAvailable, true, leftMBType, P_8x8, L0, L0, L0, mbX, 0, 2, 2, 2,
                0);
            mBlock.pb8x8.refIdx[0][3] = readRefIdx(true, true, P_8x8, P_8x8, L0, L0, L0, mbX, 2, 2, 2, 2, 0);
        }

        readSubMb8x8(mBlock, 0, mBlock.pb8x8.subMbTypes[0], topAvailable, leftAvailable, 0, 0, mbX, leftMBType,
            topMBType[mbX], P_8x8, L0, L0, L0, 0);
        readSubMb8x8(mBlock, 1, mBlock.pb8x8.subMbTypes[1], topAvailable, true, 2, 0, mbX, P_8x8, topMBType[mbX],
            P_8x8, L0, L0, L0, 0);
        readSubMb8x8(mBlock, 2, mBlock.pb8x8.subMbTypes[2], true, leftAvailable, 0, 2, mbX, leftMBType, P_8x8, P_8x8,
            L0, L0, L0, 0);
        readSubMb8x8(mBlock, 3, mBlock.pb8x8.subMbTypes[3], true, true, 2, 2, mbX, P_8x8, P_8x8, P_8x8, L0, L0, L0, 0);

        final int blk8x8X = mbX << 1;

        predModeLeft[0] = predModeLeft[1] = predModeTop[blk8x8X] = predModeTop[blk8x8X + 1] = L0;
    }

    private void readPrediction8x8B(final MBlock mBlock, final int mbX, final boolean leftAvailable, final boolean topAvailable) {
        final PartPred[] p = new PartPred[4];
        for (int i = 0; i < 4; i++) {
            mBlock.pb8x8.subMbTypes[i] = readSubMBTypeB();
            p[i] = bPartPredModes[mBlock.pb8x8.subMbTypes[i]];
        }

        for (int list = 0; list < 2; list++) {
            if (numRef[list] <= 1)
                continue;
            if (H264Const.usesList(p[0], list))
                mBlock.pb8x8.refIdx[list][0] = readRefIdx(leftAvailable, topAvailable, leftMBType, topMBType[mbX],
                    predModeLeft[0], predModeTop[mbX << 1], p[0], mbX, 0, 0, 2, 2, list);
            if (H264Const.usesList(p[1], list))
                mBlock.pb8x8.refIdx[list][1] = readRefIdx(true, topAvailable, B_8x8, topMBType[mbX], p[0],
                    predModeTop[(mbX << 1) + 1], p[1], mbX, 2, 0, 2, 2, list);
            if (H264Const.usesList(p[2], list))
                mBlock.pb8x8.refIdx[list][2] = readRefIdx(leftAvailable, true, leftMBType, B_8x8, predModeLeft[1],
                    p[0], p[2], mbX, 0, 2, 2, 2, list);
            if (H264Const.usesList(p[3], list))
                mBlock.pb8x8.refIdx[list][3] = readRefIdx(true, true, B_8x8, B_8x8, p[2], p[1], p[3], mbX, 2, 2, 2, 2,
                    list);
        }

        final int blk8x8X = mbX << 1;
        for (int list = 0; list < 2; list++) {
            if (H264Const.usesList(p[0], list)) {
                readSubMb8x8(mBlock, 0, bSubMbTypes[mBlock.pb8x8.subMbTypes[0]], topAvailable, leftAvailable, 0, 0,
                    mbX, leftMBType, topMBType[mbX], B_8x8, predModeLeft[0], predModeTop[blk8x8X], p[0], list);
            }
            if (H264Const.usesList(p[1], list)) {
                readSubMb8x8(mBlock, 1, bSubMbTypes[mBlock.pb8x8.subMbTypes[1]], topAvailable, true, 2, 0, mbX, B_8x8,
                    topMBType[mbX], B_8x8, p[0], predModeTop[blk8x8X + 1], p[1], list);
            }

            if (H264Const.usesList(p[2], list)) {
                readSubMb8x8(mBlock, 2, bSubMbTypes[mBlock.pb8x8.subMbTypes[2]], true, leftAvailable, 0, 2, mbX,
                    leftMBType, B_8x8, B_8x8, predModeLeft[1], p[0], p[2], list);
            }

            if (H264Const.usesList(p[3], list)) {
                readSubMb8x8(mBlock, 3, bSubMbTypes[mBlock.pb8x8.subMbTypes[3]], true, true, 2, 2, mbX, B_8x8, B_8x8,
                    B_8x8, p[2], p[1], p[3], list);
            }
        }

        predModeLeft[0] = p[1];
        predModeTop[blk8x8X] = p[2];
        predModeLeft[1] = predModeTop[blk8x8X + 1] = p[3];
    }

    private void readSubMb8x8(final MBlock mBlock, final int partNo, final int subMbType, final boolean tAvb, final boolean lAvb, final int blk8x8X,
                              final int blk8x8Y, final int mbX, final MBType leftMBType, final MBType topMBType, final MBType curMBType, final PartPred leftPred,
                              final PartPred topPred, final PartPred partPred, final int list) {
        switch (subMbType) {
            case 3 -> readSub4x4(mBlock, partNo, tAvb, lAvb, blk8x8X, blk8x8Y, mbX, leftMBType, topMBType, curMBType, leftPred, topPred, partPred, list);
            case 2 -> readSub4x8(mBlock, partNo, tAvb, lAvb, blk8x8X, blk8x8Y, mbX, leftMBType, topMBType, curMBType, leftPred, topPred, partPred, list);
            case 1 -> readSub8x4(mBlock, partNo, tAvb, lAvb, blk8x8X, blk8x8Y, mbX, leftMBType, topMBType, curMBType, leftPred, topPred, partPred, list);
            case 0 -> readSub8x8(mBlock, partNo, tAvb, lAvb, blk8x8X, blk8x8Y, mbX, leftMBType, topMBType, leftPred, topPred, partPred, list);
        }
    }

    private void readSub8x8(final MBlock mBlock, final int partNo, final boolean tAvb, final boolean lAvb, final int blk8x8X, final int blk8x8Y, final int mbX,
                            final MBType leftMBType, final MBType topMBType, final PartPred leftPred, final PartPred topPred, final PartPred partPred, final int list) {
        mBlock.pb8x8.mvdX1[list][partNo] = readMVD(0, lAvb, tAvb, leftMBType, topMBType, leftPred, topPred, partPred,
            mbX, blk8x8X, blk8x8Y, 2, 2, list);
        mBlock.pb8x8.mvdY1[list][partNo] = readMVD(1, lAvb, tAvb, leftMBType, topMBType, leftPred, topPred, partPred,
            mbX, blk8x8X, blk8x8Y, 2, 2, list);
    }

    private void readSub8x4(final MBlock mBlock, final int partNo, final boolean tAvb, final boolean lAvb, final int blk8x8X, final int blk8x8Y, final int mbX,
                            final MBType leftMBType, final MBType topMBType, final MBType curMBType, final PartPred leftPred, final PartPred topPred,
                            final PartPred partPred, final int list) {
        mBlock.pb8x8.mvdX1[list][partNo] = readMVD(0, lAvb, tAvb, leftMBType, topMBType, leftPred, topPred, partPred,
            mbX, blk8x8X, blk8x8Y, 2, 1, list);
        mBlock.pb8x8.mvdY1[list][partNo] = readMVD(1, lAvb, tAvb, leftMBType, topMBType, leftPred, topPred, partPred,
            mbX, blk8x8X, blk8x8Y, 2, 1, list);

        mBlock.pb8x8.mvdX2[list][partNo] = readMVD(0, lAvb, true, leftMBType, curMBType, leftPred, partPred, partPred,
            mbX, blk8x8X, blk8x8Y + 1, 2, 1, list);
        mBlock.pb8x8.mvdY2[list][partNo] = readMVD(1, lAvb, true, leftMBType, curMBType, leftPred, partPred, partPred,
            mbX, blk8x8X, blk8x8Y + 1, 2, 1, list);
    }

    private void readSub4x8(final MBlock mBlock, final int partNo, final boolean tAvb, final boolean lAvb, final int blk8x8X, final int blk8x8Y, final int mbX,
                            final MBType leftMBType, final MBType topMBType, final MBType curMBType, final PartPred leftPred, final PartPred topPred,
                            final PartPred partPred, final int list) {
        mBlock.pb8x8.mvdX1[list][partNo] = readMVD(0, lAvb, tAvb, leftMBType, topMBType, leftPred, topPred, partPred,
            mbX, blk8x8X, blk8x8Y, 1, 2, list);
        mBlock.pb8x8.mvdY1[list][partNo] = readMVD(1, lAvb, tAvb, leftMBType, topMBType, leftPred, topPred, partPred,
            mbX, blk8x8X, blk8x8Y, 1, 2, list);
        mBlock.pb8x8.mvdX2[list][partNo] = readMVD(0, true, tAvb, curMBType, topMBType, partPred, topPred, partPred,
            mbX, blk8x8X + 1, blk8x8Y, 1, 2, list);
        mBlock.pb8x8.mvdY2[list][partNo] = readMVD(1, true, tAvb, curMBType, topMBType, partPred, topPred, partPred,
            mbX, blk8x8X + 1, blk8x8Y, 1, 2, list);
    }

    private void readSub4x4(final MBlock mBlock, final int partNo, final boolean tAvb, final boolean lAvb, final int blk8x8X, final int blk8x8Y, final int mbX,
                            final MBType leftMBType, final MBType topMBType, final MBType curMBType, final PartPred leftPred, final PartPred topPred,
                            final PartPred partPred, final int list) {
        mBlock.pb8x8.mvdX1[list][partNo] = readMVD(0, lAvb, tAvb, leftMBType, topMBType, leftPred, topPred, partPred,
            mbX, blk8x8X, blk8x8Y, 1, 1, list);
        mBlock.pb8x8.mvdY1[list][partNo] = readMVD(1, lAvb, tAvb, leftMBType, topMBType, leftPred, topPred, partPred,
            mbX, blk8x8X, blk8x8Y, 1, 1, list);
        mBlock.pb8x8.mvdX2[list][partNo] = readMVD(0, true, tAvb, curMBType, topMBType, partPred, topPred, partPred,
            mbX, blk8x8X + 1, blk8x8Y, 1, 1, list);
        mBlock.pb8x8.mvdY2[list][partNo] = readMVD(1, true, tAvb, curMBType, topMBType, partPred, topPred, partPred,
            mbX, blk8x8X + 1, blk8x8Y, 1, 1, list);
        mBlock.pb8x8.mvdX3[list][partNo] = readMVD(0, lAvb, true, leftMBType, curMBType, leftPred, partPred, partPred,
            mbX, blk8x8X, blk8x8Y + 1, 1, 1, list);
        mBlock.pb8x8.mvdY3[list][partNo] = readMVD(1, lAvb, true, leftMBType, curMBType, leftPred, partPred, partPred,
            mbX, blk8x8X, blk8x8Y + 1, 1, 1, list);
        mBlock.pb8x8.mvdX4[list][partNo] = readMVD(0, true, true, curMBType, curMBType, partPred, partPred, partPred,
            mbX, blk8x8X + 1, blk8x8Y + 1, 1, 1, list);
        mBlock.pb8x8.mvdY4[list][partNo] = readMVD(1, true, true, curMBType, curMBType, partPred, partPred, partPred,
            mbX, blk8x8X + 1, blk8x8Y + 1, 1, 1, list);
    }

    public void readIntraNxN(final MBlock mBlock) {
        final int mbX = mapper.getMbX(mBlock.mbIdx);
        final int mbY = mapper.getMbY(mBlock.mbIdx);
        final boolean leftAvailable = mapper.leftAvailable(mBlock.mbIdx);
        final boolean topAvailable = mapper.topAvailable(mBlock.mbIdx);

        mBlock.transform8x8Used = false;
        if (transform8x8) {
            mBlock.transform8x8Used = readTransform8x8Flag(leftAvailable, topAvailable, leftMBType, topMBType[mbX],
                tf8x8Left, tf8x8Top[mbX]);
        }

        if (!mBlock.transform8x8Used) {
            for (int bInd = 0; bInd < 16; bInd++) {
                final int blkX = H264Const.MB_DISP_OFF_LEFT[bInd];
                final int blkY = H264Const.MB_DISP_OFF_TOP[bInd];
                mBlock.lumaModes[bInd] = readPredictionI4x4Block(leftAvailable, topAvailable, leftMBType, topMBType[mbX],
                    blkX, blkY, mbX);
            }
        } else {
            for (int i = 0; i < 4; i++) {
                final int blkX = (i & 1) << 1;
                final int blkY = i & 2;
                mBlock.lumaModes[i] = readPredictionI4x4Block(leftAvailable, topAvailable, leftMBType, topMBType[mbX],
                    blkX, blkY, mbX);
                i4x4PredLeft[blkY + 1] = i4x4PredLeft[blkY];
                i4x4PredTop[(mbX << 2) + blkX + 1] = i4x4PredTop[(mbX << 2) + blkX];
            }
        }
        mBlock.chromaPredictionMode = readChromaPredMode(mbX, leftAvailable, topAvailable);

        mBlock._cbp = readCodedBlockPatternIntra(leftAvailable, topAvailable, leftCBPLuma | (leftCBPChroma << 4),
            topCBPLuma[mbX] | (topCBPChroma[mbX] << 4), leftMBType, topMBType[mbX]);

        if (mBlock.cbpLuma() > 0 || mBlock.cbpChroma() > 0) {
            mBlock.mbQPDelta = readMBQpDelta(mBlock.prevMbType);
        }
        readResidualLuma(mBlock, leftAvailable, topAvailable, mbX, mbY);
        if (chromaFormat != ColorSpace.MONO) {
            readChromaResidual(mBlock, leftAvailable, topAvailable, mbX);
        }
    }

    public void readResidualLuma(final MBlock mBlock, final boolean leftAvailable, final boolean topAvailable, final int mbX, final int mbY) {
        if (!mBlock.transform8x8Used) {
            readLuma(mBlock, leftAvailable, topAvailable, mbX);
        } else if (sh.pps.entropyCodingModeFlag) {
            readLuma8x8CABAC(mBlock, mbX, mbY);
        } else {
            readLuma8x8CAVLC(mBlock, leftAvailable, topAvailable, mbX);
        }
    }

    private void readLuma(final MBlock mBlock, final boolean leftAvailable, final boolean topAvailable, final int mbX) {
        for (int i = 0; i < 16; i++) {
            final int blkOffLeft = H264Const.MB_DISP_OFF_LEFT[i];
            final int blkOffTop = H264Const.MB_DISP_OFF_TOP[i];
            final int blkX = (mbX << 2) + blkOffLeft;

            if ((mBlock.cbpLuma() & (1 << (i >> 2))) == 0) {
                if (!sh.pps.entropyCodingModeFlag)
                    setZeroCoeff(0, blkX, blkOffTop);
                continue;
            }

            mBlock.nCoeff[i] = readResidualAC(leftAvailable, topAvailable, mbX, mBlock.curMbType, mBlock.cbpLuma(),
                blkOffLeft, blkOffTop, blkX, mBlock.ac[0][i]);
        }

        savePrevCBP(mBlock._cbp);
    }

    private void readLuma8x8CABAC(final MBlock mBlock, final int mbX, final int mbY) {
        for (int i = 0; i < 4; i++) {
            final int blkOffLeft = (i & 1) << 1;
            final int blkOffTop = i & 2;
            final int blkX = (mbX << 2) + blkOffLeft;
            final int blkY = (mbY << 2) + blkOffTop;

            if ((mBlock.cbpLuma() & (1 << i)) == 0) {
                continue;
            }

            final int nCoeff = readLumaAC8x8(blkX, blkY, mBlock.ac[0][i]);

            final int blk4x4Offset = i << 2;
            mBlock.nCoeff[blk4x4Offset] = mBlock.nCoeff[blk4x4Offset + 1] = mBlock.nCoeff[blk4x4Offset + 2] = mBlock.nCoeff[blk4x4Offset + 3] = nCoeff;
        }
        savePrevCBP(mBlock._cbp);
    }

    private void readLuma8x8CAVLC(final MBlock mBlock, final boolean leftAvailable, final boolean topAvailable, final int mbX) {
        for (int i = 0; i < 4; i++) {
            final int blk8x8OffLeft = (i & 1) << 1;
            final int blk8x8OffTop = i & 2;
            final int blkX = (mbX << 2) + blk8x8OffLeft;

            if ((mBlock.cbpLuma() & (1 << i)) == 0) {
                setZeroCoeff(0, blkX, blk8x8OffTop);
                setZeroCoeff(0, blkX + 1, blk8x8OffTop);
                setZeroCoeff(0, blkX, blk8x8OffTop + 1);
                setZeroCoeff(0, blkX + 1, blk8x8OffTop + 1);
                continue;
            }
            int coeffs = 0;
            for (int j = 0; j < 4; j++) {
                final int[] ac16 = new int[16];
                final int blkOffLeft = blk8x8OffLeft + (j & 1);
                final int blkOffTop = blk8x8OffTop + (j >> 1);
                coeffs += readLumaAC(leftAvailable, topAvailable, mbX, mBlock.curMbType, blkX, j, ac16, blkOffLeft,
                    blkOffTop);
                for (int k = 0; k < 16; k++)
                    mBlock.ac[0][i][CoeffTransformer.zigzag8x8[(k << 2) + j]] = ac16[k];
            }
            final int blk4x4Offset = i << 2;
            mBlock.nCoeff[blk4x4Offset] = mBlock.nCoeff[blk4x4Offset + 1] = mBlock.nCoeff[blk4x4Offset + 2] = mBlock.nCoeff[blk4x4Offset + 3] = coeffs;
        }
    }

    public void readChromaResidual(final MBlock mBlock, final boolean leftAvailable, final boolean topAvailable, final int mbX) {
        if (mBlock.cbpChroma() != 0) {
            if ((mBlock.cbpChroma() & 3) > 0) {
                readChromaDC(mbX, leftAvailable, topAvailable, mBlock.dc1, 1, mBlock.curMbType);
                readChromaDC(mbX, leftAvailable, topAvailable, mBlock.dc2, 2, mBlock.curMbType);
            }
            _readChromaAC(leftAvailable, topAvailable, mbX, mBlock.dc1, 1, mBlock.curMbType,
                (mBlock.cbpChroma() & 2) > 0, mBlock.ac[1]);
            _readChromaAC(leftAvailable, topAvailable, mbX, mBlock.dc2, 2, mBlock.curMbType,
                (mBlock.cbpChroma() & 2) > 0, mBlock.ac[2]);
        } else if (!sh.pps.entropyCodingModeFlag) {
            setZeroCoeff(1, mbX << 1, 0);
            setZeroCoeff(1, (mbX << 1) + 1, 1);
            setZeroCoeff(2, mbX << 1, 0);
            setZeroCoeff(2, (mbX << 1) + 1, 1);
        }
    }

    private void _readChromaAC(final boolean leftAvailable, final boolean topAvailable, final int mbX, final int[] dc, final int comp,
                               final MBType curMbType, final boolean codedAC, final int[][] residualOut) {
        for (int i = 0; i < dc.length; i++) {
            final int[] ac = residualOut[i];
            final int blkOffLeft = H264Const.MB_DISP_OFF_LEFT[i];
            final int blkOffTop = H264Const.MB_DISP_OFF_TOP[i];

            final int blkX = (mbX << 1) + blkOffLeft;

            if (codedAC) {
                readChromaAC(leftAvailable, topAvailable, mbX, comp, curMbType, ac, blkOffLeft, blkOffTop, blkX);
            } else {
                if (!sh.pps.entropyCodingModeFlag)
                    setZeroCoeff(comp, blkX, blkOffTop);
            }
        }
    }

    private void readIPCM(final MBlock mBlock) {
        reader.align();

        for (int i = 0; i < 256; i++) {
            mBlock.ipcm.samplesLuma[i] = reader.readNBit(8);
        }
        final int MbWidthC = 16 >> chromaFormat.compWidth[1];
        final int MbHeightC = 16 >> chromaFormat.compHeight[1];

        for (int i = 0; i < 2 * MbWidthC * MbHeightC; i++) {
            mBlock.ipcm.samplesChroma[i] = reader.readNBit(8);
        }
    }

    public void readMBlock(final MBlock mBlock, final SliceType sliceType) {

        if (sliceType == SliceType.I) {
            readMBlockI(mBlock);
        } else if (sliceType == SliceType.P) {
            readMBlockP(mBlock);
        } else {
            readMBlockB(mBlock);
        }
        final int mbX = mapper.getMbX(mBlock.mbIdx);
        topCBPLuma[mbX] = leftCBPLuma = mBlock.cbpLuma();
        topCBPChroma[mbX] = leftCBPChroma = mBlock.cbpChroma();
        tf8x8Left = tf8x8Top[mbX] = mBlock.transform8x8Used;
    }

    private void readMBlockI(final MBlock mBlock) {

        mBlock.mbType = decodeMBTypeI(mapper.leftAvailable(mBlock.mbIdx),
            mapper.topAvailable(mBlock.mbIdx), leftMBType, topMBType[mapper.getMbX(mBlock.mbIdx)]);
        readMBlockIInt(mBlock, mBlock.mbType);
    }

    private void readMBlockIInt(final MBlock mBlock, final int mbType) {
        if (mbType == 0) {
            mBlock.curMbType = MBType.I_NxN;
            readIntraNxN(mBlock);
        } else if (mbType >= 1 && mbType <= 24) {
            mBlock.curMbType = MBType.I_16x16;
            readIntra16x16(mbType - 1, mBlock);
        } else {
            mBlock.curMbType = MBType.I_PCM;
            readIPCM(mBlock);
        }
    }

    private void readMBlockP(final MBlock mBlock) {
        mBlock.mbType = readMBTypeP();

        switch (mBlock.mbType) {
            case 0 -> {
                mBlock.curMbType = MBType.P_16x16;
                readInter16x16(L0, mBlock);
            }
            case 1 -> {
                mBlock.curMbType = MBType.P_16x8;
                readInter16x8(L0, L0, mBlock);
            }
            case 2 -> {
                mBlock.curMbType = MBType.P_8x16;
                readIntra8x16(L0, L0, mBlock);
            }
            case 3 -> {
                mBlock.curMbType = MBType.P_8x8;
                readMBlock8x8(mBlock);
            }
            case 4 -> {
                mBlock.curMbType = MBType.P_8x8ref0;
                readMBlock8x8(mBlock);
            }
            default -> readMBlockIInt(mBlock, mBlock.mbType - 5);
        }
    }

    private void readMBlockB(final MBlock mBlock) {
        mBlock.mbType = readMBTypeB(mapper.leftAvailable(mBlock.mbIdx),
            mapper.topAvailable(mBlock.mbIdx), leftMBType, topMBType[mapper.getMbX(mBlock.mbIdx)]);
        if (mBlock.mbType >= 23) {
            readMBlockIInt(mBlock, mBlock.mbType - 23);
        } else {
            mBlock.curMbType = H264Const.bMbTypes[mBlock.mbType];

            if (mBlock.mbType == 0) {
                readMBlockBDirect(mBlock);
            } else if (mBlock.mbType <= 3) {
                readInter16x16(H264Const.bPredModes[mBlock.mbType][0], mBlock);
            } else if (mBlock.mbType == 22) {
                readMBlock8x8(mBlock);
            } else if ((mBlock.mbType & 1) == 0) {
                readInter16x8(H264Const.bPredModes[mBlock.mbType][0], H264Const.bPredModes[mBlock.mbType][1], mBlock);
            } else {
                readIntra8x16(H264Const.bPredModes[mBlock.mbType][0], H264Const.bPredModes[mBlock.mbType][1], mBlock);
            }
        }
    }

    public SliceHeader getSliceHeader() {
        return sh;
    }

    public NALUnit getNALUnit() {
        return nalUnit;
    }
}
