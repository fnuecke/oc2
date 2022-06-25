/* SPDX-License-Identifier: BSD 2-Clause "Simplified" License */

package li.cil.oc2.jcodec.codecs.h264;

import li.cil.oc2.jcodec.codecs.h264.io.model.NALUnit;
import li.cil.oc2.jcodec.codecs.h264.io.model.RefPicMarking.InstrType;
import li.cil.oc2.jcodec.codecs.h264.io.model.RefPicMarking.Instruction;
import li.cil.oc2.jcodec.codecs.h264.io.model.SliceHeader;

import static li.cil.oc2.jcodec.codecs.h264.io.model.NALUnitType.IDR_SLICE;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * <p>
 * POC ( Picture Order Count ) manager
 * <p>
 * Picture Order Count is used to represent an order of picture in a GOP ( Group
 * of Pictures ) this is needed to correctly reorder and B-framed GOPs. POC is
 * also used when building lists of reference pictures ( see 8.2.4.2 ).
 * <p>
 * There are 3 possible ways of assigning POC to decoded pictures:
 * <p>
 * - Explicit, i.e. POC is directly specified in a slice header in form <POC
 * Pred> + <POC Dec>. <POC Pred> is a significant part of POC ( see 8.2.1.1 ). -
 * Frame based type 1 ( see 8.2.1.2 ). - Frame based type 2 ( see 8.2.1.3 ).
 *
 * @author The JCodec project
 */
public final class POCManager {
    private int prevPOCMsb;
    private int prevPOCLsb;

    public int calcPOC(final SliceHeader firstSliceHeader, final NALUnit firstNu) {
        return switch (firstSliceHeader.sps.picOrderCntType) {
            case 0 -> calcPOC0(firstSliceHeader, firstNu);
            case 1 -> calcPOC1(firstSliceHeader);
            case 2 -> calcPOC2(firstSliceHeader);
            default -> throw new RuntimeException("Invalid POC");
        };
    }

    private int calcPOC2(final SliceHeader firstSliceHeader) {
        return firstSliceHeader.frameNum << 1;
    }

    private int calcPOC1(final SliceHeader firstSliceHeader) {
        return firstSliceHeader.frameNum << 1;
    }

    private int calcPOC0(final SliceHeader firstSliceHeader, final NALUnit firstNu) {
        if (firstNu.type == IDR_SLICE) {
            prevPOCMsb = prevPOCLsb = 0;
        }
        final int maxPOCLsbDiv2 = 1 << (firstSliceHeader.sps.log2MaxPicOrderCntLsbMinus4 + 3);
        final int maxPOCLsb = maxPOCLsbDiv2 << 1;
        final int POCLsb = firstSliceHeader.picOrderCntLsb;

        final int POCMsb;
        final int POC;
        if ((POCLsb < prevPOCLsb) && ((prevPOCLsb - POCLsb) >= maxPOCLsbDiv2))
            POCMsb = prevPOCMsb + maxPOCLsb;
        else if ((POCLsb > prevPOCLsb) && ((POCLsb - prevPOCLsb) > maxPOCLsbDiv2))
            POCMsb = prevPOCMsb - maxPOCLsb;
        else
            POCMsb = prevPOCMsb;

        POC = POCMsb + POCLsb;

        if (firstNu.nal_ref_idc > 0) {
            if (hasMMCO5(firstSliceHeader, firstNu)) {
                prevPOCMsb = 0;
                prevPOCLsb = POC;
            } else {
                prevPOCMsb = POCMsb;
                prevPOCLsb = POCLsb;
            }
        }

        return POC;
    }

    private boolean hasMMCO5(final SliceHeader firstSliceHeader, final NALUnit firstNu) {
        if (firstNu.type != IDR_SLICE && firstSliceHeader.refPicMarkingNonIDR != null) {
            final Instruction[] instructions = firstSliceHeader.refPicMarkingNonIDR.instructions();
            for (final Instruction instruction : instructions) {
                if (instruction.type() == InstrType.CLEAR)
                    return true;
            }
        }
        return false;
    }
}
