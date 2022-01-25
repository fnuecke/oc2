/* SPDX-License-Identifier: BSD 2-Clause "Simplified" License */

package li.cil.oc2.jcodec.codecs.h264;

import li.cil.oc2.jcodec.codecs.h264.H264Utils.MvList2D;
import li.cil.oc2.jcodec.codecs.h264.decode.DeblockerInput;
import li.cil.oc2.jcodec.codecs.h264.decode.FrameReader;
import li.cil.oc2.jcodec.codecs.h264.decode.SliceDecoder;
import li.cil.oc2.jcodec.codecs.h264.decode.SliceReader;
import li.cil.oc2.jcodec.codecs.h264.decode.deblock.DeblockingFilter;
import li.cil.oc2.jcodec.codecs.h264.io.model.*;
import li.cil.oc2.jcodec.common.IntObjectMap;
import li.cil.oc2.jcodec.common.VideoDecoder;
import li.cil.oc2.jcodec.common.model.ColorSpace;
import li.cil.oc2.jcodec.common.tools.MathUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static li.cil.oc2.jcodec.codecs.h264.H264Const.*;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * <p>
 * MPEG 4 AVC ( H.264 ) Decoder
 * <p>
 * Conforms to H.264 ( ISO/IEC 14496-10 ) specifications
 *
 * @author The JCodec project
 */
public final class H264Decoder extends VideoDecoder {
    private Frame[] sRefs;
    private IntObjectMap<Frame> lRefs;
    private final List<Frame> pictureBuffer;
    private final POCManager poc;
    private final FrameReader reader;
    private ExecutorService tp;
    private final boolean threaded;

    public H264Decoder() {
        pictureBuffer = new ArrayList<>();
        poc = new POCManager();
        this.threaded = Runtime.getRuntime().availableProcessors() > 1;
        if (threaded) {
            tp = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), r -> {
                final Thread t = Executors.defaultThreadFactory().newThread(r);
                t.setName("h264 Decoder");
                t.setDaemon(true);
                return t;
            });
        }
        reader = new FrameReader();
    }

    @Override
    public Frame decodeFrame(final ByteBuffer data, final byte[][] buffer) {
        return decodeFrameFromNals(H264Utils.splitFrame(data), buffer);
    }

    public Frame decodeFrameFromNals(final List<ByteBuffer> nalUnits, final byte[][] buffer) {
        return new FrameDecoder(this).decodeFrame(nalUnits, buffer);
    }

    static class FrameDecoder {
        private SeqParameterSet activeSps;
        private DeblockingFilter filter;
        private SliceHeader firstSliceHeader;
        private NALUnit firstNu;
        private final H264Decoder dec;
        private DeblockerInput di;

        public FrameDecoder(final H264Decoder decoder) {
            this.dec = decoder;
        }

        public Frame decodeFrame(final List<ByteBuffer> nalUnits, final byte[][] buffer) {
            final List<SliceReader> sliceReaders = dec.reader.readFrame(nalUnits);
            if (sliceReaders == null || sliceReaders.size() == 0)
                return null;
            final Frame result = init(sliceReaders.get(0), buffer);
            if (dec.threaded && sliceReaders.size() > 1) {
                final List<Future<?>> futures = new ArrayList<>();
                for (final SliceReader sliceReader : sliceReaders) {
                    final SliceDecoder decoder = new SliceDecoder(activeSps, dec.sRefs, dec.lRefs, di, result);
                    futures.add(dec.tp.submit(() -> decoder.decodeFromReader(sliceReader)));
                }

                for (final Future<?> future : futures) {
                    waitForSure(future);
                }

            } else {
                for (final SliceReader sliceReader : sliceReaders) {
                    new SliceDecoder(activeSps, dec.sRefs, dec.lRefs, di, result).decodeFromReader(sliceReader);
                }
            }

            filter.deblockFrame(result);

            updateReferences(result);

            return result;
        }

        private void waitForSure(final Future<?> future) {
            while (true) {
                try {
                    future.get();
                    break;
                } catch (final Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        private void updateReferences(final Frame picture) {
            if (firstNu.nal_ref_idc != 0) {
                if (firstNu.type == NALUnitType.IDR_SLICE) {
                    performIDRMarking(firstSliceHeader.refPicMarkingIDR, picture);
                } else {
                    performMarking(firstSliceHeader.refPicMarkingNonIDR, picture);
                }
            }
        }

        private Frame init(final SliceReader sliceReader, final byte[][] buffer) {
            firstNu = sliceReader.getNALUnit();

            firstSliceHeader = sliceReader.getSliceHeader();
            activeSps = firstSliceHeader.sps;

            validateSupportedFeatures(firstSliceHeader.sps, firstSliceHeader.pps);

            if (dec.sRefs == null) {
                dec.sRefs = new Frame[1 << (firstSliceHeader.sps.log2MaxFrameNumMinus4 + 4)];
                dec.lRefs = new IntObjectMap<>();
            }

            di = new DeblockerInput(activeSps);

            final Frame result = createFrame(activeSps, buffer, firstSliceHeader.frameNum,
                di.mvs, di.refsUsed, dec.poc.calcPOC(firstSliceHeader, firstNu));

            filter = new DeblockingFilter(di);

            return result;
        }

        private void validateSupportedFeatures(final SeqParameterSet sps, final PictureParameterSet pps) {
            if (sps.mbAdaptiveFrameFieldFlag)
                throw new RuntimeException("Unsupported h264 feature: MBAFF.");
            if (sps.bitDepthLumaMinus8 != 0 || sps.bitDepthChromaMinus8 != 0)
                throw new RuntimeException("Unsupported h264 feature: High bit depth.");
            if (sps.chromaFormatIdc != ColorSpace.YUV420J)
                throw new RuntimeException("Unsupported h264 feature: " + sps.chromaFormatIdc + " color.");
            if (!sps.frameMbsOnlyFlag || sps.fieldPicFlag)
                throw new RuntimeException("Unsupported h264 feature: interlace.");
            if (pps.constrainedIntraPredFlag)
                throw new RuntimeException("Unsupported h264 feature: constrained intra prediction.");
//            if (sps.getScalingMatrix() != null || pps.extended != null && pps.extended.getScalingMatrix() != null)
//                throw new RuntimeException("Unsupported h264 feature: scaling list.");
            if (sps.qpprimeYZeroTransformBypassFlag)
                throw new RuntimeException("Unsupported h264 feature: qprime zero transform bypass.");
            if (sps.profileIdc != PROFILE_BASELINE && sps.profileIdc != PROFILE_MAIN && sps.profileIdc != PROFILE_HIGH)
                throw new RuntimeException("Unsupported h264 feature: " + sps.profileIdc + " profile.");
        }

        public void performIDRMarking(final RefPicMarkingIDR refPicMarkingIDR, final Frame picture) {
            clearAll();
            dec.pictureBuffer.clear();

            final Frame saved = saveRef(picture);
            if (refPicMarkingIDR.useForlongTerm()) {
                dec.lRefs.put(0, saved);
                saved.setShortTerm(false);
            } else
                dec.sRefs[firstSliceHeader.frameNum] = saved;
        }

        private Frame saveRef(final Frame decoded) {
            final Frame frame = dec.pictureBuffer.size() > 0 ? dec.pictureBuffer.remove(0) : Frame.createFrame(decoded);
            frame.copyFromFrame(decoded);
            return frame;
        }

        private void releaseRef(final Frame picture) {
            if (picture != null) {
                dec.pictureBuffer.add(picture);
            }
        }

        public void clearAll() {
            for (int i = 0; i < dec.sRefs.length; i++) {
                releaseRef(dec.sRefs[i]);
                dec.sRefs[i] = null;
            }
            final int[] keys = dec.lRefs.keys();
            for (final int key : keys) {
                releaseRef(dec.lRefs.get(key));
            }
            dec.lRefs.clear();
        }

        public void performMarking(final RefPicMarking refPicMarking, final Frame picture) {
            Frame saved = saveRef(picture);

            if (refPicMarking != null) {
                final RefPicMarking.Instruction[] instructions = refPicMarking.instructions();
                for (final RefPicMarking.Instruction instr : instructions) {
                    switch (instr.type()) {
                        case REMOVE_SHORT -> unrefShortTerm(instr.arg1());
                        case REMOVE_LONG -> unrefLongTerm(instr.arg1());
                        case CONVERT_INTO_LONG -> convert(instr.arg1(), instr.arg2());
                        case TRUNK_LONG -> truncateLongTerm(instr.arg1() - 1);
                        case CLEAR -> clearAll();
                        case MARK_LONG -> {
                            saveLong(saved, instr.arg1());
                            saved = null;
                        }
                    }
                }
            }
            if (saved != null)
                saveShort(saved);

            final int maxFrames = 1 << (activeSps.log2MaxFrameNumMinus4 + 4);
            if (refPicMarking == null) {
                final int maxShort = Math.max(1, activeSps.numRefFrames - dec.lRefs.size());
                int min = Integer.MAX_VALUE, num = 0, minFn = 0;
                for (int i = 0; i < dec.sRefs.length; i++) {
                    if (dec.sRefs[i] != null) {
                        final int fnWrap = unwrap(firstSliceHeader.frameNum, dec.sRefs[i].getFrameNo(), maxFrames);
                        if (fnWrap < min) {
                            min = fnWrap;
                            minFn = dec.sRefs[i].getFrameNo();
                        }
                        num++;
                    }
                }
                if (num > maxShort) {
                    releaseRef(dec.sRefs[minFn]);
                    dec.sRefs[minFn] = null;
                }
            }
        }

        private int unwrap(final int thisFrameNo, final int refFrameNo, final int maxFrames) {
            return refFrameNo > thisFrameNo ? refFrameNo - maxFrames : refFrameNo;
        }

        private void saveShort(final Frame saved) {
            dec.sRefs[firstSliceHeader.frameNum] = saved;
        }

        private void saveLong(final Frame saved, final int longNo) {
            final Frame prev = dec.lRefs.get(longNo);
            if (prev != null)
                releaseRef(prev);
            saved.setShortTerm(false);

            dec.lRefs.put(longNo, saved);
        }

        private void truncateLongTerm(final int maxLongNo) {
            final int[] keys = dec.lRefs.keys();
            for (final int key : keys) {
                if (key > maxLongNo) {
                    releaseRef(dec.lRefs.get(key));
                    dec.lRefs.remove(key);
                }
            }
        }

        private void convert(final int shortNo, final int longNo) {
            final int ind = MathUtil.wrap(firstSliceHeader.frameNum - shortNo,
                1 << (firstSliceHeader.sps.log2MaxFrameNumMinus4 + 4));
            releaseRef(dec.lRefs.get(longNo));
            dec.lRefs.put(longNo, dec.sRefs[ind]);
            dec.sRefs[ind] = null;
            dec.lRefs.get(longNo).setShortTerm(false);
        }

        private void unrefLongTerm(final int longNo) {
            releaseRef(dec.lRefs.get(longNo));
            dec.lRefs.remove(longNo);
        }

        private void unrefShortTerm(final int shortNo) {
            final int ind = MathUtil.wrap(firstSliceHeader.frameNum - shortNo,
                1 << (firstSliceHeader.sps.log2MaxFrameNumMinus4 + 4));
            releaseRef(dec.sRefs[ind]);
            dec.sRefs[ind] = null;
        }
    }

    public static Frame createFrame(final SeqParameterSet sps, final byte[][] buffer, final int frameNum,
                                    final MvList2D mvs, final Frame[][][] refsUsed, final int POC) {
        final int width = sps.picWidthInMbsMinus1 + 1 << 4;
        final int height = SeqParameterSet.getPicHeightInMbs(sps) << 4;

        return new Frame(width, height, buffer, ColorSpace.YUV420, frameNum, mvs, refsUsed, POC);
    }
}
