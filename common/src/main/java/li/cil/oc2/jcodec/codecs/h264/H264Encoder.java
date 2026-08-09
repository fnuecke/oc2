/* SPDX-License-Identifier: BSD 2-Clause "Simplified" License */

package li.cil.oc2.jcodec.codecs.h264;

import li.cil.oc2.jcodec.codecs.h264.encode.*;
import li.cil.oc2.jcodec.codecs.h264.io.CAVLC;
import li.cil.oc2.jcodec.codecs.h264.io.model.*;
import li.cil.oc2.jcodec.codecs.h264.io.write.CAVLCWriter;
import li.cil.oc2.jcodec.codecs.h264.io.write.SliceHeaderWriter;
import li.cil.oc2.jcodec.common.VideoEncoder;
import li.cil.oc2.jcodec.common.io.BitWriter;
import li.cil.oc2.jcodec.common.model.ColorSpace;
import li.cil.oc2.jcodec.common.model.Picture;
import li.cil.oc2.jcodec.common.model.Size;
import li.cil.oc2.jcodec.common.tools.MathUtil;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import static li.cil.oc2.jcodec.codecs.h264.H264Utils.escapeNAL;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * <p>
 * MPEG 4 AVC ( H.264 ) Encoder
 * <p>
 * Conforms to H.264 ( ISO/IEC 14496-10 ) specifications
 *
 * @author The JCodec project
 */
public final class H264Encoder extends VideoEncoder {
    private static final int KEY_INTERVAL_DEFAULT = 25;
    private static final int MOTION_SEARCH_RANGE_DEFAULT = 16;

    public static H264Encoder createH264Encoder() {
        return new H264Encoder(new CQPRateControl(24));
    }

    private final RateControl rc;
    private int frameNumber;
    private int keyInterval;
    private int motionSearchRange;

    private int maxPOC;

    private int maxFrameNumber;

    private SeqParameterSet sps;

    private PictureParameterSet pps;

    private MBWriterI16x16 mbEncoderI16x16;
    private MBWriterINxN mbEncoderINxN;
    private MBWriterP16x16 mbEncoderP16x16;

    private Picture ref;
    private Picture picOut;
    private EncodedMB[] topEncoded;

    private EncodingContext context;
    private boolean enableRdo;

    public H264Encoder(final RateControl rc) {
        this.rc = rc;
        this.keyInterval = KEY_INTERVAL_DEFAULT;
        this.motionSearchRange = MOTION_SEARCH_RANGE_DEFAULT;
    }

    public int getKeyInterval() {
        return keyInterval;
    }

    public void setKeyInterval(final int keyInterval) {
        this.keyInterval = keyInterval;
    }

    public int getMotionSearchRange() {
        return motionSearchRange;
    }

    public void setMotionSearchRange(final int motionSearchRange) {
        this.motionSearchRange = motionSearchRange;
    }

    public void setEnableRdo(final boolean enableRdo) {
        this.enableRdo = enableRdo;
    }

    /**
     * Encode this picture into h.264 frame. Frame type will be selected by encoder.
     */
    public EncodedFrame encodeFrame(final Picture pic, final ByteBuffer _out) {
        if (pic.getColor() != ColorSpace.YUV420J)
            throw new IllegalArgumentException("Input picture color is not supported: " + pic.getColor());

        if (frameNumber >= keyInterval) {
            frameNumber = 0;
        }

        final SliceType sliceType = frameNumber == 0 ? SliceType.I : SliceType.P;
        final boolean idr = frameNumber == 0;

        final ByteBuffer data = doEncodeFrame(pic, _out, idr, frameNumber++, sliceType);

        return new EncodedFrame(data, idr);
    }

    /**
     * Encode this picture as an IDR frame. IDR frame starts a new independently
     * decodeable video sequence
     */
    public ByteBuffer encodeIDRFrame(final Picture pic, final ByteBuffer _out) {
        frameNumber = 0;
        return doEncodeFrame(pic, _out, true, frameNumber, SliceType.I);
    }

    /**
     * Encode this picture as a P-frame. P-frame is an frame predicted from one or
     * more of the previously decoded frame and is usually 10x less in size then the
     * IDR frame.
     */
    public ByteBuffer encodePFrame(final Picture pic, final ByteBuffer _out) {
        frameNumber++;
        return doEncodeFrame(pic, _out, true, frameNumber, SliceType.P);
    }

    public ByteBuffer doEncodeFrame(final Picture pic, final ByteBuffer _out, final boolean idr, final int frameNumber, final SliceType frameType) {
        final ByteBuffer dup = _out.duplicate();
        int maxSize = Math.min(dup.remaining(), pic.getWidth() * pic.getHeight());
        maxSize -= (maxSize >>> 6); // 1.5% to account for escaping
        final int qp = rc.startPicture(pic.getSize(), maxSize, frameType);

        if (idr) {
            sps = initSPS(new Size(pic.getWidth(), pic.getHeight()));
            pps = initPPS();

            maxPOC = 1 << (sps.log2MaxPicOrderCntLsbMinus4 + 4);
            maxFrameNumber = 1 << (sps.log2MaxFrameNumMinus4 + 4);
        }

        if (idr) {
            dup.putInt(0x1);
            new NALUnit(NALUnitType.SPS, 3).write(dup);
            writeSPS(dup, sps);

            dup.putInt(0x1);
            new NALUnit(NALUnitType.PPS, 3).write(dup);
            writePPS(dup, pps);
        }

        final int mbWidth = sps.picWidthInMbsMinus1 + 1;
        final int mbHeight = sps.picHeightInMapUnitsMinus1 + 1;

        context = new EncodingContext(mbWidth, mbHeight);

        picOut = Picture.create(mbWidth << 4, mbHeight << 4, ColorSpace.YUV420J);

        topEncoded = new EncodedMB[mbWidth];

        encodeSlice(sps, pps, pic, dup, idr, frameNumber, frameType, qp);

        putLastMBLine();

        ref = picOut;

        dup.flip();
        return dup;
    }

    private void writePPS(final ByteBuffer dup, final PictureParameterSet pps) {
        final ByteBuffer tmp = ByteBuffer.allocate(1024);
        pps.write(tmp);
        tmp.flip();
        escapeNAL(tmp, dup);
    }

    private void writeSPS(final ByteBuffer dup, final SeqParameterSet sps) {
        final ByteBuffer tmp = ByteBuffer.allocate(1024);
        sps.write(tmp);
        tmp.flip();
        escapeNAL(tmp, dup);
    }

    public PictureParameterSet initPPS() {
        final PictureParameterSet pps = new PictureParameterSet();
        pps.picInitQpMinus26 = 0; // start with qp = 26
        return pps;
    }

    public SeqParameterSet initSPS(final Size sz) {
        final SeqParameterSet sps = new SeqParameterSet();
        sps.picWidthInMbsMinus1 = ((sz.width() + 15) >> 4) - 1;
        sps.picHeightInMapUnitsMinus1 = ((sz.height() + 15) >> 4) - 1;
        sps.chromaFormatIdc = ColorSpace.YUV420J;
        sps.profileIdc = 66;
        sps.levelIdc = 40;
        sps.numRefFrames = 1;
        sps.frameMbsOnlyFlag = true;
        sps.log2MaxFrameNumMinus4 = Math.max(0, MathUtil.log2(keyInterval) - 3);

        final int codedWidth = (sps.picWidthInMbsMinus1 + 1) << 4;
        final int codedHeight = (sps.picHeightInMapUnitsMinus1 + 1) << 4;
        sps.frameCroppingFlag = codedWidth != sz.width() || codedHeight != sz.height();
        sps.frameCropRightOffset = (codedWidth - sz.width() + 1) >> 1;
        sps.frameCropBottomOffset = (codedHeight - sz.height() + 1) >> 1;

        return sps;
    }

    private void encodeSlice(final SeqParameterSet sps, final PictureParameterSet pps, final Picture pic, final ByteBuffer dup, boolean idr,
                             final int frameNum, final SliceType sliceType, final int sliceQp) {
        if (idr && sliceType != SliceType.I) {
            idr = false;
        }
        context.cavlc = new CAVLC[]{new CAVLC(sps, 2, 2), new CAVLC(sps, 1, 1), new CAVLC(sps, 1, 1)};
        mbEncoderI16x16 = new MBWriterI16x16();
        mbEncoderINxN = new MBWriterINxN();
        mbEncoderP16x16 = new MBWriterP16x16(sps, ref);

        dup.putInt(0x1);
        new NALUnit(idr ? NALUnitType.IDR_SLICE : NALUnitType.NON_IDR_SLICE, 3).write(dup);
        final SliceHeader sh = new SliceHeader();
        sh.sliceType = sliceType;
        if (idr)
            sh.refPicMarkingIDR = new RefPicMarkingIDR(false, false);
        sh.pps = pps;
        sh.sps = sps;
        sh.picOrderCntLsb = (frameNum << 1) % maxPOC;
        sh.frameNum = frameNum % maxFrameNumber;
        sh.sliceQpDelta = sliceQp - (pps.picInitQpMinus26 + 26);

        ByteBuffer buf = ByteBuffer.allocate(pic.getWidth() * pic.getHeight());
        BitWriter sliceData = new BitWriter(buf);
        SliceHeaderWriter.write(sh, idr, 2, sliceData);
        final MotionEstimator estimator = new MotionEstimator(ref, sps, motionSearchRange);
        context.prevQp = sliceQp;

        final int mbWidth = sps.picWidthInMbsMinus1 + 1;
        final int mbHeight = sps.picHeightInMapUnitsMinus1 + 1;
        int oldQp = sliceQp;
        for (int mbY = 0, mbAddr = 0; mbY < mbHeight; mbY++) {
            for (int mbX = 0; mbX < mbWidth; mbX++, mbAddr++) {
                if (sliceType == SliceType.P) {
                    CAVLCWriter.writeUE(sliceData, 0); // number of skipped mbs
                }

                int qpDelta = rc.initialQpDelta(pic, mbX, mbY);
                int mbQp = oldQp + qpDelta;

                int[] mv = null;
                if (ref != null)
                    mv = estimator.mvEstimate(pic, mbX, mbY);

                final NonRdVector params = new NonRdVector(mv, IntraPredEstimator.getLumaMode(pic, context, mbX, mbY),
                    IntraPredEstimator.getLumaPred4x4(pic, context, mbX, mbY, mbQp),
                    IntraPredEstimator.getChromaMode(pic, context, mbX, mbY));

                final EncodedMB outMB = new EncodedMB();
                outMB.setPos(mbX, mbY);
                BitWriter candidate;
                EncodingContext fork;
                do {
                    candidate = sliceData.fork();
                    fork = context.fork();
                    rdMacroblock(fork, outMB, sliceType, pic, mbX, mbY, candidate, sliceQp, mbQp, params);
                    qpDelta = rc.accept(candidate.position() - sliceData.position());
                    if (qpDelta != 0)
                        mbQp += qpDelta;
                } while (qpDelta != 0);
                estimator.mvSave(mbX, new int[]{outMB.mx[0], outMB.my[0], outMB.mr[0]});
                sliceData = candidate;
                context = fork;
                oldQp = mbQp;

                context.update(outMB);

                new MBDeblocker().deblockMBP(outMB, mbX > 0 ? topEncoded[mbX - 1] : null,
                    mbY > 0 ? topEncoded[mbX] : null);
                addToReference(outMB, mbX, mbY);
            }
        }
        sliceData.write1Bit(1);
        sliceData.flush();
        buf = sliceData.getBuffer();
        buf.flip();

        escapeNAL(buf, dup);
    }

    private void calcMse(final Picture pic, final EncodedMB out, final int mbX, final int mbY, final long[] out_se) {
        final byte[] patch = new byte[256];
        for (int p = 0; p < 3; p++) {
            final byte[] outPix = out.getPixels().getData()[p];
            final int luma = p == 0 ? 1 : 0;
            MBEncoderHelper.take(pic.getPlaneData(p), pic.getPlaneWidth(p), pic.getPlaneHeight(p), mbX << (3 + luma),
                mbY << (3 + luma), patch, 8 << luma, 8 << luma);
            for (int i = 0; i < (64 << (luma * 2)); i++) {
                final int q = outPix[i] - patch[i];
                out_se[p] += q * q;
            }
        }
    }

    public static class RdVector {
        public final MBType mbType;
        public final int qp;

        public RdVector(final MBType mbType, final int qp) {
            this.mbType = mbType;
            this.qp = qp;
        }
    }

    public static class NonRdVector {
        public final int[] mv;
        public final int lumaPred16x16;
        public final int[] lumaPred4x4;
        public final int chrPred;

        public NonRdVector(final int[] mv, final int lumaPred16x16, final int[] lumaPred4x4, final int chrPred) {
            this.mv = mv;
            this.lumaPred16x16 = lumaPred16x16;
            this.lumaPred4x4 = lumaPred4x4;
            this.chrPred = chrPred;
        }
    }

    private void rdMacroblock(final EncodingContext ctx, final EncodedMB outMB, final SliceType sliceType, final Picture pic, final int mbX, final int mbY,
                              final BitWriter candidate, final int sliceQp, final int mbQp, final NonRdVector params) {
        if (!enableRdo) {
            final RdVector vector = sliceType == SliceType.P ? new RdVector(MBType.P_16x16, mbQp)
                : new RdVector(MBType.I_16x16, mbQp);
            encodeCand(ctx, outMB, sliceType, pic, mbX, mbY, candidate, params, vector);
            return;
        }

        final List<RdVector> cands = new LinkedList<>();
        cands.add(new RdVector(MBType.I_16x16, mbQp));
        cands.add(new RdVector(MBType.I_NxN, mbQp));
        if (sliceType == SliceType.P) {
            cands.add(new RdVector(MBType.P_16x16, mbQp));
        }
        long bestRd = Long.MAX_VALUE;
        RdVector bestVector = null;

        for (final RdVector rdVector : cands) {
            final EncodingContext candCtx = ctx.fork();
            final BitWriter candBits = candidate.fork();
            final long rdCost = tryVector(candCtx, sliceType, pic, mbX, mbY, candBits, sliceQp, params, rdVector);
            if (rdCost < bestRd) {
                bestRd = rdCost;
                bestVector = rdVector;
            }
        }
        encodeCand(ctx, outMB, sliceType, pic, mbX, mbY, candidate, params, bestVector);
    }

    private long tryVector(final EncodingContext ctx, final SliceType sliceType, final Picture pic, final int mbX, final int mbY, final BitWriter candidate,
                           final int sliceQp, final NonRdVector params, final RdVector vector) {
        final int start = candidate.position();
        final EncodedMB outMB = new EncodedMB();
        outMB.setPos(mbX, mbY);
        encodeCand(ctx, outMB, sliceType, pic, mbX, mbY, candidate, params, vector);

        final long[] se = new long[3];
        calcMse(pic, outMB, mbX, mbY, se);
        final long mse = (se[0] + se[1] + se[2]) / 384;
        final int bits = candidate.position() - start;
        return rdCost(mse, bits, H264Const.lambda[sliceQp]);
    }

    private long rdCost(final long mse, final int bits, final int lambda) {
        return mse + ((lambda * bits) >> 8);
    }

    private void encodeCand(final EncodingContext ctx, final EncodedMB outMB, final SliceType sliceType, final Picture pic, final int mbX, final int mbY,
                            final BitWriter candidate, final NonRdVector params, final RdVector vector) {
        if (vector.mbType == MBType.I_16x16) {
            final BitWriter tmp = new BitWriter(ByteBuffer.allocate(1024));
            final boolean cbpLuma = mbEncoderI16x16.encodeMacroblock(ctx, pic, mbX, mbY, tmp, outMB, vector.qp, params);
            final int cbpChroma = mbEncoderI16x16.getCbpChroma();

            final int i16x16TypeOffset = (cbpLuma ? 12 : 0) + cbpChroma * 4 + params.lumaPred16x16;
            final int mbTypeOffset = sliceType == SliceType.P ? 5 : 0;

            CAVLCWriter.writeUE(candidate, mbTypeOffset + vector.mbType.code() + i16x16TypeOffset);
            candidate.writeOther(tmp);
        } else if (vector.mbType == MBType.P_16x16) {
            CAVLCWriter.writeUE(candidate, vector.mbType.code());
            mbEncoderP16x16.encodeMacroblock(ctx, pic, mbX, mbY, candidate, outMB, vector.qp, params);
        } else if (vector.mbType == MBType.I_NxN) {
            CAVLCWriter.writeUE(candidate, sliceType == SliceType.P ? 5 : 0);
            mbEncoderINxN.encodeMacroblock(ctx, pic, mbX, mbY, candidate, outMB, vector.qp, params);
        } else
            throw new RuntimeException("Macroblock of type " + vector.mbType + " is not supported.");
    }

    private void addToReference(final EncodedMB outMB, final int mbX, final int mbY) {
        if (mbY > 0)
            MBEncoderHelper.putBlkPic(picOut, topEncoded[mbX].getPixels(), mbX << 4, (mbY - 1) << 4);
        topEncoded[mbX] = outMB;
    }

    private void putLastMBLine() {
        final int mbWidth = sps.picWidthInMbsMinus1 + 1;
        final int mbHeight = sps.picHeightInMapUnitsMinus1 + 1;
        for (int mbX = 0; mbX < mbWidth; mbX++)
            MBEncoderHelper.putBlkPic(picOut, topEncoded[mbX].getPixels(), mbX << 4, (mbHeight - 1) << 4);
    }

    @Override
    public ColorSpace[] getSupportedColorSpaces() {
        return new ColorSpace[]{ColorSpace.YUV420J};
    }

    @Override
    public int estimateBufferSize(final Picture frame) {
        return Math.max(1 << 16, frame.getWidth() * frame.getHeight());
    }
}
