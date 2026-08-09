/* SPDX-License-Identifier: BSD 2-Clause "Simplified" License */

package li.cil.oc2.jcodec.codecs.h264.io.model;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * <p>
 * A script of instructions applied to reference picture list
 *
 * @author The JCodec project
 */
public record RefPicMarking(Instruction[] instructions) {
    public enum InstrType {
        REMOVE_SHORT, REMOVE_LONG, CONVERT_INTO_LONG, TRUNK_LONG, CLEAR, MARK_LONG
    }

    public record Instruction(InstrType type, int arg1, int arg2) { }
}
