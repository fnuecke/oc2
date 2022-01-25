/* SPDX-License-Identifier: BSD 2-Clause "Simplified" License */

package li.cil.oc2.jcodec.common.model;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
public final class ColorSpace {
    public static final int MAX_PLANES = 4;

    public final int nComp;
    public final int[] compPlane;
    public final int[] compWidth;
    public final int[] compHeight;
    public final boolean planar;

    private final String _name;

    private ColorSpace(final String name, final int nComp, final int[] compPlane, final int[] compWidth, final int[] compHeight, final boolean planar) {
        this._name = name;
        this.nComp = nComp;
        this.compPlane = compPlane;
        this.compWidth = compWidth;
        this.compHeight = compHeight;
        this.planar = planar;
    }

    @Override
    public String toString() {
        return _name;
    }

    private static final int[] _000 = new int[]{0, 0, 0};
    private static final int[] _011 = new int[]{0, 1, 1};
    private static final int[] _012 = new int[]{0, 1, 2};
    public final static ColorSpace RGB = new ColorSpace("RGB", 3, _000, _000, _000, false);
    public final static ColorSpace YUV420 = new ColorSpace("YUV420", 3, _012, _011, _011, true);
    public final static ColorSpace YUV420J = new ColorSpace("YUV420J", 3, _012, _011, _011, true);
    public final static ColorSpace YUV422 = new ColorSpace("YUV422", 3, _012, _011, _000, true);
    public final static ColorSpace YUV444 = new ColorSpace("YUV444", 3, _012, _000, _000, true);
    public final static ColorSpace MONO = new ColorSpace("MONO", 1, _000, _000, _000, true);
}
