/* SPDX-License-Identifier: BSD 2-Clause "Simplified" License */

package li.cil.oc2.jcodec.codecs.h264.decode;

import li.cil.oc2.jcodec.codecs.h264.H264Utils;
import li.cil.oc2.jcodec.codecs.h264.io.model.Frame;
import li.cil.oc2.jcodec.codecs.h264.io.model.MBType;
import li.cil.oc2.jcodec.codecs.h264.io.model.SeqParameterSet;
import li.cil.oc2.jcodec.codecs.h264.io.model.SliceHeader;

import static li.cil.oc2.jcodec.codecs.h264.io.model.SeqParameterSet.getPicHeightInMbs;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * <p>
 * Contains an input for deblocking filter
 *
 * @author The JCodec project
 */
public final class DeblockerInput {
    public final int[][] nCoeff;
    public final H264Utils.MvList2D mvs;
    public final MBType[] mbTypes;
    public final int[][] mbQps;
    public final boolean[] tr8x8Used;
    public final Frame[][][] refsUsed;
    public final SliceHeader[] shs;

    public DeblockerInput(final SeqParameterSet activeSps) {
        final int picWidthInMbs = activeSps.picWidthInMbsMinus1 + 1;
        final int picHeightInMbs = getPicHeightInMbs(activeSps);

        nCoeff = new int[picHeightInMbs << 2][picWidthInMbs << 2];
        mvs = new H264Utils.MvList2D(picWidthInMbs << 2, picHeightInMbs << 2);
        mbTypes = new MBType[picHeightInMbs * picWidthInMbs];
        tr8x8Used = new boolean[picHeightInMbs * picWidthInMbs];
        mbQps = new int[3][picHeightInMbs * picWidthInMbs];
        shs = new SliceHeader[picHeightInMbs * picWidthInMbs];
        refsUsed = new Frame[picHeightInMbs * picWidthInMbs][][];
    }
}
