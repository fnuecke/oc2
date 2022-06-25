/* SPDX-License-Identifier: BSD 2-Clause "Simplified" License */

package li.cil.oc2.jcodec.codecs.h264.decode;

import li.cil.oc2.jcodec.codecs.h264.H264Const;
import li.cil.oc2.jcodec.codecs.h264.io.model.Frame;
import li.cil.oc2.jcodec.codecs.h264.io.model.SliceHeader;
import li.cil.oc2.jcodec.codecs.h264.io.model.SliceType;
import li.cil.oc2.jcodec.common.IntObjectMap;
import li.cil.oc2.jcodec.common.model.Picture;
import li.cil.oc2.jcodec.common.tools.MathUtil;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Contains reference picture list management logic
 *
 * @author The JCodec Project
 */
public final class RefListManager {
    private final SliceHeader sh;
    private final int[] numRef;
    private final Frame[] sRefs;
    private final IntObjectMap<Frame> lRefs;
    private final Frame frameOut;

    public RefListManager(final SliceHeader sh, final Frame[] sRefs, final IntObjectMap<Frame> lRefs, final Frame frameOut) {
        this.sh = sh;
        this.sRefs = sRefs;
        this.lRefs = lRefs;
        if (sh.numRefIdxActiveOverrideFlag)
            numRef = new int[]{sh.numRefIdxActiveMinus1[0] + 1, sh.numRefIdxActiveMinus1[1] + 1};
        else
            numRef = new int[]{sh.pps.numRefIdxActiveMinus1[0] + 1, sh.pps.numRefIdxActiveMinus1[1] + 1};
        this.frameOut = frameOut;
    }

    public Frame[][] getRefList() {
        Frame[][] refList = null;
        if (sh.sliceType == SliceType.P) {
            refList = new Frame[][]{buildRefListP(), null};
        } else if (sh.sliceType == SliceType.B) {
            refList = buildRefListB();
        }

        return refList;
    }

    private Frame[] buildRefListP() {
        final int frame_num = sh.frameNum;
        final int maxFrames = 1 << (sh.sps.log2MaxFrameNumMinus4 + 4);
        // int nLongTerm = Math.min(lRefs.size(), numRef[0] - 1);
        final Frame[] result = new Frame[numRef[0]];

        int refs = 0;
        for (int i = frame_num - 1; i >= frame_num - maxFrames && refs < numRef[0]; i--) {
            final int fn = i < 0 ? i + maxFrames : i;
            if (sRefs[fn] != null) {
                result[refs] = sRefs[fn] == H264Const.NO_PIC ? null : sRefs[fn];
                ++refs;
            }
        }
        final int[] keys = lRefs.keys();
        Arrays.sort(keys);
        for (int i = 0; i < keys.length && refs < numRef[0]; i++) {
            result[refs++] = lRefs.get(keys[i]);
        }

        reorder(result, 0);

        return result;
    }

    private Frame[][] buildRefListB() {
        final Frame[] l0 = buildList(Frame.POCDesc, Frame.POCAsc);
        final Frame[] l1 = buildList(Frame.POCAsc, Frame.POCDesc);

        if (Arrays.equals(l0, l1) && count(l1) > 1) {
            final Frame frame = l1[1];
            l1[1] = l1[0];
            l1[0] = frame;
        }

        final Frame[][] result = {Arrays.copyOf(l0, numRef[0]), Arrays.copyOf(l1, numRef[1])};

        reorder(result[0], 0);
        reorder(result[1], 1);

        return result;
    }

    private Frame[] buildList(final Comparator<Frame> cmpFwd, final Comparator<Frame> cmpInv) {
        final Frame[] refs = new Frame[sRefs.length + lRefs.size()];
        final Frame[] fwd = copySort(cmpFwd, frameOut);
        final Frame[] inv = copySort(cmpInv, frameOut);
        final int nFwd = count(fwd);
        final int nInv = count(inv);

        int ref = 0;
        for (int i = 0; i < nFwd; i++, ref++)
            refs[ref] = fwd[i];
        for (int i = 0; i < nInv; i++, ref++)
            refs[ref] = inv[i];

        final int[] keys = lRefs.keys();
        Arrays.sort(keys);
        for (int i = 0; i < keys.length; i++, ref++)
            refs[ref] = lRefs.get(keys[i]);

        return refs;
    }

    private int count(final Frame[] arr) {
        for (int nn = 0; nn < arr.length; nn++)
            if (arr[nn] == null)
                return nn;
        return arr.length;
    }

    private Frame[] copySort(final Comparator<Frame> fwd, final Frame dummy) {
        final Frame[] copyOf = Arrays.copyOf(sRefs, sRefs.length);
        for (int i = 0; i < copyOf.length; i++)
            if (fwd.compare(dummy, copyOf[i]) > 0)
                copyOf[i] = null;
        Arrays.sort(copyOf, fwd);
        return copyOf;
    }

    private void reorder(final Picture[] result, final int list) {
        if (sh.refPicReordering[list] == null)
            return;

        int predict = sh.frameNum;
        final int maxFrames = 1 << (sh.sps.log2MaxFrameNumMinus4 + 4);

        for (int ind = 0; ind < sh.refPicReordering[list][0].length; ind++) {
            final int refType = sh.refPicReordering[list][0][ind];
            final int refIdx = sh.refPicReordering[list][1][ind];

            final int count = numRef[list] - 1 - ind;
            if (count > 0)
                System.arraycopy(result, ind, result, ind + 1, count);
            if (refType == 2) {
                result[ind] = lRefs.get(refIdx);
            } else {
                predict = refType == 0 ? MathUtil.wrap(predict - refIdx - 1, maxFrames) : MathUtil.wrap(predict + refIdx + 1, maxFrames);
                result[ind] = sRefs[predict];
            }
            for (int i = ind + 1, j = i; i < numRef[list] && result[i] != null; i++) {
                if (result[i] != sRefs[predict])
                    result[j++] = result[i];
            }
        }
    }

}
