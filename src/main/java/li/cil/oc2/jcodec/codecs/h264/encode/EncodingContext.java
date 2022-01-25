/* SPDX-License-Identifier: BSD 2-Clause "Simplified" License */

package li.cil.oc2.jcodec.codecs.h264.encode;

import li.cil.oc2.jcodec.codecs.h264.io.CAVLC;
import li.cil.oc2.jcodec.codecs.h264.io.model.MBType;

import static java.lang.System.arraycopy;

public final class EncodingContext {
    public CAVLC[] cavlc;
    public final byte[][] leftRow;
    public final byte[][] topLine;
    public final byte[] topLeft;
    public final int[] mvTopX;
    public final int[] mvTopY;
    public final int[] mvTopR;
    public final int[] mvLeftX;
    public final int[] mvLeftY;
    public final int[] mvLeftR;
    public int mvTopLeftX;
    public int mvTopLeftY;
    public int mvTopLeftR;
    public final int mbHeight;
    public final int mbWidth;
    public int prevQp;

    public final int[] i4x4PredTop;
    public final int[] i4x4PredLeft;
    public MBType leftMBType;
    public final MBType[] topMBType;

    public EncodingContext(final int mbWidth, final int mbHeight) {
        this.mbWidth = mbWidth;
        this.mbHeight = mbHeight;
        leftRow = new byte[][]{new byte[16], new byte[8], new byte[8]};
        topLine = new byte[][]{new byte[mbWidth << 4], new byte[mbWidth << 3], new byte[mbWidth << 3]};
        topLeft = new byte[4];

        mvTopX = new int[mbWidth << 2];
        mvTopY = new int[mbWidth << 2];
        mvTopR = new int[mbWidth << 2];
        mvLeftX = new int[4];
        mvLeftY = new int[4];
        mvLeftR = new int[4];
        i4x4PredTop = new int[mbWidth << 2];
        i4x4PredLeft = new int[4];
        topMBType = new MBType[mbWidth];
    }

    public void update(final EncodedMB mb) {
        if (mb.getType() != MBType.I_NxN) {
            topLeft[0] = topLine[0][(mb.mbX << 4) + 15];
            arraycopy(mb.pixels.getPlaneData(0), 240, topLine[0], mb.mbX << 4, 16);
            copyCol(mb.pixels.getPlaneData(0), 15, 16, leftRow[0]);
        }
        topLeft[1] = topLine[1][(mb.mbX << 3) + 7];
        topLeft[2] = topLine[2][(mb.mbX << 3) + 7];
        arraycopy(mb.pixels.getPlaneData(1), 56, topLine[1], mb.mbX << 3, 8);
        arraycopy(mb.pixels.getPlaneData(2), 56, topLine[2], mb.mbX << 3, 8);

        copyCol(mb.pixels.getPlaneData(1), 7, 8, leftRow[1]);
        copyCol(mb.pixels.getPlaneData(2), 7, 8, leftRow[2]);

        mvTopLeftX = mvTopX[mb.mbX << 2];
        mvTopLeftY = mvTopY[mb.mbX << 2];
        mvTopLeftR = mvTopR[mb.mbX << 2];
        for (int i = 0; i < 4; i++) {
            mvTopX[(mb.mbX << 2) + i] = mb.mx[12 + i];
            mvTopY[(mb.mbX << 2) + i] = mb.my[12 + i];
            mvTopR[(mb.mbX << 2) + i] = mb.mr[12 + i];
            mvLeftX[i] = mb.mx[(i << 2)];
            mvLeftY[i] = mb.my[(i << 2)];
            mvLeftR[i] = mb.mr[(i << 2)];
        }
        topMBType[mb.mbX] = leftMBType = mb.getType();
    }

    private void copyCol(final byte[] planeData, int off, final int stride, final byte[] out) {
        for (int i = 0; i < out.length; i++) {
            out[i] = planeData[off];
            off += stride;
        }
    }

    public EncodingContext fork() {
        final EncodingContext ret = new EncodingContext(mbWidth, mbHeight);
        ret.cavlc = new CAVLC[3];
        for (int i = 0; i < 3; i++) {
            System.arraycopy(leftRow[i], 0, ret.leftRow[i], 0, leftRow[i].length);
            System.arraycopy(topLine[i], 0, ret.topLine[i], 0, topLine[i].length);
            ret.topLeft[i] = topLeft[i];
            ret.cavlc[i] = cavlc[i].fork();
        }
        System.arraycopy(mvTopX, 0, ret.mvTopX, 0, ret.mvTopX.length);
        System.arraycopy(mvTopY, 0, ret.mvTopY, 0, ret.mvTopY.length);
        System.arraycopy(mvTopR, 0, ret.mvTopR, 0, ret.mvTopR.length);
        System.arraycopy(mvLeftX, 0, ret.mvLeftX, 0, ret.mvLeftX.length);
        System.arraycopy(mvLeftY, 0, ret.mvLeftY, 0, ret.mvLeftY.length);
        System.arraycopy(mvLeftR, 0, ret.mvLeftR, 0, ret.mvLeftR.length);
        ret.mvTopLeftX = mvTopLeftX;
        ret.mvTopLeftY = mvTopLeftY;
        ret.mvTopLeftR = mvTopLeftR;
        ret.prevQp = prevQp;

        if (mbWidth > 0)
            System.arraycopy(topMBType, 0, ret.topMBType, 0, mbWidth);
        ret.leftMBType = leftMBType;

        System.arraycopy(i4x4PredTop, 0, ret.i4x4PredTop, 0, mbWidth << 2);
        System.arraycopy(i4x4PredLeft, 0, ret.i4x4PredLeft, 0, 4);
        return ret;
    }
}
