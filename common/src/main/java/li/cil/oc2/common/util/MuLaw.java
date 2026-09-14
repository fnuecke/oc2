/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.util;

/**
 * G.711 mu-law companding, the format sound card audio is sent to clients in.
 * <p>
 * Eight bits per sample, with an error proportional to the signal, so quiet audio keeps its
 * detail where linear eight bit samples would drown it in quantization noise.
 */
public final class MuLaw {
    private static final int EXPONENT_BASE_BIT = 7; // in encoded value [seeemmmm]
    private static final int MANTISSA_BITS = 4;
    private static final int MANTISSA_SHIFT = EXPONENT_BASE_BIT - MANTISSA_BITS;
    private static final int BIAS = (1 << EXPONENT_BASE_BIT) + (1 << (MANTISSA_SHIFT - 1));
    private static final int CLIP = Short.MAX_VALUE - BIAS;

    private static final short[] DECODE_TABLE = buildDecodeTable();

    // --------------------------------------------------------------------- //

    public static byte encode(final int sample) {
        final int magnitude = Math.min(Math.abs(sample), CLIP) + BIAS;

        final int sign = sample < 0 ? 0b1000_0000 : 0;
        final int exponent = 31 - Integer.numberOfLeadingZeros(magnitude >> EXPONENT_BASE_BIT);
        final int mantissa = (magnitude >> (exponent + MANTISSA_SHIFT)) & 0b0000_1111;

        return (byte) ~(sign | (exponent << MANTISSA_BITS) | mantissa);
    }

    public static short decode(final byte value) {
        return DECODE_TABLE[value & 0xFF];
    }

    // --------------------------------------------------------------------- //

    private static short[] buildDecodeTable() {
        final short[] table = new short[256];
        for (int i = 0; i < table.length; i++) {
            final int value = ~i & 0xFF;

            final int sign = value & 0b1000_0000;
            final int exponent = (value & 0b0111_0000) >> MANTISSA_BITS;
            final int mantissa = value & 0b0000_1111;

            final int magnitude = ((mantissa << MANTISSA_SHIFT) + BIAS) << exponent;
            table[i] = (short) (sign != 0 ? BIAS - magnitude : magnitude - BIAS);
        }
        return table;
    }

    // --------------------------------------------------------------------- //

    private MuLaw() {
    }
}
