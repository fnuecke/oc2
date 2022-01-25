/* SPDX-License-Identifier: BSD 2-Clause "Simplified" License */

package li.cil.oc2.jcodec.codecs.h264.encode;

import li.cil.oc2.jcodec.codecs.h264.io.model.MBType;
import li.cil.oc2.jcodec.common.model.ColorSpace;
import li.cil.oc2.jcodec.common.model.Picture;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
public final class EncodedMB {
    public final Picture pixels;
    public MBType type;
    public int qp;
    public final int[] nc;
    public final int[] mx;
    public final int[] my;
    public final int[] mr;
    public int mbX;
    public int mbY;

    public EncodedMB() {
        pixels = Picture.create(16, 16, ColorSpace.YUV420J);
        nc = new int[16];
        mx = new int[16];
        my = new int[16];
        mr = new int[16];
    }

    public Picture getPixels() {
        return pixels;
    }

    public MBType getType() {
        return type;
    }

    public void setType(final MBType type) {
        this.type = type;
    }

    public int getQp() {
        return qp;
    }

    public void setQp(final int qp) {
        this.qp = qp;
    }

    public int[] getNc() {
        return nc;
    }

    public int[] getMx() {
        return mx;
    }

    public int[] getMy() {
        return my;
    }

    public void setPos(final int mbX, final int mbY) {
        this.mbX = mbX;
        this.mbY = mbY;
    }

    public int[] getMr() {
        return mr;
    }
}
