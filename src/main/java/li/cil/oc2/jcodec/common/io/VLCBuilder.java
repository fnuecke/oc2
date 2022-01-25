/* SPDX-License-Identifier: BSD 2-Clause "Simplified" License */

package li.cil.oc2.jcodec.common.io;

import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import li.cil.oc2.jcodec.common.IntArrayList;


/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * <p>
 * prefix VLC reader builder
 *
 * @author The JCodec project
 */
public final class VLCBuilder {
    private final Int2IntArrayMap forward;
    private final Int2IntArrayMap inverse;
    private final IntArrayList codes;
    private final IntArrayList codesSizes;

    public VLCBuilder() {
        this.forward = new Int2IntArrayMap();
        this.inverse = new Int2IntArrayMap();
        this.codes = IntArrayList.createIntArrayList();
        this.codesSizes = IntArrayList.createIntArrayList();
    }

    public VLCBuilder set(final int val, final String code) {
        return setInt(Integer.parseInt(code, 2), code.length(), val);
    }

    public VLCBuilder setInt(final int code, final int len, final int val) {
        codes.add(code << (32 - len));
        codesSizes.add(len);
        forward.put(val, codes.size() - 1);
        inverse.put(codes.size() - 1, val);

        return this;
    }

    public VLC getVLC() {
        final VLCBuilder self = this;
        return new VLC(codes.toArray(), codesSizes.toArray()) {
            public int readVLC(final BitReader _in) {
                return self.inverse.get(super.readVLC(_in));
            }

            public int readVLC16(final BitReader _in) {
                return self.inverse.get(super.readVLC16(_in));
            }

            public void writeVLC(final BitWriter out, final int code) {
                super.writeVLC(out, self.forward.get(code));
            }
        };
    }
}
