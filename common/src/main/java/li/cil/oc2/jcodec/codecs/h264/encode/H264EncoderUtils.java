/* SPDX-License-Identifier: BSD 2-Clause "Simplified" License */

package li.cil.oc2.jcodec.codecs.h264.encode;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * Contains utility functions commonly used in H264 encoder
 *
 * @author Stanislav Vitvitskyy
 */
public final class H264EncoderUtils {
    public static int median(int a, boolean ar,
                             int b, boolean br,
                             int c, boolean cr,
                             final int d, final boolean dr,
                             final boolean aAvb,
                             boolean bAvb,
                             boolean cAvb,
                             final boolean dAvb) {
        ar &= aAvb;
        br &= bAvb;
        cr &= cAvb;

        if (!cAvb) {
            c = d;
            cr = dr;
            cAvb = dAvb;
        }

        if (aAvb && !bAvb && !cAvb) {
            b = c = a;
            bAvb = cAvb = true;
        }

        a = aAvb ? a : 0;
        b = bAvb ? b : 0;
        c = cAvb ? c : 0;

        if (ar && !br && !cr)
            return a;
        else if (br && !ar && !cr)
            return b;
        else if (cr && !ar && !br)
            return c;

        return a + b + c - min(min(a, b), c) - max(max(a, b), c);
    }
}
