/* SPDX-License-Identifier: BSD 2-Clause "Simplified" License */

package li.cil.oc2.jcodec.common;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author Jay Codec
 */
public final class ArrayUtil {
    public static void swap(final int[] arr, final int ind1, final int ind2) {
        if (ind1 == ind2)
            return;
        final int tmp = arr[ind1];
        arr[ind1] = arr[ind2];
        arr[ind2] = tmp;
    }

    public static int sumByte(final byte[] array) {
        int result = 0;
        for (final byte b : array) {
            result += b;
        }
        return result;
    }

    public static int sumByte(final byte[] array, final int from, final int count) {
        int result = 0;
        for (int i = from, end = from + count; i < end; i++) {
            result += array[i];
        }
        return result;
    }
}
