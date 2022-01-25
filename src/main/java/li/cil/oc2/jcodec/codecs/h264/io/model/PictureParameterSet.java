/* SPDX-License-Identifier: BSD 2-Clause "Simplified" License */

package li.cil.oc2.jcodec.codecs.h264.io.model;

import li.cil.oc2.jcodec.codecs.h264.decode.CAVLCReader;
import li.cil.oc2.jcodec.common.io.BitReader;
import li.cil.oc2.jcodec.common.io.BitWriter;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static li.cil.oc2.jcodec.codecs.h264.io.write.CAVLCWriter.*;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * <p>
 * Picture Parameter Set entity of H264 bitstream
 * <p>
 * capable to serialize / deserialize with CAVLC bitstream
 *
 * @author The JCodec project
 */
public final class PictureParameterSet {
    public static class PPSExt {
        public boolean transform8x8ModeFlag;
        public int[][] scalingMatrix;
        public int secondChromaQpIndexOffset;
    }

    // entropy_coding_mode_flag
    public boolean entropyCodingModeFlag;
    // num_ref_idx_active_minus1
    public int[] numRefIdxActiveMinus1 = new int[2];
    // slice_group_change_rate_minus1
    public int sliceGroupChangeRateMinus1;
    // pic_parameter_set_id
    public int picParameterSetId;
    // seq_parameter_set_id
    public int seqParameterSetId;
    // pic_order_present_flag
    public boolean picOrderPresentFlag;
    // num_slice_groups_minus1
    public int numSliceGroupsMinus1;
    // slice_group_map_type
    public int sliceGroupMapType;
    // weighted_pred_flag
    public boolean weightedPredFlag;
    // weighted_bipred_idc
    public int weightedBipredIdc;
    // pic_init_qp_minus26
    public int picInitQpMinus26;
    // pic_init_qs_minus26
    public int picInitQsMinus26;
    // chroma_qp_index_offset
    public int chromaQpIndexOffset;
    // deblocking_filter_control_present_flag
    public boolean deblockingFilterControlPresentFlag;
    // constrained_intra_pred_flag
    public boolean constrainedIntraPredFlag;
    // redundant_pic_cnt_present_flag
    public boolean redundantPicCntPresentFlag;
    // top_left
    public int[] topLeft;
    // bottom_right
    public int[] bottomRight;
    // run_length_minus1
    public int[] runLengthMinus1;
    // slice_group_change_direction_flag
    public boolean sliceGroupChangeDirectionFlag;
    // slice_group_id
    public int[] sliceGroupId;
    public PPSExt extended;

    public static PictureParameterSet read(final ByteBuffer is) {
        final BitReader _in = BitReader.createBitReader(is);
        final PictureParameterSet pps = new PictureParameterSet();

        pps.picParameterSetId = CAVLCReader.readUE(_in); // PPS: pic_parameter_set_id
        pps.seqParameterSetId = CAVLCReader.readUE(_in); // PPS: seq_parameter_set_id
        pps.entropyCodingModeFlag = CAVLCReader.readBool(_in); // PPS: entropy_coding_mode_flag
        pps.picOrderPresentFlag = CAVLCReader.readBool(_in); // PPS: pic_order_present_flag
        pps.numSliceGroupsMinus1 = CAVLCReader.readUE(_in); // PPS: num_slice_groups_minus1
        if (pps.numSliceGroupsMinus1 > 0) {
            pps.sliceGroupMapType = CAVLCReader.readUE(_in); // PPS: slice_group_map_type
            pps.topLeft = new int[pps.numSliceGroupsMinus1 + 1];
            pps.bottomRight = new int[pps.numSliceGroupsMinus1 + 1];
            pps.runLengthMinus1 = new int[pps.numSliceGroupsMinus1 + 1];
            if (pps.sliceGroupMapType == 0)
                for (int iGroup = 0; iGroup <= pps.numSliceGroupsMinus1; iGroup++)
                    pps.runLengthMinus1[iGroup] = CAVLCReader.readUE(_in); // PPS: run_length_minus1
            else if (pps.sliceGroupMapType == 2)
                for (int iGroup = 0; iGroup < pps.numSliceGroupsMinus1; iGroup++) {
                    pps.topLeft[iGroup] = CAVLCReader.readUE(_in); // PPS: top_left
                    pps.bottomRight[iGroup] = CAVLCReader.readUE(_in); // PPS: bottom_right
                }
            else if (pps.sliceGroupMapType == 3 || pps.sliceGroupMapType == 4 || pps.sliceGroupMapType == 5) {
                pps.sliceGroupChangeDirectionFlag = CAVLCReader.readBool(_in); // PPS: slice_group_change_direction_flag
                pps.sliceGroupChangeRateMinus1 = CAVLCReader.readUE(_in); // PPS: slice_group_change_rate_minus1
            } else if (pps.sliceGroupMapType == 6) {
                final int NumberBitsPerSliceGroupId;
                if (pps.numSliceGroupsMinus1 + 1 > 4)
                    NumberBitsPerSliceGroupId = 3;
                else if (pps.numSliceGroupsMinus1 + 1 > 2)
                    NumberBitsPerSliceGroupId = 2;
                else
                    NumberBitsPerSliceGroupId = 1;
                final int pic_size_in_map_units_minus1 = CAVLCReader.readUE(_in); // PPS: pic_size_in_map_units_minus1
                pps.sliceGroupId = new int[pic_size_in_map_units_minus1 + 1];
                for (int i = 0; i <= pic_size_in_map_units_minus1; i++) {
                    pps.sliceGroupId[i] = CAVLCReader.readU(_in, NumberBitsPerSliceGroupId); // PPS: slice_group_id [i] f
                }
            }
        }
        pps.numRefIdxActiveMinus1 = new int[]{
            CAVLCReader.readUE(_in), // PPS: num_ref_idx_l0_active_minus1
            CAVLCReader.readUE(_in) // PPS: num_ref_idx_l1_active_minus1
        };
        pps.weightedPredFlag = CAVLCReader.readBool(_in); // PPS: weighted_pred_flag
        pps.weightedBipredIdc = CAVLCReader.readNBit(_in, 2); // PPS: weighted_bipred_idc
        pps.picInitQpMinus26 = CAVLCReader.readSE(_in); // PPS: pic_init_qp_minus26
        pps.picInitQsMinus26 = CAVLCReader.readSE(_in); // PPS: pic_init_qs_minus26
        pps.chromaQpIndexOffset = CAVLCReader.readSE(_in); // PPS: chroma_qp_index_offset
        pps.deblockingFilterControlPresentFlag = CAVLCReader.readBool(_in); // PPS: deblocking_filter_control_present_flag
        pps.constrainedIntraPredFlag = CAVLCReader.readBool(_in); // PPS: constrained_intra_pred_flag
        pps.redundantPicCntPresentFlag = CAVLCReader.readBool(_in); // PPS: redundant_pic_cnt_present_flag
        if (CAVLCReader.moreRBSPData(_in)) {
            pps.extended = new PPSExt();
            pps.extended.transform8x8ModeFlag = CAVLCReader.readBool(_in); // PPS: transform_8x8_mode_flag
            final boolean pic_scaling_matrix_present_flag = CAVLCReader.readBool(_in); // PPS: pic_scaling_matrix_present_flag
            if (pic_scaling_matrix_present_flag) {
                pps.extended.scalingMatrix = new int[8][];
                for (int i = 0; i < 6 + 2 * (pps.extended.transform8x8ModeFlag ? 1 : 0); i++) {
                    final int scalingListSize = i < 6 ? 16 : 64;
                    if (CAVLCReader.readBool(_in)) { // PPS: pic_scaling_list_present_flag
                        pps.extended.scalingMatrix[i] = SeqParameterSet.readScalingList(_in, scalingListSize);
                    }
                }
            }
            pps.extended.secondChromaQpIndexOffset = CAVLCReader.readSE(_in); // PPS: second_chroma_qp_index_offset
        }

        return pps;
    }

    public void write(final ByteBuffer out) {
        final BitWriter writer = new BitWriter(out);

        // PPS: pic_parameter_set_id
        writeUE(writer, picParameterSetId);
        // PPS: seq_parameter_set_id
        writeUE(writer, seqParameterSetId);
        writeBool(writer, entropyCodingModeFlag); // PPS: entropy_coding_mode_flag
        writeBool(writer, picOrderPresentFlag); // PPS: pic_order_present_flag
        // PPS: num_slice_groups_minus1
        writeUE(writer, numSliceGroupsMinus1);
        if (numSliceGroupsMinus1 > 0) {
            // PPS: slice_group_map_type
            writeUE(writer, sliceGroupMapType);
            if (sliceGroupMapType == 0) {
                for (int iGroup = 0; iGroup <= numSliceGroupsMinus1; iGroup++) {
                    // PPS:
                    writeUE(writer, runLengthMinus1[iGroup]);
                }
            } else if (sliceGroupMapType == 2) {
                for (int iGroup = 0; iGroup < numSliceGroupsMinus1; iGroup++) {
                    // PPS:
                    writeUE(writer, topLeft[iGroup]);
                    // PPS:
                    writeUE(writer, bottomRight[iGroup]);
                }
            } else if (sliceGroupMapType == 3 || sliceGroupMapType == 4 || sliceGroupMapType == 5) {
                writeBool(writer, sliceGroupChangeDirectionFlag); // PPS: slice_group_change_direction_flag
                // PPS: slice_group_change_rate_minus1
                writeUE(writer, sliceGroupChangeRateMinus1);
            } else if (sliceGroupMapType == 6) {
                final int NumberBitsPerSliceGroupId;
                if (numSliceGroupsMinus1 + 1 > 4)
                    NumberBitsPerSliceGroupId = 3;
                else if (numSliceGroupsMinus1 + 1 > 2)
                    NumberBitsPerSliceGroupId = 2;
                else
                    NumberBitsPerSliceGroupId = 1;
                // PPS:
                writeUE(writer, sliceGroupId.length);
                for (int i = 0; i <= sliceGroupId.length; i++) {
                    writeU(writer, sliceGroupId[i], NumberBitsPerSliceGroupId);
                }
            }
        }
        // PPS: num_ref_idx_l0_active_minus1
        writeUE(writer, numRefIdxActiveMinus1[0]);
        // PPS: num_ref_idx_l1_active_minus1
        writeUE(writer, numRefIdxActiveMinus1[1]);
        writeBool(writer, weightedPredFlag); // PPS: weighted_pred_flag
        writeNBit(writer, weightedBipredIdc, 2, ""); // PPS: weighted_bipred_idc
        // PPS: pic_init_qp_minus26
        writeSE(writer, picInitQpMinus26);
        // PPS: pic_init_qs_minus26
        writeSE(writer, picInitQsMinus26);
        // PPS: chroma_qp_index_offset
        writeSE(writer, chromaQpIndexOffset);
        writeBool(writer, deblockingFilterControlPresentFlag); // PPS: deblocking_filter_control_present_flag
        writeBool(writer, constrainedIntraPredFlag); // PPS: constrained_intra_pred_flag
        writeBool(writer, redundantPicCntPresentFlag); // PPS: redundant_pic_cnt_present_flag
        if (extended != null) {
            writeBool(writer, extended.transform8x8ModeFlag); // PPS: transform_8x8_mode_flag
            writeBool(writer, extended.scalingMatrix != null); // PPS: scalindMatrix
            if (extended.scalingMatrix != null) {
                for (int i = 0; i < 6 + 2 * (extended.transform8x8ModeFlag ? 1 : 0); i++) {
                    writeBool(writer, extended.scalingMatrix[i] != null); // PPS:
                    if (extended.scalingMatrix[i] != null) {
                        SeqParameterSet.writeScalingList(writer, extended.scalingMatrix, i);
                    }
                }
            }
            // PPS:
            writeSE(writer, extended.secondChromaQpIndexOffset);
        }

        writeTrailingBits(writer);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(bottomRight);
        result = prime * result + chromaQpIndexOffset;
        result = prime * result + (constrainedIntraPredFlag ? 1231 : 1237);
        result = prime * result + (deblockingFilterControlPresentFlag ? 1231 : 1237);
        result = prime * result + (entropyCodingModeFlag ? 1231 : 1237);
        result = prime * result + ((extended == null) ? 0 : extended.hashCode());
        result = prime * result + numRefIdxActiveMinus1[0];
        result = prime * result + numRefIdxActiveMinus1[1];
        result = prime * result + numSliceGroupsMinus1;
        result = prime * result + picInitQpMinus26;
        result = prime * result + picInitQsMinus26;
        result = prime * result + (picOrderPresentFlag ? 1231 : 1237);
        result = prime * result + picParameterSetId;
        result = prime * result + (redundantPicCntPresentFlag ? 1231 : 1237);
        result = prime * result + Arrays.hashCode(runLengthMinus1);
        result = prime * result + seqParameterSetId;
        result = prime * result + (sliceGroupChangeDirectionFlag ? 1231 : 1237);
        result = prime * result + sliceGroupChangeRateMinus1;
        result = prime * result + Arrays.hashCode(sliceGroupId);
        result = prime * result + sliceGroupMapType;
        result = prime * result + Arrays.hashCode(topLeft);
        result = prime * result + weightedBipredIdc;
        result = prime * result + (weightedPredFlag ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final PictureParameterSet other = (PictureParameterSet) obj;
        if (!Arrays.equals(bottomRight, other.bottomRight))
            return false;
        if (chromaQpIndexOffset != other.chromaQpIndexOffset)
            return false;
        if (constrainedIntraPredFlag != other.constrainedIntraPredFlag)
            return false;
        if (deblockingFilterControlPresentFlag != other.deblockingFilterControlPresentFlag)
            return false;
        if (entropyCodingModeFlag != other.entropyCodingModeFlag)
            return false;
        if (extended == null) {
            if (other.extended != null)
                return false;
        } else if (!extended.equals(other.extended))
            return false;
        if (numRefIdxActiveMinus1[0] != other.numRefIdxActiveMinus1[0])
            return false;
        if (numRefIdxActiveMinus1[1] != other.numRefIdxActiveMinus1[1])
            return false;
        if (numSliceGroupsMinus1 != other.numSliceGroupsMinus1)
            return false;
        if (picInitQpMinus26 != other.picInitQpMinus26)
            return false;
        if (picInitQsMinus26 != other.picInitQsMinus26)
            return false;
        if (picOrderPresentFlag != other.picOrderPresentFlag)
            return false;
        if (picParameterSetId != other.picParameterSetId)
            return false;
        if (redundantPicCntPresentFlag != other.redundantPicCntPresentFlag)
            return false;
        if (!Arrays.equals(runLengthMinus1, other.runLengthMinus1))
            return false;
        if (seqParameterSetId != other.seqParameterSetId)
            return false;
        if (sliceGroupChangeDirectionFlag != other.sliceGroupChangeDirectionFlag)
            return false;
        if (sliceGroupChangeRateMinus1 != other.sliceGroupChangeRateMinus1)
            return false;
        if (!Arrays.equals(sliceGroupId, other.sliceGroupId))
            return false;
        if (sliceGroupMapType != other.sliceGroupMapType)
            return false;
        if (!Arrays.equals(topLeft, other.topLeft))
            return false;
        if (weightedBipredIdc != other.weightedBipredIdc)
            return false;
        if (weightedPredFlag != other.weightedPredFlag)
            return false;
        return true;
    }

    public PictureParameterSet copy() {
        final ByteBuffer buf = ByteBuffer.allocate(2048);
        write(buf);
        buf.flip();
        return read(buf);
    }
}
