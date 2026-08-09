/* SPDX-License-Identifier: BSD 2-Clause "Simplified" License */

package li.cil.oc2.jcodec.codecs.h264.decode;

import li.cil.oc2.jcodec.codecs.h264.io.model.*;
import li.cil.oc2.jcodec.codecs.h264.io.model.RefPicMarking.InstrType;
import li.cil.oc2.jcodec.codecs.h264.io.model.RefPicMarking.Instruction;
import li.cil.oc2.jcodec.common.IntArrayList;
import li.cil.oc2.jcodec.common.io.BitReader;
import li.cil.oc2.jcodec.common.model.ColorSpace;

import java.util.ArrayList;

import static li.cil.oc2.jcodec.codecs.h264.io.model.SeqParameterSet.getPicHeightInMbs;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * <p>
 * Reads header of the coded slice
 *
 * @author The JCodec project
 */
public final class SliceHeaderReader {
    private SliceHeaderReader() {
    }

    public static SliceHeader readPart1(final BitReader in) {
        final SliceHeader sh = new SliceHeader();
        sh.firstMbInSlice = CAVLCReader.readUE(in); // SH: first_mb_in_slice
        final int shType = CAVLCReader.readUE(in); // SH: slice_type
        sh.sliceType = SliceType.fromValue(shType % 5);
        sh.sliceTypeRestr = (shType / 5) > 0;
        sh.picParameterSetId = CAVLCReader.readUE(in); // SH: pic_parameter_set_id

        return sh;
    }

    public static void readPart2(final SliceHeader sh, final NALUnit nalUnit,
                                 final SeqParameterSet sps, final PictureParameterSet pps,
                                 final BitReader in) {
        sh.pps = pps;
        sh.sps = sps;

        sh.frameNum = CAVLCReader.readU(in, sps.log2MaxFrameNumMinus4 + 4); // SH: frame_num
        if (!sps.frameMbsOnlyFlag) {
            sh.fieldPicFlag = CAVLCReader.readBool(in); // SH: field_pic_flag
            if (sh.fieldPicFlag) {
                sh.bottomFieldFlag = CAVLCReader.readBool(in); // SH: bottom_field_flag
            }
        }
        if (nalUnit.type == NALUnitType.IDR_SLICE) {
            sh.idrPicId = CAVLCReader.readUE(in); // SH: idr_pic_id
        }
        if (sps.picOrderCntType == 0) {
            sh.picOrderCntLsb = CAVLCReader.readU(in, sps.log2MaxPicOrderCntLsbMinus4 + 4); // SH: pic_order_cnt_lsb
            if (pps.picOrderPresentFlag && !sps.fieldPicFlag) {
                sh.deltaPicOrderCntBottom = CAVLCReader.readSE(in); // SH: delta_pic_order_cnt_bottom
            }
        }
        sh.deltaPicOrderCnt = new int[2];
        if (sps.picOrderCntType == 1 && !sps.deltaPicOrderAlwaysZeroFlag) {
            sh.deltaPicOrderCnt[0] = CAVLCReader.readSE(in); // SH: delta_pic_order_cnt[0]
            if (pps.picOrderPresentFlag && !sps.fieldPicFlag)
                sh.deltaPicOrderCnt[1] = CAVLCReader.readSE(in); // SH: delta_pic_order_cnt[1]
        }
        if (pps.redundantPicCntPresentFlag) {
            sh.redundantPicCnt = CAVLCReader.readUE(in); // SH: redundant_pic_cnt
        }
        if (sh.sliceType == SliceType.B) {
            sh.directSpatialMvPredFlag = CAVLCReader.readBool(in); // SH: direct_spatial_mv_pred_flag
        }
        if (sh.sliceType == SliceType.P || sh.sliceType == SliceType.SP || sh.sliceType == SliceType.B) {
            sh.numRefIdxActiveOverrideFlag = CAVLCReader.readBool(in); // SH: num_ref_idx_active_override_flag
            if (sh.numRefIdxActiveOverrideFlag) {
                sh.numRefIdxActiveMinus1[0] = CAVLCReader.readUE(in); // SH: num_ref_idx_l0_active_minus1
                if (sh.sliceType == SliceType.B) {
                    sh.numRefIdxActiveMinus1[1] = CAVLCReader.readUE(in); // SH: num_ref_idx_l1_active_minus1
                }
            }
        }
        readRefPicListReordering(sh, in);
        if ((pps.weightedPredFlag && (sh.sliceType == SliceType.P || sh.sliceType == SliceType.SP))
            || (pps.weightedBipredIdc == 1 && sh.sliceType == SliceType.B))
            readPredWeightTable(sps, pps, sh, in);
        if (nalUnit.nal_ref_idc != 0)
            readDecoderPicMarking(nalUnit, sh, in);
        if (pps.entropyCodingModeFlag && sh.sliceType.isInter()) {
            sh.cabacInitIdc = CAVLCReader.readUE(in); // SH: cabac_init_idc
        }
        sh.sliceQpDelta = CAVLCReader.readSE(in); // SH: slice_qp_delta
        if (sh.sliceType == SliceType.SP || sh.sliceType == SliceType.SI) {
            if (sh.sliceType == SliceType.SP) {
                sh.spForSwitchFlag = CAVLCReader.readBool(in); // SH: sp_for_switch_flag
            }
            sh.sliceQsDelta = CAVLCReader.readSE(in); // SH: slice_qs_delta
        }
        if (pps.deblockingFilterControlPresentFlag) {
            sh.disableDeblockingFilterIdc = CAVLCReader.readUE(in); // SH: disable_deblocking_filter_idc
            if (sh.disableDeblockingFilterIdc != 1) {
                sh.sliceAlphaC0OffsetDiv2 = CAVLCReader.readSE(in); // SH: slice_alpha_c0_offset_div2
                sh.sliceBetaOffsetDiv2 = CAVLCReader.readSE(in); // SH: slice_beta_offset_div2
            }
        }
        if (pps.numSliceGroupsMinus1 > 0 && pps.sliceGroupMapType >= 3 && pps.sliceGroupMapType <= 5) {
            int len = getPicHeightInMbs(sps) * (sps.picWidthInMbsMinus1 + 1) / (pps.sliceGroupChangeRateMinus1 + 1);
            if ((getPicHeightInMbs(sps) * (sps.picWidthInMbsMinus1 + 1)) % (pps.sliceGroupChangeRateMinus1 + 1) > 0)
                len += 1;

            len = CeilLog2(len + 1);
            sh.sliceGroupChangeCycle = CAVLCReader.readU(in, len); // SH: slice_group_change_cycle
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

    private static void readDecoderPicMarking(final NALUnit nalUnit, final SliceHeader sh, final BitReader _in) {
        if (nalUnit.type == NALUnitType.IDR_SLICE) {
            final boolean noOutputOfPriorPicsFlag = CAVLCReader.readBool(_in); // SH: no_output_of_prior_pics_flag
            final boolean longTermReferenceFlag = CAVLCReader.readBool(_in); // SH: long_term_reference_flag
            sh.refPicMarkingIDR = new RefPicMarkingIDR(noOutputOfPriorPicsFlag, longTermReferenceFlag);
        } else {
            final boolean adaptiveRefPicMarkingModeFlag = CAVLCReader.readBool(_in); // SH: adaptive_ref_pic_marking_mode_flag
            if (adaptiveRefPicMarkingModeFlag) {
                final ArrayList<Instruction> mmops = new ArrayList<>();
                int memoryManagementControlOperation;
                do {
                    memoryManagementControlOperation = CAVLCReader.readUE(_in); // SH: memory_management_control_operation

                    final Instruction instr = switch (memoryManagementControlOperation) {
                        case 1 -> new Instruction(InstrType.REMOVE_SHORT, CAVLCReader.readUE(_in) + 1, 0); // SH: difference_of_pic_nums_minus1
                        case 2 -> new Instruction(InstrType.REMOVE_LONG, CAVLCReader.readUE(_in), 0); // SH: long_term_pic_num
                        case 3 -> new Instruction(InstrType.CONVERT_INTO_LONG,
                            CAVLCReader.readUE(_in) + 1, // SH: difference_of_pic_nums_minus1
                            CAVLCReader.readUE(_in)); // SH: long_term_frame_idx
                        case 4 -> new Instruction(InstrType.TRUNK_LONG, CAVLCReader.readUE(_in) - 1, 0); // SH: max_long_term_frame_idx_plus1
                        case 5 -> new Instruction(InstrType.CLEAR, 0, 0);
                        case 6 -> new Instruction(InstrType.MARK_LONG, CAVLCReader.readUE(_in), 0); // SH: long_term_frame_idx
                        default -> null;
                    };

                    if (instr != null)
                        mmops.add(instr);
                } while (memoryManagementControlOperation != 0);
                sh.refPicMarkingNonIDR = new RefPicMarking(mmops.toArray(new Instruction[]{ }));
            }
        }
    }

    private static void readPredWeightTable(final SeqParameterSet sps, final PictureParameterSet pps, final SliceHeader sh, final BitReader _in) {
        sh.predWeightTable = new PredictionWeightTable();
        final int[] numRefsMinus1 = sh.numRefIdxActiveOverrideFlag ? sh.numRefIdxActiveMinus1 : pps.numRefIdxActiveMinus1;
        final int[] nr = new int[]{numRefsMinus1[0] + 1, numRefsMinus1[1] + 1};

        sh.predWeightTable.lumaLog2WeightDenom = CAVLCReader.readUE(_in); // SH: luma_log2_weight_denom
        if (sps.chromaFormatIdc != ColorSpace.MONO) {
            sh.predWeightTable.chromaLog2WeightDenom = CAVLCReader.readUE(_in); // SH: chroma_log2_weight_denom
        }
        final int defaultLW = 1 << sh.predWeightTable.lumaLog2WeightDenom;
        final int defaultCW = 1 << sh.predWeightTable.chromaLog2WeightDenom;

        for (int list = 0; list < 2; list++) {
            sh.predWeightTable.lumaWeight[list] = new int[nr[list]];
            sh.predWeightTable.lumaOffset[list] = new int[nr[list]];
            sh.predWeightTable.chromaWeight[list] = new int[2][nr[list]];
            sh.predWeightTable.chromaOffset[list] = new int[2][nr[list]];
            for (int i = 0; i < nr[list]; i++) {
                sh.predWeightTable.lumaWeight[list][i] = defaultLW;
                sh.predWeightTable.lumaOffset[list][i] = 0;
                sh.predWeightTable.chromaWeight[list][0][i] = defaultCW;
                sh.predWeightTable.chromaOffset[list][0][i] = 0;
                sh.predWeightTable.chromaWeight[list][1][i] = defaultCW;
                sh.predWeightTable.chromaOffset[list][1][i] = 0;
            }
        }

        readWeightOffset(sps, sh, _in, nr, 0);
        if (sh.sliceType == SliceType.B) {
            readWeightOffset(sps, sh, _in, nr, 1);
        }
    }

    private static void readWeightOffset(final SeqParameterSet sps, final SliceHeader sh, final BitReader _in,
                                         final int[] numRefs, final int list) {
        for (int i = 0; i < numRefs[list]; i++) {
            final boolean lumaWeightL0Flag = CAVLCReader.readBool(_in); // SH: luma_weight_l0_flag
            if (lumaWeightL0Flag) {
                sh.predWeightTable.lumaWeight[list][i] = CAVLCReader.readSE(_in); // SH: weight
                sh.predWeightTable.lumaOffset[list][i] = CAVLCReader.readSE(_in); // SH: offset
            }
            if (sps.chromaFormatIdc != ColorSpace.MONO) {
                final boolean chromaWeightL0Flag = CAVLCReader.readBool(_in); // SH: chroma_weight_l0_flag
                if (chromaWeightL0Flag) {
                    sh.predWeightTable.chromaWeight[list][0][i] = CAVLCReader.readSE(_in); // SH: weight
                    sh.predWeightTable.chromaOffset[list][0][i] = CAVLCReader.readSE(_in); // SH: offset
                    sh.predWeightTable.chromaWeight[list][1][i] = CAVLCReader.readSE(_in); // SH: weight
                    sh.predWeightTable.chromaOffset[list][1][i] = CAVLCReader.readSE(_in); // SH: offset
                }
            }
        }
    }

    private static void readRefPicListReordering(final SliceHeader sh, final BitReader _in) {
        sh.refPicReordering = new int[2][][];
        if (sh.sliceType.isInter()) {
            final boolean refPicListReorderingFlagL0 = CAVLCReader.readBool(_in); // SH: ref_pic_list_reordering_flag_l0
            if (refPicListReorderingFlagL0) {
                sh.refPicReordering[0] = readReorderingEntries(_in);
            }
        }
        if (sh.sliceType == SliceType.B) {
            final boolean refPicListReorderingFlagL1 = CAVLCReader.readBool(_in); // SH: ref_pic_list_reordering_flag_l1
            if (refPicListReorderingFlagL1) {
                sh.refPicReordering[1] = readReorderingEntries(_in);
            }
        }
    }

    private static int[][] readReorderingEntries(final BitReader _in) {
        final IntArrayList ops = IntArrayList.createIntArrayList();
        final IntArrayList args = IntArrayList.createIntArrayList();
        do {
            final int idc = CAVLCReader.readUE(_in); // SH: reordering_of_pic_nums_idc
            if (idc == 3)
                break;
            ops.add(idc);
            args.add(CAVLCReader.readUE(_in)); // SH: abs_diff_pic_num_minus1
        } while (true);
        return new int[][]{ops.toArray(), args.toArray()};
    }
}
