/* SPDX-License-Identifier: BSD 2-Clause "Simplified" License */

package li.cil.oc2.jcodec.codecs.h264.io.model;

import li.cil.oc2.jcodec.codecs.h264.H264Const;
import li.cil.oc2.jcodec.codecs.h264.decode.CAVLCReader;
import li.cil.oc2.jcodec.common.io.BitReader;
import li.cil.oc2.jcodec.common.io.BitWriter;
import li.cil.oc2.jcodec.common.model.ColorSpace;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static li.cil.oc2.jcodec.codecs.h264.io.write.CAVLCWriter.*;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * <p>
 * Sequence Parameter Set structure of h264 bitstream
 * <p>
 * capable to serialize and deserialize with CAVLC bitstream
 *
 * @author The JCodec project
 */
public final class SeqParameterSet {
    // pic_order_cnt_type
    public int picOrderCntType;
    // field_pic_flag
    public boolean fieldPicFlag;
    // delta_pic_order_always_zero_flag
    public boolean deltaPicOrderAlwaysZeroFlag;
    // mb_adaptive_frame_field_flag
    public boolean mbAdaptiveFrameFieldFlag;
    // direct_8x8_inference_flag
    public boolean direct8x8InferenceFlag;
    // chroma_format_idc
    public ColorSpace chromaFormatIdc;
    // log2_max_frame_num_minus4
    public int log2MaxFrameNumMinus4;
    // log2_max_pic_order_cnt_lsb_minus4
    public int log2MaxPicOrderCntLsbMinus4;
    // pic_height_in_map_units_minus1
    public int picHeightInMapUnitsMinus1;
    // pic_width_in_mbs_minus1
    public int picWidthInMbsMinus1;
    // bit_depth_luma_minus8
    public int bitDepthLumaMinus8;
    // bit_depth_chroma_minus8
    public int bitDepthChromaMinus8;
    // qpprime_y_zero_transform_bypass_flag
    public boolean qpprimeYZeroTransformBypassFlag;
    // profile_idc
    public int profileIdc;
    // constraint_set0_flag
    public boolean constraintSet0Flag;
    // constraint_set1_flag
    public boolean constraintSet1Flag;
    // constraint_set2_flag
    public boolean constraintSet2Flag;
    // constraint_set_3_flag
    public boolean constraintSet3Flag;
    // constraint_set_4_flag
    public boolean constraintSet4Flag;
    // constraint_set_5_flag
    public boolean constraintSet5Flag;
    // level_idc
    public int levelIdc;
    // seq_parameter_set_id
    public int seqParameterSetId;
    /**
     * separate_colour_plane_flag. When a picture is coded using three separate
     * colour planes (separate_colour_plane_flag is equal to 1), a slice
     * contains only macroblocks of one colour component being identified by the
     * corresponding value of colour_plane_id, and each colour component array
     * of a picture consists of slices having the same colour_plane_id value.
     * Coded slices with different values of colour_plane_id within an access
     * unit can be interleaved with each other under the constraint that for
     * each value of colour_plane_id, the coded slice NAL units with that value
     * colour_plane_id shall be in the order of increasing macroblock address
     * for the first macroblock of each coded slice NAL unit.
     */
    public boolean separateColourPlaneFlag;

    /**
     * offset_for_non_ref_pic is used to calculate the picture order count of a
     * non-reference picture as specified in 8.2.1. The value of
     * offset_for_non_ref_pic shall be in the range of -231 to 231 - 1,
     * inclusive.
     */
    public int offsetForNonRefPic;

    /**
     * offset_for_top_to_bottom_field is used to calculate the picture order
     * count of a bottom field as specified in subclause 8.2.1. The value of
     * offset_for_top_to_bottom_field shall be in the range of -231 to 231 - 1,
     * inclusive.
     */
    public int offsetForTopToBottomField;

    // num_ref_frames
    public int numRefFrames;

    /**
     * gaps_in_frame_num_value_allowed_flag specifies the allowed values of
     * frame_num as specified in subclause 7.4.3 and the decoding process in
     * case of an inferred gap between values of frame_num as specified in
     * subclause 8.2.5.2.
     */
    public boolean gapsInFrameNumValueAllowedFlag;

    /**
     * frame_mbs_only_flag equal to 0 specifies that coded pictures of the coded
     * video sequence may either be coded fields or coded frames.
     * frame_mbs_only_flag equal to 1 specifies that every coded picture of the
     * coded video sequence is a coded frame containing only frame macroblocks.
     */
    public boolean frameMbsOnlyFlag;

    // frame_cropping_flag
    public boolean frameCroppingFlag;

    // frame_crop_left_offset
    public int frameCropLeftOffset;

    // frame_crop_right_offset
    public int frameCropRightOffset;

    // frame_crop_top_offset
    public int frameCropTopOffset;

    // frame_crop_bottom_offset
    public int frameCropBottomOffset;

    public int[] offsetForRefFrame;
    public VUIParameters vuiParams;
    public int[][] scalingMatrix;

    // num_ref_frames_in_pic_order_cnt_cycle
    public int numRefFramesInPicOrderCntCycle;

    public static ColorSpace getColor(final int id) {
        switch (id) {
            case 0:
                return ColorSpace.MONO;
            case 1:
                return ColorSpace.YUV420J;
            case 2:
                return ColorSpace.YUV422;
            case 3:
                return ColorSpace.YUV444;
        }
        throw new RuntimeException("Colorspace not supported");
    }

    public static int fromColor(final ColorSpace color) {
        if (color == ColorSpace.MONO) {
            return 0;
        } else if (color == ColorSpace.YUV420J) {
            return 1;
        } else if (color == ColorSpace.YUV422) {
            return 2;
        } else if (color == ColorSpace.YUV444) {
            return 3;
        }
        throw new RuntimeException("Colorspace not supported");
    }

    public static SeqParameterSet read(final ByteBuffer is) {
        final BitReader _in = BitReader.createBitReader(is);
        final SeqParameterSet sps = new SeqParameterSet();

        sps.profileIdc = CAVLCReader.readNBit(_in, 8); // SPS: profile_idc
        sps.constraintSet0Flag = CAVLCReader.readBool(_in); // SPS: constraint_set_0_flag
        sps.constraintSet1Flag = CAVLCReader.readBool(_in); // SPS: constraint_set_1_flag
        sps.constraintSet2Flag = CAVLCReader.readBool(_in); // SPS: constraint_set_2_flag
        sps.constraintSet3Flag = CAVLCReader.readBool(_in); // SPS: constraint_set_3_flag
        sps.constraintSet4Flag = CAVLCReader.readBool(_in); // SPS: constraint_set_4_flag
        sps.constraintSet5Flag = CAVLCReader.readBool(_in); // SPS: constraint_set_5_flag
        CAVLCReader.readNBit(_in, 2); // SPS: reserved_zero_2bits
        sps.levelIdc = CAVLCReader.readNBit(_in, 8); // SPS: level_idc
        sps.seqParameterSetId = CAVLCReader.readUE(_in); // SPS: seq_parameter_set_id

        if (sps.profileIdc == 100 || sps.profileIdc == 110 || sps.profileIdc == 122 || sps.profileIdc == 144) {
            sps.chromaFormatIdc = getColor(CAVLCReader.readUE(_in)); // SPS: chroma_format_idc
            if (sps.chromaFormatIdc == ColorSpace.YUV444) {
                sps.separateColourPlaneFlag = CAVLCReader.readBool(_in); // SPS: separate_colour_plane_flag
            }
            sps.bitDepthLumaMinus8 = CAVLCReader.readUE(_in); // SPS: bit_depth_luma_minus8
            sps.bitDepthChromaMinus8 = CAVLCReader.readUE(_in); // SPS: bit_depth_chroma_minus8
            sps.qpprimeYZeroTransformBypassFlag = CAVLCReader.readBool(_in); // SPS: qpprime_y_zero_transform_bypass_flag
            final boolean seqScalingMatrixPresent = CAVLCReader.readBool(_in); // SPS: seq_scaling_matrix_present_lag
            if (seqScalingMatrixPresent) {
                readScalingListMatrix(_in, sps);
            }
        } else {
            sps.chromaFormatIdc = ColorSpace.YUV420J;
        }
        sps.log2MaxFrameNumMinus4 = CAVLCReader.readUE(_in); // SPS: log2_max_frame_num_minus4
        sps.picOrderCntType = CAVLCReader.readUE(_in); // SPS: pic_order_cnt_type
        if (sps.picOrderCntType == 0) {
            sps.log2MaxPicOrderCntLsbMinus4 = CAVLCReader.readUE(_in); // SPS: log2_max_pic_order_cnt_lsb_minus4
        } else if (sps.picOrderCntType == 1) {
            sps.deltaPicOrderAlwaysZeroFlag = CAVLCReader.readBool(_in); // SPS: delta_pic_order_always_zero_flag
            sps.offsetForNonRefPic = CAVLCReader.readSE(_in); // SPS: offset_for_non_ref_pic
            sps.offsetForTopToBottomField = CAVLCReader.readSE(_in); // SPS: offset_for_top_to_bottom_field
            sps.numRefFramesInPicOrderCntCycle = CAVLCReader.readUE(_in); // SPS: num_ref_frames_in_pic_order_cnt_cycle
            sps.offsetForRefFrame = new int[sps.numRefFramesInPicOrderCntCycle];
            for (int i = 0; i < sps.numRefFramesInPicOrderCntCycle; i++) {
                sps.offsetForRefFrame[i] = CAVLCReader.readSE(_in); // SPS: offsetForRefFrame [i]
            }
        }
        sps.numRefFrames = CAVLCReader.readUE(_in); // SPS: num_ref_frames
        sps.gapsInFrameNumValueAllowedFlag = CAVLCReader.readBool(_in); // SPS: gaps_in_frame_num_value_allowed_flag
        sps.picWidthInMbsMinus1 = CAVLCReader.readUE(_in); // SPS: pic_width_in_mbs_minus1
        sps.picHeightInMapUnitsMinus1 = CAVLCReader.readUE(_in); // SPS: pic_height_in_map_units_minus1
        sps.frameMbsOnlyFlag = CAVLCReader.readBool(_in); // SPS: frame_mbs_only_flag
        if (!sps.frameMbsOnlyFlag) {
            sps.mbAdaptiveFrameFieldFlag = CAVLCReader.readBool(_in); // SPS: mb_adaptive_frame_field_flag
        }
        sps.direct8x8InferenceFlag = CAVLCReader.readBool(_in); // SPS: direct_8x8_inference_flag
        sps.frameCroppingFlag = CAVLCReader.readBool(_in); // SPS: frame_cropping_flag
        if (sps.frameCroppingFlag) {
            sps.frameCropLeftOffset = CAVLCReader.readUE(_in); // SPS: frame_crop_left_offset
            sps.frameCropRightOffset = CAVLCReader.readUE(_in); // SPS: frame_crop_right_offset
            sps.frameCropTopOffset = CAVLCReader.readUE(_in); // SPS: frame_crop_top_offset
            sps.frameCropBottomOffset = CAVLCReader.readUE(_in); // SPS: frame_crop_bottom_offset
        }
        final boolean vuiParametersPresentFlag = CAVLCReader.readBool(_in); // SPS: vui_parameters_present_flag
        if (vuiParametersPresentFlag)
            sps.vuiParams = readVUIParameters(_in);

        return sps;
    }

    public static void writeScalingList(final BitWriter out, final int[][] scalingMatrix, final int which) {
        // Want to find out if the default scaling list is actually used
        final boolean useDefaultScalingMatrixFlag = switch (which) {
            case 0 -> Arrays.equals(scalingMatrix[which], H264Const.defaultScalingList4x4Intra); // 4x4 intra y
            case 1, 2 -> Arrays.equals(scalingMatrix[which], scalingMatrix[0]);
            case 3 -> Arrays.equals(scalingMatrix[which], H264Const.defaultScalingList4x4Inter);
            case 4, 5 -> Arrays.equals(scalingMatrix[which], scalingMatrix[3]);
            case 6 -> Arrays.equals(scalingMatrix[which], H264Const.defaultScalingList8x8Intra);
            case 7 -> Arrays.equals(scalingMatrix[which], H264Const.defaultScalingList8x8Inter);
            default -> false;
        };
        final int[] scalingList = scalingMatrix[which];

        if (useDefaultScalingMatrixFlag) {
            // SPS:
            writeSE(out, -8);
            return;
        }

        int lastScale = 8;
        for (final int i : scalingList) {
            final int deltaScale = i - lastScale - 256;
            // SPS:
            writeSE(out, deltaScale);
            lastScale = i;
        }
    }

    public static int[] readScalingList(final BitReader src, final int sizeOfScalingList) {

        final int[] scalingList = new int[sizeOfScalingList];
        int lastScale = 8;
        int nextScale = 8;
        for (int j = 0; j < sizeOfScalingList; j++) {
            if (nextScale != 0) {
                final int deltaScale = CAVLCReader.readSE(src); // deltaScale
                nextScale = (lastScale + deltaScale + 256) % 256;
                if (j == 0 && nextScale == 0)
                    return null;
            }
            scalingList[j] = nextScale == 0 ? lastScale : nextScale;
            lastScale = scalingList[j];
        }
        return scalingList;
    }

    private static void readScalingListMatrix(final BitReader src, final SeqParameterSet sps) {
        sps.scalingMatrix = new int[8][];
        for (int i = 0; i < 8; i++) {
            final boolean seqScalingListPresentFlag = CAVLCReader.readBool(src); // SPS: seqScalingListPresentFlag
            if (seqScalingListPresentFlag) {
                final int scalingListSize = i < 6 ? 16 : 64;
                sps.scalingMatrix[i] = readScalingList(src, scalingListSize);
            }
        }
    }

    private static VUIParameters readVUIParameters(final BitReader _in) {
        final VUIParameters vuip = new VUIParameters();
        vuip.aspectRatioInfoPresentFlag = CAVLCReader.readBool(_in); // VUI: aspect_ratio_info_present_flag
        if (vuip.aspectRatioInfoPresentFlag) {
            vuip.aspectRatio = AspectRatio.fromValue(CAVLCReader.readNBit(_in, 8)); // VUI: aspect_ratio
            if (vuip.aspectRatio == AspectRatio.Extended_SAR) {
                vuip.sarWidth = CAVLCReader.readNBit(_in, 16); // VUI: sar_width
                vuip.sarHeight = CAVLCReader.readNBit(_in, 16); // VUI: sar_height
            }
        }
        vuip.overscanInfoPresentFlag = CAVLCReader.readBool(_in); // VUI: overscan_info_present_flag
        if (vuip.overscanInfoPresentFlag) {
            vuip.overscanAppropriateFlag = CAVLCReader.readBool(_in); // VUI: overscan_appropriate_flag
        }
        vuip.videoSignalTypePresentFlag = CAVLCReader.readBool(_in); // VUI: video_signal_type_present_flag
        if (vuip.videoSignalTypePresentFlag) {
            vuip.videoFormat = CAVLCReader.readNBit(_in, 3); // VUI: video_format
            vuip.videoFullRangeFlag = CAVLCReader.readBool(_in); // VUI: video_full_range_flag
            vuip.colourDescriptionPresentFlag = CAVLCReader.readBool(_in); // VUI: colour_description_present_flag
            if (vuip.colourDescriptionPresentFlag) {
                vuip.colourPrimaries = CAVLCReader.readNBit(_in, 8); // VUI: colour_primaries
                vuip.transferCharacteristics = CAVLCReader.readNBit(_in, 8); // VUI: transfer_characteristics
                vuip.matrixCoefficients = CAVLCReader.readNBit(_in, 8); // VUI: matrix_coefficients
            }
        }
        vuip.chromaLocInfoPresentFlag = CAVLCReader.readBool(_in); // VUI: chroma_loc_info_present_flag
        if (vuip.chromaLocInfoPresentFlag) {
            vuip.chromaSampleLocTypeTopField = CAVLCReader.readUE(_in); // VUI chroma_sample_loc_type_top_field
            vuip.chromaSampleLocTypeBottomField = CAVLCReader.readUE(_in); // VUI chroma_sample_loc_type_bottom_field
        }
        vuip.timingInfoPresentFlag = CAVLCReader.readBool(_in); // VUI: timing_info_present_flag
        if (vuip.timingInfoPresentFlag) {
            vuip.numUnitsInTick = CAVLCReader.readNBit(_in, 32); // VUI: num_units_in_tick
            vuip.timeScale = CAVLCReader.readNBit(_in, 32); // VUI: time_scale
            vuip.fixedFrameRateFlag = CAVLCReader.readBool(_in); // VUI: fixed_frame_rate_flag
        }
        final boolean nalHRDParametersPresentFlag = CAVLCReader.readBool(_in); // VUI: nal_hrd_parameters_present_flag
        if (nalHRDParametersPresentFlag)
            vuip.nalHRDParams = readHRDParameters(_in);
        final boolean vclHRDParametersPresentFlag = CAVLCReader.readBool(_in); // VUI: vcl_hrd_parameters_present_flag
        if (vclHRDParametersPresentFlag)
            vuip.vclHRDParams = readHRDParameters(_in);
        if (nalHRDParametersPresentFlag || vclHRDParametersPresentFlag) {
            vuip.lowDelayHrdFlag = CAVLCReader.readBool(_in); // VUI: low_delay_hrd_flag
        }
        vuip.picStructPresentFlag = CAVLCReader.readBool(_in); // VUI: pic_struct_present_flag
        final boolean bitstreamRestrictionFlag = CAVLCReader.readBool(_in); // VUI: bitstream_restriction_flag
        if (bitstreamRestrictionFlag) {
            vuip.bitstreamRestriction = new VUIParameters.BitstreamRestriction();
            vuip.bitstreamRestriction.motionVectorsOverPicBoundariesFlag = CAVLCReader.readBool(_in); // VUI: motion_vectors_over_pic_boundaries_flag
            vuip.bitstreamRestriction.maxBytesPerPicDenom = CAVLCReader.readUE(_in); // VUI max_bytes_per_pic_denom
            vuip.bitstreamRestriction.maxBitsPerMbDenom = CAVLCReader.readUE(_in); // VUI max_bits_per_mb_denom
            vuip.bitstreamRestriction.log2MaxMvLengthHorizontal = CAVLCReader.readUE(_in); // VUI log2_max_mv_length_horizontal
            vuip.bitstreamRestriction.log2MaxMvLengthVertical = CAVLCReader.readUE(_in); // VUI log2_max_mv_length_vertical
            vuip.bitstreamRestriction.numReorderFrames = CAVLCReader.readUE(_in); // VUI num_reorder_frames
            vuip.bitstreamRestriction.maxDecFrameBuffering = CAVLCReader.readUE(_in); // VUI max_dec_frame_buffering
        }

        return vuip;
    }

    private static HRDParameters readHRDParameters(final BitReader _in) {
        final HRDParameters hrd = new HRDParameters();
        hrd.cpbCntMinus1 = CAVLCReader.readUE(_in); // SPS: cpb_cnt_minus1
        hrd.bitRateScale = CAVLCReader.readNBit(_in, 4); // HRD: bit_rate_scale
        hrd.cpbSizeScale = CAVLCReader.readNBit(_in, 4); // HRD: cpb_size_scale
        hrd.bitRateValueMinus1 = new int[hrd.cpbCntMinus1 + 1];
        hrd.cpbSizeValueMinus1 = new int[hrd.cpbCntMinus1 + 1];
        hrd.cbrFlag = new boolean[hrd.cpbCntMinus1 + 1];

        for (int SchedSelIdx = 0; SchedSelIdx <= hrd.cpbCntMinus1; SchedSelIdx++) {
            hrd.bitRateValueMinus1[SchedSelIdx] = CAVLCReader.readUE(_in); // HRD: bit_rate_value_minus1
            hrd.cpbSizeValueMinus1[SchedSelIdx] = CAVLCReader.readUE(_in); // HRD: cpb_size_value_minus1
            hrd.cbrFlag[SchedSelIdx] = CAVLCReader.readBool(_in); // HRD: cbr_flag
        }
        hrd.initialCpbRemovalDelayLengthMinus1 = CAVLCReader.readNBit(_in, 5); // HRD: initial_cpb_removal_delay_length_minus1
        hrd.cpbRemovalDelayLengthMinus1 = CAVLCReader.readNBit(_in, 5); // HRD: cpb_removal_delay_length_minus1
        hrd.dpbOutputDelayLengthMinus1 = CAVLCReader.readNBit(_in, 5); // HRD: dpb_output_delay_length_minus1
        hrd.timeOffsetLength = CAVLCReader.readNBit(_in, 5); // HRD: time_offset_length
        return hrd;
    }

    public void write(final ByteBuffer out) {
        final BitWriter writer = new BitWriter(out);

        writeNBit(writer, profileIdc, 8, ""); // SPS: profile_idc
        writeBool(writer, constraintSet0Flag); // SPS: constraint_set_0_flag
        writeBool(writer, constraintSet1Flag); // SPS: constraint_set_1_flag
        writeBool(writer, constraintSet2Flag); // SPS: constraint_set_2_flag
        writeBool(writer, constraintSet3Flag); // SPS: constraint_set_3_flag
        writeBool(writer, constraintSet4Flag); // SPS: constraint_set_4_flag
        writeBool(writer, constraintSet5Flag); // SPS: constraint_set_5_flag
        writeNBit(writer, 0, 2, ""); // SPS: reserved
        writeNBit(writer, levelIdc, 8, ""); // SPS: level_idc
        // SPS: seq_parameter_set_id
        writeUE(writer, seqParameterSetId);

        if (profileIdc == 100 || profileIdc == 110 || profileIdc == 122 || profileIdc == 144) {
            // SPS: chroma_format_idc
            writeUE(writer, fromColor(chromaFormatIdc));
            if (chromaFormatIdc == ColorSpace.YUV444) {
                writeBool(writer, separateColourPlaneFlag); // SPS: residual_color_transform_flag
            }
            // SPS:
            writeUE(writer, bitDepthLumaMinus8);
            // SPS:
            writeUE(writer, bitDepthChromaMinus8);
            writeBool(writer, qpprimeYZeroTransformBypassFlag); // SPS: qpprime_y_zero_transform_bypass_flag
            writeBool(writer, scalingMatrix != null); // SPS:
            if (scalingMatrix != null) {
                for (int i = 0; i < 8; i++) {
                    writeBool(writer, scalingMatrix[i] != null); // SPS:
                    if (scalingMatrix[i] != null)
                        writeScalingList(writer, scalingMatrix, i);
                }
            }
        }
        // SPS: log2_max_frame_num_minus4
        writeUE(writer, log2MaxFrameNumMinus4);
        // SPS: pic_order_cnt_type
        writeUE(writer, picOrderCntType);
        if (picOrderCntType == 0) {
            // SPS: log2_max_pic_order_cnt_lsb_minus4
            writeUE(writer, log2MaxPicOrderCntLsbMinus4);
        } else if (picOrderCntType == 1) {
            writeBool(writer, deltaPicOrderAlwaysZeroFlag); // SPS: delta_pic_order_always_zero_flag
            // SPS: offset_for_non_ref_pic
            writeSE(writer, offsetForNonRefPic);
            // SPS: offset_for_top_to_bottom_field
            writeSE(writer, offsetForTopToBottomField);
            // SPS:
            writeUE(writer, offsetForRefFrame.length);
            for (final int j : offsetForRefFrame)
                writeSE(writer, j);
        }
        // SPS: num_ref_frames
        writeUE(writer, numRefFrames);
        writeBool(writer, gapsInFrameNumValueAllowedFlag); // SPS: gaps_in_frame_num_value_allowed_flag
        // SPS: pic_width_in_mbs_minus1
        writeUE(writer, picWidthInMbsMinus1);
        // SPS: pic_height_in_map_units_minus1
        writeUE(writer, picHeightInMapUnitsMinus1);
        writeBool(writer, frameMbsOnlyFlag); // SPS: frame_mbs_only_flag
        if (!frameMbsOnlyFlag) {
            writeBool(writer, mbAdaptiveFrameFieldFlag); // SPS: mb_adaptive_frame_field_flag
        }
        writeBool(writer, direct8x8InferenceFlag); // SPS: direct_8x8_inference_flag
        writeBool(writer, frameCroppingFlag); // SPS: frame_cropping_flag
        if (frameCroppingFlag) {
            // SPS: frame_crop_left_offset
            writeUE(writer, frameCropLeftOffset);
            // SPS: frame_crop_right_offset
            writeUE(writer, frameCropRightOffset);
            // SPS: frame_crop_top_offset
            writeUE(writer, frameCropTopOffset);
            // SPS: frame_crop_bottom_offset
            writeUE(writer, frameCropBottomOffset);
        }
        writeBool(writer, vuiParams != null); // SPS:
        if (vuiParams != null)
            writeVUIParameters(vuiParams, writer);

        writeTrailingBits(writer);
    }

    private void writeVUIParameters(final VUIParameters vuip, final BitWriter writer) {
        writeBool(writer, vuip.aspectRatioInfoPresentFlag); // VUI: aspect_ratio_info_present_flag
        if (vuip.aspectRatioInfoPresentFlag) {
            writeNBit(writer, vuip.aspectRatio.value(), 8, ""); // VUI: aspect_ratio
            if (vuip.aspectRatio == AspectRatio.Extended_SAR) {
                writeNBit(writer, vuip.sarWidth, 16, ""); // VUI: sar_width
                writeNBit(writer, vuip.sarHeight, 16, ""); // VUI: sar_height
            }
        }
        writeBool(writer, vuip.overscanInfoPresentFlag); // VUI: overscan_info_present_flag
        if (vuip.overscanInfoPresentFlag) {
            writeBool(writer, vuip.overscanAppropriateFlag); // VUI: overscan_appropriate_flag
        }
        writeBool(writer, vuip.videoSignalTypePresentFlag); // VUI: video_signal_type_present_flag
        if (vuip.videoSignalTypePresentFlag) {
            writeNBit(writer, vuip.videoFormat, 3, ""); // VUI: video_format
            writeBool(writer, vuip.videoFullRangeFlag); // VUI: video_full_range_flag
            writeBool(writer, vuip.colourDescriptionPresentFlag); // VUI: colour_description_present_flag
            if (vuip.colourDescriptionPresentFlag) {
                writeNBit(writer, vuip.colourPrimaries, 8, ""); // VUI: colour_primaries
                writeNBit(writer, vuip.transferCharacteristics, 8, ""); // VUI: transfer_characteristics
                writeNBit(writer, vuip.matrixCoefficients, 8, ""); // VUI: matrix_coefficients
            }
        }
        writeBool(writer, vuip.chromaLocInfoPresentFlag); // VUI: chroma_loc_info_present_flag
        if (vuip.chromaLocInfoPresentFlag) {
            // VUI: chroma_sample_loc_type_top_field
            writeUE(writer, vuip.chromaSampleLocTypeTopField);
            // VUI: chroma_sample_loc_type_bottom_field
            writeUE(writer, vuip.chromaSampleLocTypeBottomField);
        }
        writeBool(writer, vuip.timingInfoPresentFlag); // VUI: timing_info_present_flag
        if (vuip.timingInfoPresentFlag) {
            writeNBit(writer, vuip.numUnitsInTick, 32, ""); // VUI: num_units_in_tick
            writeNBit(writer, vuip.timeScale, 32, ""); // VUI: time_scale
            writeBool(writer, vuip.fixedFrameRateFlag); // VUI: fixed_frame_rate_flag
        }
        writeBool(writer, vuip.nalHRDParams != null); // VUI:
        if (vuip.nalHRDParams != null) {
            writeHRDParameters(vuip.nalHRDParams, writer);
        }
        writeBool(writer, vuip.vclHRDParams != null); // VUI:
        if (vuip.vclHRDParams != null) {
            writeHRDParameters(vuip.vclHRDParams, writer);
        }

        if (vuip.nalHRDParams != null || vuip.vclHRDParams != null) {
            writeBool(writer, vuip.lowDelayHrdFlag); // VUI: low_delay_hrd_flag
        }
        writeBool(writer, vuip.picStructPresentFlag); // VUI: pic_struct_present_flag
        writeBool(writer, vuip.bitstreamRestriction != null); // VUI:
        if (vuip.bitstreamRestriction != null) {
            writeBool(writer, vuip.bitstreamRestriction.motionVectorsOverPicBoundariesFlag); // VUI: motion_vectors_over_pic_boundaries_flag
            // VUI: max_bytes_per_pic_denom
            writeUE(writer, vuip.bitstreamRestriction.maxBytesPerPicDenom);
            // VUI: max_bits_per_mb_denom
            writeUE(writer, vuip.bitstreamRestriction.maxBitsPerMbDenom);
            // VUI: log2_max_mv_length_horizontal
            writeUE(writer, vuip.bitstreamRestriction.log2MaxMvLengthHorizontal);
            // VUI: log2_max_mv_length_vertical
            writeUE(writer, vuip.bitstreamRestriction.log2MaxMvLengthVertical);
            // VUI: num_reorder_frames
            writeUE(writer, vuip.bitstreamRestriction.numReorderFrames);
            // VUI: max_dec_frame_buffering
            writeUE(writer, vuip.bitstreamRestriction.maxDecFrameBuffering);
        }

    }

    private void writeHRDParameters(final HRDParameters hrd, final BitWriter writer) {
        // HRD: cpb_cnt_minus1
        writeUE(writer, hrd.cpbCntMinus1);
        writeNBit(writer, hrd.bitRateScale, 4, ""); // HRD: bit_rate_scale
        writeNBit(writer, hrd.cpbSizeScale, 4, ""); // HRD: cpb_size_scale

        for (int SchedSelIdx = 0; SchedSelIdx <= hrd.cpbCntMinus1; SchedSelIdx++) {
            // HRD:
            writeUE(writer, hrd.bitRateValueMinus1[SchedSelIdx]);
            // HRD:
            writeUE(writer, hrd.cpbSizeValueMinus1[SchedSelIdx]);
            writeBool(writer, hrd.cbrFlag[SchedSelIdx]); // HRD:
        }
        writeNBit(writer, hrd.initialCpbRemovalDelayLengthMinus1, 5, ""); // HRD: initial_cpb_removal_delay_length_minus1
        writeNBit(writer, hrd.cpbRemovalDelayLengthMinus1, 5, ""); // HRD: cpb_removal_delay_length_minus1
        writeNBit(writer, hrd.dpbOutputDelayLengthMinus1, 5, ""); // HRD: dpb_output_delay_length_minus1
        writeNBit(writer, hrd.timeOffsetLength, 5, ""); // HRD: time_offset_length
    }

    public SeqParameterSet copy() {
        final ByteBuffer buf = ByteBuffer.allocate(2048);
        write(buf);
        buf.flip();
        return read(buf);
    }

    public static int getPicHeightInMbs(final SeqParameterSet sps) {
        return (sps.picHeightInMapUnitsMinus1 + 1) << (sps.frameMbsOnlyFlag ? 0 : 1);
    }
}
