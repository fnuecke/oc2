/* SPDX-License-Identifier: BSD 2-Clause "Simplified" License */

package li.cil.oc2.jcodec.common.tools;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
public final class MathUtil {
    private static final int[] logTab = new int[]{0, 0, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4,
        4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
        5, 5, 5, 5, 5, 5, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6,
        6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 7, 7,
        7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
        7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
        7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
        7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7};

    public static int log2(int v) {
        int n = 0;
        if ((v & 0xffff0000) != 0) {
            v >>= 16;
            n += 16;
        }
        if ((v & 0xff00) != 0) {
            v >>= 8;
            n += 8;
        }
        n += logTab[v];

        return n;
    }

    public static int clip(final int val, final int from, final int to) {
        return Math.max(from, Math.min(val, to));
    }

    public static int abs(final int val) {
        final int sign = (val >> 31);
        return (val ^ sign) - sign;
    }

    public static int golomb(final int signedLevel) {
        if (signedLevel == 0)
            return 0;
        return (abs(signedLevel) << 1) - (~signedLevel >>> 31);
    }

    public static int toSigned(final int val, final int sign) {
        return (val ^ sign) - sign;
    }

    public static int wrap(final int picNo, final int maxFrames) {
        return picNo < 0 ? picNo + maxFrames : (picNo >= maxFrames ? picNo - maxFrames : picNo);
    }

    public static int max3(final int a, final int b, final int c) {
        return Math.max(Math.max(a, b), c);
    }

    public static int min3(final int a, final int b, final int c) {
        return Math.min(Math.min(a, b), c);
    }
}
