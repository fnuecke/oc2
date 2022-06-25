/* SPDX-License-Identifier: BSD 2-Clause "Simplified" License */

package li.cil.oc2.jcodec.common.io;

import li.cil.oc2.jcodec.common.IntArrayList;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * <p>
 * Table-based prefix VLC reader
 *
 * @author The JCodec project
 */
public class VLC {
    private final int[] codes;
    private final int[] codeSizes;

    private int[] values;
    private int[] valueSizes;

    public VLC(final int[] codes, final int[] codeSizes) {
        this.codes = codes;
        this.codeSizes = codeSizes;

        invert();
    }

    private void invert() {
        final IntArrayList values = IntArrayList.createIntArrayList();
        final IntArrayList valueSizes = IntArrayList.createIntArrayList();
        invert(0, 0, 0, values, valueSizes);
        this.values = values.toArray();
        this.valueSizes = valueSizes.toArray();
    }

    private int invert(final int startOff, final int level, final int prefix, final IntArrayList values, final IntArrayList valueSizes) {
        int tableEnd = startOff + 256;
        values.fill(startOff, tableEnd, -1);
        valueSizes.fill(startOff, tableEnd, 0);

        final int prefLen = level << 3;
        for (int i = 0; i < codeSizes.length; i++) {
            if ((codeSizes[i] <= prefLen) || (level > 0 && (codes[i] >>> (32 - prefLen)) != prefix))
                continue;

            final int pref = codes[i] >>> (32 - prefLen - 8);
            final int code = pref & 0xff;
            final int len = codeSizes[i] - prefLen;
            if (len <= 8) {
                for (int k = 0; k < (1 << (8 - len)); k++) {
                    values.set(startOff + code + k, i);
                    valueSizes.set(startOff + code + k, len);
                }
            } else {
                if (values.get(startOff + code) == -1) {
                    values.set(startOff + code, tableEnd);
                    tableEnd = invert(tableEnd, level + 1, pref, values, valueSizes);
                }
            }
        }

        return tableEnd;
    }

    public int readVLC16(final BitReader _in) {

        final int string = _in.check16Bits();
        int b = string >>> 8;
        int code = values[b];
        final int len = valueSizes[b];

        if (len == 0) {
            b = (string & 0xff) + code;
            code = values[b];
            _in.skipFast(8 + valueSizes[b]);
        } else
            _in.skipFast(len);

        return code;
    }

    public int readVLC(final BitReader _in) {

        int code = 0, len = 0, overall = 0;
        for (int i = 0; len == 0; i++) {
            final int string = _in.checkNBit(8);
            final int ind = string + code;
            code = values[ind];
            len = valueSizes[ind];

            final int bits = len != 0 ? len : 8;
            overall = (overall << bits) | (string >> (8 - bits));
            _in.skip(bits);

            if (code == -1)
                throw new RuntimeException("Invalid code prefix");
        }

        return code;
    }

    public void writeVLC(final BitWriter out, final int code) {
        out.writeNBit(codes[code] >>> (32 - codeSizes[code]), codeSizes[code]);
    }

    public int[] getCodes() {
        return codes;
    }

    public int[] getCodeSizes() {
        return codeSizes;
    }
}
