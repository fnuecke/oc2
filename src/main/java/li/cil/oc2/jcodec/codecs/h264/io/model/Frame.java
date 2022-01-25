/* SPDX-License-Identifier: BSD 2-Clause "Simplified" License */

package li.cil.oc2.jcodec.codecs.h264.io.model;

import li.cil.oc2.jcodec.codecs.h264.H264Utils.MvList2D;
import li.cil.oc2.jcodec.common.model.ColorSpace;
import li.cil.oc2.jcodec.common.model.Picture;

import java.util.Comparator;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * <p>
 * Picture extension with frame number, makes it easier to debug reordering
 *
 * @author The JCodec project
 */
public final class Frame extends Picture {
    private int frameNo;
    private MvList2D mvs;
    private Frame[][][] refsUsed;
    private boolean shortTerm;
    private int poc;

    public Frame(final int width, final int height, final byte[][] data, final ColorSpace color, final int frameNo,
                 final MvList2D mvs, final Frame[][][] refsUsed, final int poc) {
        super(width, height, data, color);
        this.frameNo = frameNo;
        this.mvs = mvs;
        this.refsUsed = refsUsed;
        this.poc = poc;
        shortTerm = true;
    }

    public static Frame createFrame(final Frame pic) {
        final Picture comp = pic.createCompatible();
        return new Frame(comp.getWidth(), comp.getHeight(), comp.getData(), comp.getColor(),
            pic.frameNo, pic.mvs, pic.refsUsed, pic.poc);
    }

    public void copyFromFrame(final Frame src) {
        super.copyFrom(src);
        this.frameNo = src.frameNo;
        this.mvs = src.mvs;
        this.shortTerm = src.shortTerm;
        this.refsUsed = src.refsUsed;
        this.poc = src.poc;
    }

    public int getFrameNo() {
        return frameNo;
    }

    public MvList2D getMvs() {
        return mvs;
    }

    public boolean isShortTerm() {
        return shortTerm;
    }

    public void setShortTerm(final boolean shortTerm) {
        this.shortTerm = shortTerm;
    }

    public int getPOC() {
        return poc;
    }

    public static final Comparator<Frame> POCAsc = (o1, o2) -> {
        if (o1 == null && o2 == null)
            return 0;
        else if (o1 == null)
            return 1;
        else if (o2 == null)
            return -1;
        else
            return Integer.compare(o1.poc, o2.poc);
    };

    public static final Comparator<Frame> POCDesc = (o1, o2) -> {
        if (o1 == null && o2 == null)
            return 0;
        else if (o1 == null)
            return 1;
        else if (o2 == null)
            return -1;
        else
            return Integer.compare(o2.poc, o1.poc);
    };

    public Frame[][][] getRefsUsed() {
        return refsUsed;
    }
}
