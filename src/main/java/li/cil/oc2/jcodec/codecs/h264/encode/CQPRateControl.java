/* SPDX-License-Identifier: BSD 2-Clause "Simplified" License */

package li.cil.oc2.jcodec.codecs.h264.encode;

import li.cil.oc2.jcodec.codecs.h264.io.model.SliceType;
import li.cil.oc2.jcodec.common.model.Picture;
import li.cil.oc2.jcodec.common.model.Size;
import li.cil.oc2.jcodec.common.tools.MathUtil;

/**
 * Constant QP with psyvisual adjustments
 *
 * @author Stanislav Vitvitskiy
 */
public final class CQPRateControl implements RateControl {
    private static final int MINQP = 12;
    private final int initialQp;
    private int oldQp;
    private SliceType sliceType;

    public CQPRateControl(final int qp) {
        this.initialQp = qp;
    }

    @Override
    public int startPicture(final Size sz, final int maxSize, final SliceType sliceType) {
        this.oldQp = initialQp;
        this.sliceType = sliceType;
        return initialQp;
    }

    @Override
    public int accept(final int bits) {
        return 0;
    }

    @Override
    public int initialQpDelta(final Picture pic, final int mbX, final int mbY) {
        if (initialQp <= MINQP) {
            return 0;
        }
        final byte[] patch = new byte[256];
        MBEncoderHelper.take(pic.getPlaneData(0), pic.getPlaneWidth(0), pic.getPlaneHeight(0), mbX << 4, mbY << 4,
            patch, 16, 16);
        final int avg = calcAvg(patch);
        double var = calcVar(patch, avg);
        final double bright = calcBright(avg);
        int newQp = initialQp;
        final int range = (initialQp - MINQP) / 2;
        // Brightness
        final double delta = var * 0.1 * Math.max(0, bright - 2);
        var += delta;

        // Variance
        if (var < 4) {
            newQp = sliceType == SliceType.I ? Math.max(initialQp / 2, 12) : Math.max(2 * initialQp / 3, 18);
        } else if (var < 8) {
            newQp = initialQp - range / 2;
        } else if (var < 16) {
            newQp = initialQp - range / 4;
        } else if (var < 32) {
            newQp = initialQp - range / 8;
        } else if (var < 64) {
            newQp = initialQp - range / 16;
        }
        final int qpDelta = newQp - oldQp;
        oldQp = newQp;
        return qpDelta;
    }

    private int calcAvg(final byte[] patch) {
        int sum = 0;
        for (int i = 0; i < 256; i++)
            sum += patch[i];
        return sum >> 8;
    }

    private double calcVar(final byte[] patch, final int avg) {
        long sum1 = 0;
        for (int i = 0; i < 256; i++) {
            final int diff = patch[i] - avg;
            sum1 += diff * diff;
        }

        return Math.sqrt(sum1 >> 8);
    }

    private double calcBright(final int avg) {
        return MathUtil.log2(avg + 128);
    }
}
