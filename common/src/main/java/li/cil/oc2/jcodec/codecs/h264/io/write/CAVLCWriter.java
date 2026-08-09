/* SPDX-License-Identifier: BSD 2-Clause "Simplified" License */

package li.cil.oc2.jcodec.codecs.h264.io.write;

import li.cil.oc2.jcodec.common.io.BitWriter;
import li.cil.oc2.jcodec.common.tools.MathUtil;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * <p>
 * A class responsible for outputting exp-Golomb values into binary stream
 *
 * @author The JCodec project
 */
public final class CAVLCWriter {
    private CAVLCWriter() {
    }

    public static void writeUE(final BitWriter out, final int value) {
        int bits = 0;
        int cumul = 0;
        for (int i = 0; i < 15; i++) {
            if (value < cumul + (1 << i)) {
                bits = i;
                break;
            }
            cumul += (1 << i);
        }
        out.writeNBit(0, bits);
        out.write1Bit(1);
        out.writeNBit(value - cumul, bits);
    }

    public static void writeSE(final BitWriter out, final int value) {
        writeUE(out, MathUtil.golomb(value));
    }

    public static void writeTE(final BitWriter out, final int value, final int max) {
        if (max > 1)
            writeUE(out, value);
        else
            out.write1Bit(~value & 0x1);
    }

    public static void writeBool(final BitWriter out, final boolean value) {
        out.write1Bit(value ? 1 : 0);
    }

    public static void writeU(final BitWriter out, final int i, final int n) {
        out.writeNBit(i, n);
    }

    public static void writeNBit(final BitWriter out, final long value, final int n, final String message) {
        for (int i = 0; i < n; i++) {
            out.write1Bit((int) (value >> (n - i - 1)) & 0x1);
        }
    }

    public static void writeTrailingBits(final BitWriter out) {
        out.write1Bit(1);
        out.flush();
    }
}
