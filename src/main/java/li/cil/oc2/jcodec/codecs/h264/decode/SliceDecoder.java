/* SPDX-License-Identifier: BSD 2-Clause "Simplified" License */

package li.cil.oc2.jcodec.codecs.h264.decode;

import li.cil.oc2.jcodec.codecs.h264.H264Const;
import li.cil.oc2.jcodec.codecs.h264.decode.aso.MapManager;
import li.cil.oc2.jcodec.codecs.h264.decode.aso.Mapper;
import li.cil.oc2.jcodec.codecs.h264.io.model.*;
import li.cil.oc2.jcodec.common.IntObjectMap;
import li.cil.oc2.jcodec.common.model.Picture;

import static java.lang.System.arraycopy;
import static li.cil.oc2.jcodec.codecs.h264.H264Const.PartPred.L0;
import static li.cil.oc2.jcodec.codecs.h264.io.model.MBType.*;
import static li.cil.oc2.jcodec.codecs.h264.io.model.SliceType.P;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * <p>
 * A decoder for an individual slice
 *
 * @author The JCodec project
 */
public final class SliceDecoder {
    private Mapper mapper;

    private MBlockDecoderIntra16x16 decoderIntra16x16;
    private MBlockDecoderIntraNxN decoderIntraNxN;
    private MBlockDecoderInter decoderInter;
    private MBlockDecoderInter8x8 decoderInter8x8;
    private MBlockSkipDecoder skipDecoder;
    private MBlockDecoderBDirect decoderBDirect;
    private RefListManager refListManager;
    private MBlockDecoderIPCM decoderIPCM;
    private SliceReader parser;
    private final SeqParameterSet activeSps;
    private final Frame frameOut;
    private final DeblockerInput di;
    private final IntObjectMap<Frame> lRefs;
    private final Frame[] sRefs;

    public SliceDecoder(final SeqParameterSet activeSps, final Frame[] sRefs,
                        final IntObjectMap<Frame> lRefs, final DeblockerInput di, final Frame result) {
        this.di = di;
        this.activeSps = activeSps;
        this.frameOut = result;
        this.sRefs = sRefs;
        this.lRefs = lRefs;
    }

    public void decodeFromReader(final SliceReader sliceReader) {
        parser = sliceReader;

        initContext();

        final Frame[][] refList = refListManager.getRefList();

        decodeMacroblocks(refList);
    }

    private void initContext() {
        final SliceHeader sh = parser.getSliceHeader();

        final DecoderState decoderState = new DecoderState(sh);
        mapper = new MapManager(sh.sps, sh.pps).getMapper(sh);

        decoderIntra16x16 = new MBlockDecoderIntra16x16(mapper, sh, di, frameOut.getPOC(), decoderState);
        decoderIntraNxN = new MBlockDecoderIntraNxN(mapper, sh, di, frameOut.getPOC(), decoderState);
        decoderInter = new MBlockDecoderInter(mapper, sh, di, frameOut.getPOC(), decoderState);
        decoderBDirect = new MBlockDecoderBDirect(mapper, sh, di, frameOut.getPOC(), decoderState);
        decoderInter8x8 = new MBlockDecoderInter8x8(mapper, decoderBDirect, sh, di, frameOut.getPOC(), decoderState);
        skipDecoder = new MBlockSkipDecoder(mapper, decoderBDirect, sh, di, frameOut.getPOC(), decoderState);
        decoderIPCM = new MBlockDecoderIPCM(mapper, decoderState);

        refListManager = new RefListManager(sh, sRefs, lRefs, frameOut);
    }

    private void decodeMacroblocks(final Frame[][] refList) {
        final Picture mb = Picture.create(16, 16, activeSps.chromaFormatIdc);
        final int mbWidth = activeSps.picWidthInMbsMinus1 + 1;

        final MBlock mBlock = new MBlock(activeSps.chromaFormatIdc);
        while (parser.readMacroblock(mBlock)) {
            final int mbAddr = mapper.getAddress(mBlock.mbIdx);
            final int mbX = mbAddr % mbWidth;
            final int mbY = mbAddr / mbWidth;
            decode(mBlock, parser.getSliceHeader().sliceType, mb, refList);
            putMacroblock(frameOut, mb, mbX, mbY);
            di.shs[mbAddr] = parser.getSliceHeader();
            di.refsUsed[mbAddr] = refList;
            fillCoeff(mBlock, mbX, mbY);
            mb.fill(0);
            mBlock.clear();
        }
    }

    private void fillCoeff(final MBlock mBlock, final int mbX, final int mbY) {
        for (int i = 0; i < 16; i++) {
            final int blkOffLeft = H264Const.MB_DISP_OFF_LEFT[i];
            final int blkOffTop = H264Const.MB_DISP_OFF_TOP[i];
            final int blkX = (mbX << 2) + blkOffLeft;
            final int blkY = (mbY << 2) + blkOffTop;

            di.nCoeff[blkY][blkX] = mBlock.nCoeff[i];
        }
    }

    public void decode(final MBlock mBlock, final SliceType sliceType, final Picture mb, final Frame[][] references) {
        if (mBlock.skipped) {
            skipDecoder.decodeSkip(mBlock, references, mb, sliceType);
        } else if (sliceType == SliceType.I) {
            decodeMBlockI(mBlock, mb);
        } else if (sliceType == SliceType.P) {
            decodeMBlockP(mBlock, mb, references);
        } else {
            decodeMBlockB(mBlock, mb, references);
        }
    }

    private void decodeMBlockI(final MBlock mBlock, final Picture mb) {
        decodeMBlockIInt(mBlock, mb);
    }

    private void decodeMBlockIInt(final MBlock mBlock, final Picture mb) {
        if (mBlock.curMbType == MBType.I_NxN) {
            decoderIntraNxN.decode(mBlock, mb);
        } else if (mBlock.curMbType == MBType.I_16x16) {
            decoderIntra16x16.decode(mBlock, mb);
        } else {
            decoderIPCM.decode(mBlock, mb);
        }
    }

    private void decodeMBlockP(final MBlock mBlock, final Picture mb, final Frame[][] references) {
        if (P_16x16 == mBlock.curMbType) {
            decoderInter.decode16x16(mBlock, mb, references, L0);
        } else if (P_16x8 == mBlock.curMbType) {
            decoderInter.decode16x8(mBlock, mb, references, L0, L0);
        } else if (P_8x16 == mBlock.curMbType) {
            decoderInter.decode8x16(mBlock, mb, references, L0, L0);
        } else if (P_8x8 == mBlock.curMbType) {
            decoderInter8x8.decode(mBlock, references, mb, P);
        } else if (P_8x8ref0 == mBlock.curMbType) {
            decoderInter8x8.decode(mBlock, references, mb, P);
        } else {
            decodeMBlockIInt(mBlock, mb);
        }
    }

    private void decodeMBlockB(final MBlock mBlock, final Picture mb, final Frame[][] references) {
        if (mBlock.curMbType.isIntra()) {
            decodeMBlockIInt(mBlock, mb);
        } else {
            if (mBlock.curMbType == B_Direct_16x16) {
                decoderBDirect.decode(mBlock, mb, references);
            } else if (mBlock.mbType <= 3) {
                decoderInter.decode16x16(mBlock, mb, references, H264Const.bPredModes[mBlock.mbType][0]);
            } else if (mBlock.mbType == 22) {
                decoderInter8x8.decode(mBlock, references, mb, SliceType.B);
            } else if ((mBlock.mbType & 1) == 0) {
                decoderInter.decode16x8(mBlock, mb, references, H264Const.bPredModes[mBlock.mbType][0],
                    H264Const.bPredModes[mBlock.mbType][1]);
            } else {
                decoderInter.decode8x16(mBlock, mb, references, H264Const.bPredModes[mBlock.mbType][0],
                    H264Const.bPredModes[mBlock.mbType][1]);
            }
        }
    }

    private static void putMacroblock(final Picture tgt, final Picture decoded, final int mbX, final int mbY) {
        final byte[] luma = tgt.getPlaneData(0);
        final int stride = tgt.getPlaneWidth(0);

        final byte[] cb = tgt.getPlaneData(1);
        final byte[] cr = tgt.getPlaneData(2);
        final int strideChroma = tgt.getPlaneWidth(1);

        int dOff = 0;
        final int mbx16 = mbX * 16;
        final int mby16 = mbY * 16;
        final byte[] decodedY = decoded.getPlaneData(0);
        for (int i = 0; i < 16; i++) {
            arraycopy(decodedY, dOff, luma, (mby16 + i) * stride + mbx16, 16);
            dOff += 16;
        }

        final int mbx8 = mbX * 8;
        final int mby8 = mbY * 8;
        final byte[] decodedCb = decoded.getPlaneData(1);
        final byte[] decodedCr = decoded.getPlaneData(2);
        for (int i = 0; i < 8; i++) {
            final int decodePos = i << 3;
            final int chromaPos = (mby8 + i) * strideChroma + mbx8;
            arraycopy(decodedCb, decodePos, cb, chromaPos, 8);
            arraycopy(decodedCr, decodePos, cr, chromaPos, 8);
        }
    }
}
