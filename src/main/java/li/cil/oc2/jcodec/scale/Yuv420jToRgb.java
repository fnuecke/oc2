/* SPDX-License-Identifier: BSD 2-Clause "Simplified" License */

package li.cil.oc2.jcodec.scale;

import li.cil.oc2.jcodec.common.model.Picture;
import li.cil.oc2.jcodec.common.tools.MathUtil;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
public final class Yuv420jToRgb implements Transform {
    public Yuv420jToRgb() {
    }

    @Override
    public void transform(final Picture src, final Picture dst) {
        final byte[] y = src.getPlaneData(0);
        final byte[] u = src.getPlaneData(1);
        final byte[] v = src.getPlaneData(2);
        final byte[] data = dst.getPlaneData(0);

        int offLuma = 0, offChroma = 0;
        final int stride = dst.getWidth();
        for (int i = 0; i < (dst.getHeight() >> 1); i++) {
            for (int k = 0; k < (dst.getWidth() >> 1); k++) {
                final int j = k << 1;
                YUVJtoRGB(y[offLuma + j], u[offChroma], v[offChroma], data, (offLuma + j) * 3);
                YUVJtoRGB(y[offLuma + j + 1], u[offChroma], v[offChroma], data, (offLuma + j + 1) * 3);

                YUVJtoRGB(y[offLuma + j + stride], u[offChroma], v[offChroma], data, (offLuma + j + stride) * 3);
                YUVJtoRGB(y[offLuma + j + stride + 1], u[offChroma], v[offChroma], data, (offLuma + j + stride + 1) * 3);

                ++offChroma;
            }
            if ((dst.getWidth() & 0x1) != 0) {
                final int j = dst.getWidth() - 1;

                YUVJtoRGB(y[offLuma + j], u[offChroma], v[offChroma], data, (offLuma + j) * 3);
                YUVJtoRGB(y[offLuma + j + stride], u[offChroma], v[offChroma], data, (offLuma + j + stride) * 3);

                ++offChroma;
            }

            offLuma += 2 * stride;
        }
        if ((dst.getHeight() & 0x1) != 0) {
            for (int k = 0; k < (dst.getWidth() >> 1); k++) {
                final int j = k << 1;
                YUVJtoRGB(y[offLuma + j], u[offChroma], v[offChroma], data, (offLuma + j) * 3);
                YUVJtoRGB(y[offLuma + j + 1], u[offChroma], v[offChroma], data, (offLuma + j + 1) * 3);

                ++offChroma;
            }
            if ((dst.getWidth() & 0x1) != 0) {
                final int j = dst.getWidth() - 1;

                YUVJtoRGB(y[offLuma + j], u[offChroma], v[offChroma], data, (offLuma + j) * 3);

                ++offChroma;
            }
        }
    }

    private static final int SCALEBITS = 10;
    private static final int ONE_HALF = (1 << (SCALEBITS - 1));

    private static int FIX(final double x) {
        return ((int) ((x) * (1 << SCALEBITS) + 0.5));
    }

    private static final int FIX_0_71414 = FIX(0.71414);
    private static final int FIX_1_772 = FIX(1.77200);
    private static final int _FIX_0_34414 = -FIX(0.34414);
    private static final int FIX_1_402 = FIX(1.40200);

    public static void YUVJtoRGB(final byte y, final byte cb, final byte cr, final byte[] data, final int off) {
        final int y_ = (y + 128) << SCALEBITS;
        final int add_r = FIX_1_402 * cr + ONE_HALF;
        final int add_g = _FIX_0_34414 * cb - FIX_0_71414 * cr + ONE_HALF;
        final int add_b = FIX_1_772 * cb + ONE_HALF;

        final int r = (y_ + add_r) >> SCALEBITS;
        final int g = (y_ + add_g) >> SCALEBITS;
        final int b = (y_ + add_b) >> SCALEBITS;
        data[off] = (byte) MathUtil.clip(r - 128, -128, 127);
        data[off + 1] = (byte) MathUtil.clip(g - 128, -128, 127);
        data[off + 2] = (byte) MathUtil.clip(b - 128, -128, 127);
    }
}
