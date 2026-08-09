/* SPDX-License-Identifier: BSD 2-Clause "Simplified" License */

package li.cil.oc2.jcodec.codecs.h264.decode;

import li.cil.oc2.jcodec.codecs.h264.H264Utils;
import li.cil.oc2.jcodec.common.io.BitReader;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
public final class CAVLCReader {
    private CAVLCReader() {
    }

    public static int readNBit(final BitReader bits, final int n) {
        return bits.readNBit(n);
    }

    public static int readUE(final BitReader bits) {
        int cnt = 0;
        while (bits.read1Bit() == 0 && cnt < 32)
            cnt++;

        int res = 0;
        if (cnt > 0) {
            final long val = bits.readNBit(cnt);

            res = (int) ((1 << cnt) - 1 + val);
        }

        return res;
    }

    public static int readSE(final BitReader bits) {
        int val = readUE(bits);

        val = H264Utils.golomb2Signed(val);

        return val;
    }

    public static boolean readBool(final BitReader bits) {
        return bits.read1Bit() != 0;
    }

    public static int readU(final BitReader bits, final int i) {
        return readNBit(bits, i);
    }

    public static int readTE(final BitReader bits, final int max) {
        if (max > 1)
            return readUE(bits);
        return ~bits.read1Bit() & 0x1;
    }

    public static int readZeroBitCount(final BitReader bits) {
        int count = 0;
        while (bits.read1Bit() == 0 && count < 32)
            count++;

        return count;
    }

    public static boolean moreRBSPData(final BitReader bits) {
        return !(bits.remaining() < 32 && bits.checkNBit(1) == 1 && (bits.checkNBit(24) << 9) == 0);
    }
}
