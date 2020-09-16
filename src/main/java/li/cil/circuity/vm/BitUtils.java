package li.cil.circuity.vm;

public final class BitUtils {
    /**
     * Extract a field encoded in an integer value and shifts it to the desired output position.
     * The length is determined by the destination low and high bit indices.
     *
     * @param value       the value that contains the field.
     * @param srcBitFrom  the lowest bit of the field in the source value.
     * @param srcBitUntil the highest bit of the field in the source value.
     * @param destBit     the lowest bit of the field in the destination (returned) value.
     * @return the extracted field at the bit location specified in <code>destBitFrom</code> and <code>destBitUntil</code>.
     */
    public static int getField(final int value, final int srcBitFrom, final int srcBitUntil, final int destBit) {
        // For bit-shifts Java always only uses the lowest five bits for the right-hand operand,
        // so we can't be clever and shift by a negative amount; need to branch here.
        // NB: This method is optimized for bytecode size to make sure it gets inlined.
        return (destBit >= srcBitFrom
                ? value << (destBit - srcBitFrom)
                : value >>> (srcBitFrom - destBit))
               & ((1 << (srcBitUntil - srcBitFrom + 1)) - 1) << destBit;
    }

    /**
     * Sign-extends a value of the specified bit width stored in the specified integer value.
     *
     * @param value the integer value holding the less-than-32-bit wide value.
     * @param width the bit width of the value to be sign-extended.
     * @return the sign-extended integer value representing the specified value.
     */
    public static int extendSign(final int value, final int width) {
        return (value << (32 - width)) >> (32 - width);
    }
}
