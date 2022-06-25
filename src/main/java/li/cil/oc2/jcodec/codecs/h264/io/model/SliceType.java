/* SPDX-License-Identifier: BSD 2-Clause "Simplified" License */

package li.cil.oc2.jcodec.codecs.h264.io.model;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
public final class SliceType {
    private final static SliceType[] _values = new SliceType[5];
    public final static SliceType P = new SliceType("P", 0);
    public final static SliceType B = new SliceType("B", 1);
    public final static SliceType I = new SliceType("I", 2);
    public final static SliceType SP = new SliceType("SP", 3);
    public final static SliceType SI = new SliceType("SI", 4);
    private final String _name;
    private final int _ordinal;

    private SliceType(final String name, final int ordinal) {
        this._name = name;
        this._ordinal = ordinal;
        _values[ordinal] = this;
    }

    public boolean isIntra() {
        return this == I || this == SI;
    }

    public boolean isInter() {
        return this != I && this != SI;
    }

    public static SliceType[] values() {
        return _values;
    }

    public int ordinal() {
        return _ordinal;
    }

    @Override
    public String toString() {
        return _name;
    }

    public String name() {
        return _name;
    }

    public static SliceType fromValue(final int j) {
        return values()[j];
    }
}
