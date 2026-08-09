/* SPDX-License-Identifier: BSD 2-Clause "Simplified" License */

package li.cil.oc2.jcodec.codecs.h264.io.write;

import li.cil.oc2.jcodec.codecs.h264.io.model.*;
import li.cil.oc2.jcodec.common.io.BitWriter;
import li.cil.oc2.jcodec.common.model.ColorSpace;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * <p>
 * A writer for slice header data structure
 *
 * @author The JCodec project
 */
public final class SliceHeaderWriter {
    private SliceHeaderWriter() {
    }

    public static void write(final SliceHeader sliceHeader, final boolean idrSlice, final int nalRefIdc, final BitWriter writer) {
        final SeqParameterSet sps = sliceHeader.sps;
        final PictureParameterSet pps = sliceHeader.pps;
        // SH: first_mb_in_slice
        CAVLCWriter.writeUE(writer, sliceHeader.firstMbInSlice);
        // SH: slice_type
        final int value = sliceHeader.sliceType.ordinal() + (sliceHeader.sliceTypeRestr ? 5 : 0);
        CAVLCWriter.writeUE(writer, value);
        // SH: pic_parameter_set_id
        CAVLCWriter.writeUE(writer, sliceHeader.picParameterSetId);
        if (sliceHeader.frameNum > (1 << (sps.log2MaxFrameNumMinus4 + 4))) {
            throw new IllegalArgumentException("frame_num > " + (1 << (sps.log2MaxFrameNumMinus4 + 4)));
        }
        // SH: frame_num
        writer.writeNBit(sliceHeader.frameNum, sps.log2MaxFrameNumMinus4 + 4);
        if (!sps.frameMbsOnlyFlag) {
            CAVLCWriter.writeBool(writer, sliceHeader.fieldPicFlag); // SH: field_pic_flag
            if (sliceHeader.fieldPicFlag) {
                CAVLCWriter.writeBool(writer, sliceHeader.bottomFieldFlag); // SH: bottom_field_flag
            }
        }
        if (idrSlice) {
            // SH: idr_pic_id
            CAVLCWriter.writeUE(writer, sliceHeader.idrPicId);
        }
        if (sps.picOrderCntType == 0) {
            if (sliceHeader.picOrderCntLsb > (1 << (sps.log2MaxPicOrderCntLsbMinus4 + 4))) {
                throw new IllegalArgumentException("pic_order_cnt_lsb > " + (1 << (sps.log2MaxPicOrderCntLsbMinus4 + 4)));
            }
            CAVLCWriter.writeU(writer, sliceHeader.picOrderCntLsb, sps.log2MaxPicOrderCntLsbMinus4 + 4);
            if (pps.picOrderPresentFlag && !sps.fieldPicFlag) {
                // SH: delta_pic_order_cnt_bottom
                CAVLCWriter.writeSE(writer, sliceHeader.deltaPicOrderCntBottom);
            }
        }
        if (sps.picOrderCntType == 1 && !sps.deltaPicOrderAlwaysZeroFlag) {
            // SH: delta_pic_order_cnt
            CAVLCWriter.writeSE(writer, sliceHeader.deltaPicOrderCnt[0]);
            if (pps.picOrderPresentFlag && !sps.fieldPicFlag)
                CAVLCWriter.writeSE(writer, sliceHeader.deltaPicOrderCnt[1]);
        }
        if (pps.redundantPicCntPresentFlag) {
            // SH: redundant_pic_cnt
            CAVLCWriter.writeUE(writer, sliceHeader.redundantPicCnt);
        }
        if (sliceHeader.sliceType == SliceType.B) {
            CAVLCWriter.writeBool(writer, sliceHeader.directSpatialMvPredFlag); // SH: direct_spatial_mv_pred_flag
        }
        if (sliceHeader.sliceType == SliceType.P || sliceHeader.sliceType == SliceType.SP
            || sliceHeader.sliceType == SliceType.B) {
            CAVLCWriter.writeBool(writer, sliceHeader.numRefIdxActiveOverrideFlag); // SH: num_ref_idx_active_override_flag
            if (sliceHeader.numRefIdxActiveOverrideFlag) {
                // SH: num_ref_idx_l0_active_minus1
                CAVLCWriter.writeUE(writer, sliceHeader.numRefIdxActiveMinus1[0]);
                if (sliceHeader.sliceType == SliceType.B) {
                    // SH: num_ref_idx_l1_active_minus1
                    CAVLCWriter.writeUE(writer, sliceHeader.numRefIdxActiveMinus1[1]);
                }
            }
        }
        writeRefPicListReordering(sliceHeader, writer);
        if ((pps.weightedPredFlag && (sliceHeader.sliceType == SliceType.P || sliceHeader.sliceType == SliceType.SP))
            || (pps.weightedBipredIdc == 1 && sliceHeader.sliceType == SliceType.B))
            writePredWeightTable(sliceHeader, writer);
        if (nalRefIdc != 0)
            writeDecRefPicMarking(sliceHeader, idrSlice, writer);
        if (pps.entropyCodingModeFlag && sliceHeader.sliceType.isInter()) {
            // SH: cabac_init_idc
            CAVLCWriter.writeUE(writer, sliceHeader.cabacInitIdc);
        }
        // SH: slice_qp_delta
        CAVLCWriter.writeSE(writer, sliceHeader.sliceQpDelta);
        if (sliceHeader.sliceType == SliceType.SP || sliceHeader.sliceType == SliceType.SI) {
            if (sliceHeader.sliceType == SliceType.SP) {
                CAVLCWriter.writeBool(writer, sliceHeader.spForSwitchFlag); // SH: sp_for_switch_flag
            }
            // SH: slice_qs_delta
            CAVLCWriter.writeSE(writer, sliceHeader.sliceQsDelta);
        }
        if (pps.deblockingFilterControlPresentFlag) {
            // SH: disable_deblocking_filter_idc
            CAVLCWriter.writeUE(writer, sliceHeader.disableDeblockingFilterIdc);
            if (sliceHeader.disableDeblockingFilterIdc != 1) {
                // SH: slice_alpha_c0_offset_div2
                CAVLCWriter.writeSE(writer, sliceHeader.sliceAlphaC0OffsetDiv2);
                // SH: slice_beta_offset_div2
                CAVLCWriter.writeSE(writer, sliceHeader.sliceBetaOffsetDiv2);
            }
        }
        if (pps.numSliceGroupsMinus1 > 0 && pps.sliceGroupMapType >= 3 && pps.sliceGroupMapType <= 5) {
            int len = (sps.picHeightInMapUnitsMinus1 + 1) * (sps.picWidthInMbsMinus1 + 1)
                / (pps.sliceGroupChangeRateMinus1 + 1);
            if (((sps.picHeightInMapUnitsMinus1 + 1) * (sps.picWidthInMbsMinus1 + 1))
                % (pps.sliceGroupChangeRateMinus1 + 1) > 0)
                len += 1;

            len = CeilLog2(len + 1);
            CAVLCWriter.writeU(writer, sliceHeader.sliceGroupChangeCycle, len);
        }

    }

    private static int CeilLog2(final int uiVal) {
        int uiTmp = uiVal - 1;
        int uiRet = 0;

        while (uiTmp != 0) {
            uiTmp >>= 1;
            uiRet++;
        }
        return uiRet;
    }

    private static void writeDecRefPicMarking(final SliceHeader sliceHeader, final boolean idrSlice, final BitWriter writer) {
        if (idrSlice) {
            final RefPicMarkingIDR drpmidr = sliceHeader.refPicMarkingIDR;
            CAVLCWriter.writeBool(writer, drpmidr.discardDecodedPics()); // SH: no_output_of_prior_pics_flag
            CAVLCWriter.writeBool(writer, drpmidr.useForlongTerm()); // SH: long_term_reference_flag
        } else {
            CAVLCWriter.writeBool(writer, sliceHeader.refPicMarkingNonIDR != null); // SH: adaptive_ref_pic_marking_mode_flag
            if (sliceHeader.refPicMarkingNonIDR != null) {
                final RefPicMarking drpmidr = sliceHeader.refPicMarkingNonIDR;
                final RefPicMarking.Instruction[] instructions = drpmidr.instructions();
                for (final RefPicMarking.Instruction mmop : instructions) {
                    switch (mmop.type()) {
                        case REMOVE_SHORT -> {
                            // SH: memory_management_control_operation
                            CAVLCWriter.writeUE(writer, 1);
                            // SH: difference_of_pic_nums_minus1
                            CAVLCWriter.writeUE(writer, mmop.arg1() - 1);
                        }
                        case REMOVE_LONG -> {
                            // SH: memory_management_control_operation
                            CAVLCWriter.writeUE(writer, 2);
                            // SH: long_term_pic_num
                            CAVLCWriter.writeUE(writer, mmop.arg1());
                        }
                        case CONVERT_INTO_LONG -> {
                            // SH: memory_management_control_operation
                            CAVLCWriter.writeUE(writer, 3);
                            // SH: difference_of_pic_nums_minus1
                            CAVLCWriter.writeUE(writer, mmop.arg1() - 1);
                            // SH: long_term_frame_idx
                            CAVLCWriter.writeUE(writer, mmop.arg2());
                        }
                        case TRUNK_LONG -> {
                            // SH: memory_management_control_operation
                            CAVLCWriter.writeUE(writer, 4);
                            // SH: max_long_term_frame_idx_plus1
                            CAVLCWriter.writeUE(writer, mmop.arg1() + 1);
                        }
                        case CLEAR -> CAVLCWriter.writeUE(writer, 5); // SH: memory_management_control_operation
                        case MARK_LONG -> {
                            // SH: memory_management_control_operation
                            CAVLCWriter.writeUE(writer, 6);
                            // SH: long_term_frame_idx
                            CAVLCWriter.writeUE(writer, mmop.arg1());
                        }
                    }
                }
                // SH: memory_management_control_operation
                CAVLCWriter.writeUE(writer, 0);
            }
        }
    }

    private static void writePredWeightTable(final SliceHeader sliceHeader, final BitWriter writer) {
        final SeqParameterSet sps = sliceHeader.sps;
        // SH: luma_log2_weight_denom
        CAVLCWriter.writeUE(writer, sliceHeader.predWeightTable.lumaLog2WeightDenom);
        if (sps.chromaFormatIdc != ColorSpace.MONO) {
            // SH: chroma_log2_weight_denom
            CAVLCWriter.writeUE(writer, sliceHeader.predWeightTable.chromaLog2WeightDenom);
        }

        writeOffsetWeight(sliceHeader, writer, 0);
        if (sliceHeader.sliceType == SliceType.B) {
            writeOffsetWeight(sliceHeader, writer, 1);
        }
    }

    private static void writeOffsetWeight(final SliceHeader sliceHeader, final BitWriter writer, final int list) {
        final SeqParameterSet sps = sliceHeader.sps;
        final int defaultLW = 1 << sliceHeader.predWeightTable.lumaLog2WeightDenom;
        final int defaultCW = 1 << sliceHeader.predWeightTable.chromaLog2WeightDenom;

        for (int i = 0; i < sliceHeader.predWeightTable.lumaWeight[list].length; i++) {
            final boolean flagLuma = sliceHeader.predWeightTable.lumaWeight[list][i] != defaultLW
                || sliceHeader.predWeightTable.lumaOffset[list][i] != 0;
            CAVLCWriter.writeBool(writer, flagLuma); // SH: luma_weight_l0_flag
            if (flagLuma) {
                // SH: luma_weight_l {list}
                CAVLCWriter.writeSE(writer, sliceHeader.predWeightTable.lumaWeight[list][i]);
                // SH: luma_offset_l {list}
                CAVLCWriter.writeSE(writer, sliceHeader.predWeightTable.lumaOffset[list][i]);
            }
            if (sps.chromaFormatIdc != ColorSpace.MONO) {
                final boolean flagChroma = sliceHeader.predWeightTable.chromaWeight[list][0][i] != defaultCW
                    || sliceHeader.predWeightTable.chromaOffset[list][0][i] != 0
                    || sliceHeader.predWeightTable.chromaWeight[list][1][i] != defaultCW
                    || sliceHeader.predWeightTable.chromaOffset[list][1][i] != 0;
                CAVLCWriter.writeBool(writer, flagChroma); // SH: chroma_weight_l0_flag
                if (flagChroma)
                    for (int j = 0; j < 2; j++) {
                        // SH: chroma_weight_l {list}
                        CAVLCWriter.writeSE(writer, sliceHeader.predWeightTable.chromaWeight[list][j][i]);
                        // SH: chroma_offset_l {list}
                        CAVLCWriter.writeSE(writer, sliceHeader.predWeightTable.chromaOffset[list][j][i]);
                    }
            }
        }
    }

    private static void writeRefPicListReordering(final SliceHeader sliceHeader, final BitWriter writer) {
        if (sliceHeader.sliceType.isInter()) {
            final boolean l0ReorderingPresent = sliceHeader.refPicReordering != null
                && sliceHeader.refPicReordering[0] != null;
            CAVLCWriter.writeBool(writer, l0ReorderingPresent); // SH: ref_pic_list_reordering_flag_l0
            if (l0ReorderingPresent)
                writeReorderingList(sliceHeader.refPicReordering[0], writer);
        }
        if (sliceHeader.sliceType == SliceType.B) {
            final boolean l1ReorderingPresent = sliceHeader.refPicReordering != null
                && sliceHeader.refPicReordering[1] != null;
            CAVLCWriter.writeBool(writer, l1ReorderingPresent); // SH: ref_pic_list_reordering_flag_l1
            if (l1ReorderingPresent)
                writeReorderingList(sliceHeader.refPicReordering[1], writer);
        }
    }

    private static void writeReorderingList(final int[][] reordering, final BitWriter writer) {
        if (reordering == null)
            return;

        for (int i = 0; i < reordering[0].length; i++) {
            // SH: reordering_of_pic_nums_idc
            CAVLCWriter.writeUE(writer, reordering[0][i]);
            // SH: abs_diff_pic_num_minus1
            CAVLCWriter.writeUE(writer, reordering[1][i]);
        }
        // SH: reordering_of_pic_nums_idc
        CAVLCWriter.writeUE(writer, 3);
    }
}
