/* SPDX-License-Identifier: BSD 2-Clause "Simplified" License */

package li.cil.oc2.jcodec.codecs.h264;

import li.cil.oc2.jcodec.codecs.h264.io.model.MBType;
import li.cil.oc2.jcodec.common.io.VLC;
import li.cil.oc2.jcodec.common.io.VLCBuilder;
import li.cil.oc2.jcodec.common.model.Picture;

import java.util.Arrays;

import static li.cil.oc2.jcodec.codecs.h264.H264Const.PartPred.*;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
public final class H264Const {
    public final static VLC[] CoeffToken = new VLC[10];
    public final static VLC coeffTokenChromaDCY420;
    public final static VLC coeffTokenChromaDCY422;
    public final static VLC[] run;
    public static final int[] lambda = new int[]{14, 18, 23, 29, 36, 46, 58, 73, 91, 115, 145, 183, 230, 290, 366, 461, 581,
        731, 922, 1161, 1463, 1843, 2322, 2926, 3686, 4645, 5852, 7373, 9289, 11704, 14746, 18578, 23407, 29491,
        37157, 46814, 58982, 74313, 93629, 117965, 148626, 187257, 235930, 297253, 374515, 471859, 594505, 749030,
        943718, 1189011, 1498060, 1887437, 2378021, 2996119, 3774874, 4756043, 5992238, 7549747, 9512085, 11984477,
        15099494, 19024171, 23968953, 30198989, 38048342, 47937907, 60397978, 76096683, 95875813, 120795955,
        120795955, 120795955, 120795955, 120795955, 120795955, 120795955, 120795955, 120795955, 120795955,
        120795955, 120795955, 120795955};

    static {
        final VLCBuilder vbl = new VLCBuilder();

        vbl.set(0, "1");

        vbl.set(coeffToken(1, 0), "000101");
        vbl.set(coeffToken(1, 1), "01");
        vbl.set(coeffToken(2, 0), "00000111");
        vbl.set(coeffToken(2, 1), "000100");
        vbl.set(coeffToken(2, 2), "001");
        vbl.set(coeffToken(3, 0), "000000111");
        vbl.set(coeffToken(3, 1), "00000110");
        vbl.set(coeffToken(3, 2), "0000101");
        vbl.set(coeffToken(3, 3), "00011");
        vbl.set(coeffToken(4, 0), "0000000111");
        vbl.set(coeffToken(4, 1), "000000110");
        vbl.set(coeffToken(4, 2), "00000101");
        vbl.set(coeffToken(4, 3), "000011");
        vbl.set(coeffToken(5, 0), "00000000111");
        vbl.set(coeffToken(5, 1), "0000000110");
        vbl.set(coeffToken(5, 2), "000000101");
        vbl.set(coeffToken(5, 3), "0000100");
        vbl.set(coeffToken(6, 0), "0000000001111");
        vbl.set(coeffToken(6, 1), "00000000110");
        vbl.set(coeffToken(6, 2), "0000000101");
        vbl.set(coeffToken(6, 3), "00000100");
        vbl.set(coeffToken(7, 0), "0000000001011");
        vbl.set(coeffToken(7, 1), "0000000001110");
        vbl.set(coeffToken(7, 2), "00000000101");
        vbl.set(coeffToken(7, 3), "000000100");
        vbl.set(coeffToken(8, 0), "0000000001000");
        vbl.set(coeffToken(8, 1), "0000000001010");
        vbl.set(coeffToken(8, 2), "0000000001101");
        vbl.set(coeffToken(8, 3), "0000000100");
        vbl.set(coeffToken(9, 0), "00000000001111");
        vbl.set(coeffToken(9, 1), "00000000001110");
        vbl.set(coeffToken(9, 2), "0000000001001");
        vbl.set(coeffToken(9, 3), "00000000100");
        vbl.set(coeffToken(10, 0), "00000000001011");
        vbl.set(coeffToken(10, 1), "00000000001010");
        vbl.set(coeffToken(10, 2), "00000000001101");
        vbl.set(coeffToken(10, 3), "0000000001100");
        vbl.set(coeffToken(11, 0), "000000000001111");
        vbl.set(coeffToken(11, 1), "000000000001110");
        vbl.set(coeffToken(11, 2), "00000000001001");
        vbl.set(coeffToken(11, 3), "00000000001100");
        vbl.set(coeffToken(12, 0), "000000000001011");
        vbl.set(coeffToken(12, 1), "000000000001010");
        vbl.set(coeffToken(12, 2), "000000000001101");
        vbl.set(coeffToken(12, 3), "00000000001000");
        vbl.set(coeffToken(13, 0), "0000000000001111");
        vbl.set(coeffToken(13, 1), "000000000000001");
        vbl.set(coeffToken(13, 2), "000000000001001");
        vbl.set(coeffToken(13, 3), "000000000001100");
        vbl.set(coeffToken(14, 0), "0000000000001011");
        vbl.set(coeffToken(14, 1), "0000000000001110");
        vbl.set(coeffToken(14, 2), "0000000000001101");
        vbl.set(coeffToken(14, 3), "000000000001000");
        vbl.set(coeffToken(15, 0), "0000000000000111");
        vbl.set(coeffToken(15, 1), "0000000000001010");
        vbl.set(coeffToken(15, 2), "0000000000001001");
        vbl.set(coeffToken(15, 3), "0000000000001100");
        vbl.set(coeffToken(16, 0), "0000000000000100");
        vbl.set(coeffToken(16, 1), "0000000000000110");
        vbl.set(coeffToken(16, 2), "0000000000000101");
        vbl.set(coeffToken(16, 3), "0000000000001000");
        CoeffToken[0] = CoeffToken[1] = vbl.getVLC();
    }

    static {
        final VLCBuilder vbl = new VLCBuilder();
        vbl.set(coeffToken(0, 0), "11");
        vbl.set(coeffToken(1, 0), "001011");
        vbl.set(coeffToken(1, 1), "10");
        vbl.set(coeffToken(2, 0), "000111");
        vbl.set(coeffToken(2, 1), "00111");
        vbl.set(coeffToken(2, 2), "011");
        vbl.set(coeffToken(3, 0), "0000111");
        vbl.set(coeffToken(3, 1), "001010");
        vbl.set(coeffToken(3, 2), "001001");
        vbl.set(coeffToken(3, 3), "0101");
        vbl.set(coeffToken(4, 0), "00000111");
        vbl.set(coeffToken(4, 1), "000110");
        vbl.set(coeffToken(4, 2), "000101");
        vbl.set(coeffToken(4, 3), "0100");
        vbl.set(coeffToken(5, 0), "00000100");
        vbl.set(coeffToken(5, 1), "0000110");
        vbl.set(coeffToken(5, 2), "0000101");
        vbl.set(coeffToken(5, 3), "00110");
        vbl.set(coeffToken(6, 0), "000000111");
        vbl.set(coeffToken(6, 1), "00000110");
        vbl.set(coeffToken(6, 2), "00000101");
        vbl.set(coeffToken(6, 3), "001000");
        vbl.set(coeffToken(7, 0), "00000001111");
        vbl.set(coeffToken(7, 1), "000000110");
        vbl.set(coeffToken(7, 2), "000000101");
        vbl.set(coeffToken(7, 3), "000100");
        vbl.set(coeffToken(8, 0), "00000001011");
        vbl.set(coeffToken(8, 1), "00000001110");
        vbl.set(coeffToken(8, 2), "00000001101");
        vbl.set(coeffToken(8, 3), "0000100");
        vbl.set(coeffToken(9, 0), "000000001111");
        vbl.set(coeffToken(9, 1), "00000001010");
        vbl.set(coeffToken(9, 2), "00000001001");
        vbl.set(coeffToken(9, 3), "000000100");
        vbl.set(coeffToken(10, 0), "000000001011");
        vbl.set(coeffToken(10, 1), "000000001110");
        vbl.set(coeffToken(10, 2), "000000001101");
        vbl.set(coeffToken(10, 3), "00000001100");
        vbl.set(coeffToken(11, 0), "000000001000");
        vbl.set(coeffToken(11, 1), "000000001010");
        vbl.set(coeffToken(11, 2), "000000001001");
        vbl.set(coeffToken(11, 3), "00000001000");
        vbl.set(coeffToken(12, 0), "0000000001111");
        vbl.set(coeffToken(12, 1), "0000000001110");
        vbl.set(coeffToken(12, 2), "0000000001101");
        vbl.set(coeffToken(12, 3), "000000001100");
        vbl.set(coeffToken(13, 0), "0000000001011");
        vbl.set(coeffToken(13, 1), "0000000001010");
        vbl.set(coeffToken(13, 2), "0000000001001");
        vbl.set(coeffToken(13, 3), "0000000001100");
        vbl.set(coeffToken(14, 0), "0000000000111");
        vbl.set(coeffToken(14, 1), "00000000001011");
        vbl.set(coeffToken(14, 2), "0000000000110");
        vbl.set(coeffToken(14, 3), "0000000001000");
        vbl.set(coeffToken(15, 0), "00000000001001");
        vbl.set(coeffToken(15, 1), "00000000001000");
        vbl.set(coeffToken(15, 2), "00000000001010");
        vbl.set(coeffToken(15, 3), "0000000000001");
        vbl.set(coeffToken(16, 0), "00000000000111");
        vbl.set(coeffToken(16, 1), "00000000000110");
        vbl.set(coeffToken(16, 2), "00000000000101");
        vbl.set(coeffToken(16, 3), "00000000000100");
        CoeffToken[2] = CoeffToken[3] = vbl.getVLC();
    }

    static {
        final VLCBuilder vbl = new VLCBuilder();

        vbl.set(coeffToken(0, 0), "1111");
        vbl.set(coeffToken(1, 0), "001111");
        vbl.set(coeffToken(1, 1), "1110");
        vbl.set(coeffToken(2, 0), "001011");
        vbl.set(coeffToken(2, 1), "01111");
        vbl.set(coeffToken(2, 2), "1101");
        vbl.set(coeffToken(3, 0), "001000");
        vbl.set(coeffToken(3, 1), "01100");
        vbl.set(coeffToken(3, 2), "01110");
        vbl.set(coeffToken(3, 3), "1100");
        vbl.set(coeffToken(4, 0), "0001111");
        vbl.set(coeffToken(4, 1), "01010");
        vbl.set(coeffToken(4, 2), "01011");
        vbl.set(coeffToken(4, 3), "1011");
        vbl.set(coeffToken(5, 0), "0001011");
        vbl.set(coeffToken(5, 1), "01000");
        vbl.set(coeffToken(5, 2), "01001");
        vbl.set(coeffToken(5, 3), "1010");
        vbl.set(coeffToken(6, 0), "0001001");
        vbl.set(coeffToken(6, 1), "001110");
        vbl.set(coeffToken(6, 2), "001101");
        vbl.set(coeffToken(6, 3), "1001");
        vbl.set(coeffToken(7, 0), "0001000");
        vbl.set(coeffToken(7, 1), "001010");
        vbl.set(coeffToken(7, 2), "001001");
        vbl.set(coeffToken(7, 3), "1000");
        vbl.set(coeffToken(8, 0), "00001111");
        vbl.set(coeffToken(8, 1), "0001110");
        vbl.set(coeffToken(8, 2), "0001101");
        vbl.set(coeffToken(8, 3), "01101");
        vbl.set(coeffToken(9, 0), "00001011");
        vbl.set(coeffToken(9, 1), "00001110");
        vbl.set(coeffToken(9, 2), "0001010");
        vbl.set(coeffToken(9, 3), "001100");
        vbl.set(coeffToken(10, 0), "000001111");
        vbl.set(coeffToken(10, 1), "00001010");
        vbl.set(coeffToken(10, 2), "00001101");
        vbl.set(coeffToken(10, 3), "0001100");
        vbl.set(coeffToken(11, 0), "000001011");
        vbl.set(coeffToken(11, 1), "000001110");
        vbl.set(coeffToken(11, 2), "00001001");
        vbl.set(coeffToken(11, 3), "00001100");
        vbl.set(coeffToken(12, 0), "000001000");
        vbl.set(coeffToken(12, 1), "000001010");
        vbl.set(coeffToken(12, 2), "000001101");
        vbl.set(coeffToken(12, 3), "00001000");
        vbl.set(coeffToken(13, 0), "0000001101");
        vbl.set(coeffToken(13, 1), "000000111");
        vbl.set(coeffToken(13, 2), "000001001");
        vbl.set(coeffToken(13, 3), "000001100");
        vbl.set(coeffToken(14, 0), "0000001001");
        vbl.set(coeffToken(14, 1), "0000001100");
        vbl.set(coeffToken(14, 2), "0000001011");
        vbl.set(coeffToken(14, 3), "0000001010");
        vbl.set(coeffToken(15, 0), "0000000101");
        vbl.set(coeffToken(15, 1), "0000001000");
        vbl.set(coeffToken(15, 2), "0000000111");
        vbl.set(coeffToken(15, 3), "0000000110");
        vbl.set(coeffToken(16, 0), "0000000001");
        vbl.set(coeffToken(16, 1), "0000000100");
        vbl.set(coeffToken(16, 2), "0000000011");
        vbl.set(coeffToken(16, 3), "0000000010");
        CoeffToken[4] = CoeffToken[5] = CoeffToken[6] = CoeffToken[7] = vbl.getVLC();
    }

    static {
        final VLCBuilder vbl = new VLCBuilder();
        vbl.set(coeffToken(0, 0), "000011");
        vbl.set(coeffToken(1, 0), "000000");
        vbl.set(coeffToken(1, 1), "000001");
        vbl.set(coeffToken(2, 0), "000100");
        vbl.set(coeffToken(2, 1), "000101");
        vbl.set(coeffToken(2, 2), "000110");
        vbl.set(coeffToken(3, 0), "001000");
        vbl.set(coeffToken(3, 1), "001001");
        vbl.set(coeffToken(3, 2), "001010");
        vbl.set(coeffToken(3, 3), "001011");
        vbl.set(coeffToken(4, 0), "001100");
        vbl.set(coeffToken(4, 1), "001101");
        vbl.set(coeffToken(4, 2), "001110");
        vbl.set(coeffToken(4, 3), "001111");
        vbl.set(coeffToken(5, 0), "010000");
        vbl.set(coeffToken(5, 1), "010001");
        vbl.set(coeffToken(5, 2), "010010");
        vbl.set(coeffToken(5, 3), "010011");
        vbl.set(coeffToken(6, 0), "010100");
        vbl.set(coeffToken(6, 1), "010101");
        vbl.set(coeffToken(6, 2), "010110");
        vbl.set(coeffToken(6, 3), "010111");
        vbl.set(coeffToken(7, 0), "011000");
        vbl.set(coeffToken(7, 1), "011001");
        vbl.set(coeffToken(7, 2), "011010");
        vbl.set(coeffToken(7, 3), "011011");
        vbl.set(coeffToken(8, 0), "011100");
        vbl.set(coeffToken(8, 1), "011101");
        vbl.set(coeffToken(8, 2), "011110");
        vbl.set(coeffToken(8, 3), "011111");
        vbl.set(coeffToken(9, 0), "100000");
        vbl.set(coeffToken(9, 1), "100001");
        vbl.set(coeffToken(9, 2), "100010");
        vbl.set(coeffToken(9, 3), "100011");
        vbl.set(coeffToken(10, 0), "100100");
        vbl.set(coeffToken(10, 1), "100101");
        vbl.set(coeffToken(10, 2), "100110");
        vbl.set(coeffToken(10, 3), "100111");
        vbl.set(coeffToken(11, 0), "101000");
        vbl.set(coeffToken(11, 1), "101001");
        vbl.set(coeffToken(11, 2), "101010");
        vbl.set(coeffToken(11, 3), "101011");
        vbl.set(coeffToken(12, 0), "101100");
        vbl.set(coeffToken(12, 1), "101101");
        vbl.set(coeffToken(12, 2), "101110");
        vbl.set(coeffToken(12, 3), "101111");
        vbl.set(coeffToken(13, 0), "110000");
        vbl.set(coeffToken(13, 1), "110001");
        vbl.set(coeffToken(13, 2), "110010");
        vbl.set(coeffToken(13, 3), "110011");
        vbl.set(coeffToken(14, 0), "110100");
        vbl.set(coeffToken(14, 1), "110101");
        vbl.set(coeffToken(14, 2), "110110");
        vbl.set(coeffToken(14, 3), "110111");
        vbl.set(coeffToken(15, 0), "111000");
        vbl.set(coeffToken(15, 1), "111001");
        vbl.set(coeffToken(15, 2), "111010");
        vbl.set(coeffToken(15, 3), "111011");
        vbl.set(coeffToken(16, 0), "111100");
        vbl.set(coeffToken(16, 1), "111101");
        vbl.set(coeffToken(16, 2), "111110");
        vbl.set(coeffToken(16, 3), "111111");
        CoeffToken[8] = vbl.getVLC();
    }

    static {
        final VLCBuilder vbl = new VLCBuilder();
        vbl.set(coeffToken(0, 0), "01");
        vbl.set(coeffToken(1, 0), "000111");
        vbl.set(coeffToken(1, 1), "1");
        vbl.set(coeffToken(2, 0), "000100");
        vbl.set(coeffToken(2, 1), "000110");
        vbl.set(coeffToken(2, 2), "001");
        vbl.set(coeffToken(3, 0), "000011");
        vbl.set(coeffToken(3, 1), "0000011");
        vbl.set(coeffToken(3, 2), "0000010");
        vbl.set(coeffToken(3, 3), "000101");
        vbl.set(coeffToken(4, 0), "000010");
        vbl.set(coeffToken(4, 1), "00000011");
        vbl.set(coeffToken(4, 2), "00000010");
        vbl.set(coeffToken(4, 3), "0000000");
        coeffTokenChromaDCY420 = vbl.getVLC();
    }

    static {
        final VLCBuilder vbl = new VLCBuilder();
        vbl.set(coeffToken(0, 0), "1");
        vbl.set(coeffToken(1, 0), "0001111");
        vbl.set(coeffToken(1, 1), "01");
        vbl.set(coeffToken(2, 0), "0001110");
        vbl.set(coeffToken(2, 1), "0001101");
        vbl.set(coeffToken(2, 2), "001");
        vbl.set(coeffToken(3, 0), "000000111");
        vbl.set(coeffToken(3, 1), "0001100");
        vbl.set(coeffToken(3, 2), "0001011");
        vbl.set(coeffToken(3, 3), "00001");
        vbl.set(coeffToken(4, 0), "000000110");
        vbl.set(coeffToken(4, 1), "000000101");
        vbl.set(coeffToken(4, 2), "0001010");
        vbl.set(coeffToken(4, 3), "000001");
        vbl.set(coeffToken(5, 0), "0000000111");
        vbl.set(coeffToken(5, 1), "0000000110");
        vbl.set(coeffToken(5, 2), "000000100");
        vbl.set(coeffToken(5, 3), "0001001");
        vbl.set(coeffToken(6, 0), "00000000111");
        vbl.set(coeffToken(6, 1), "00000000110");
        vbl.set(coeffToken(6, 2), "0000000101");
        vbl.set(coeffToken(6, 3), "0001000");
        vbl.set(coeffToken(7, 0), "000000000111");
        vbl.set(coeffToken(7, 1), "000000000110");
        vbl.set(coeffToken(7, 2), "00000000101");
        vbl.set(coeffToken(7, 3), "0000000100");
        vbl.set(coeffToken(8, 0), "0000000000111");
        vbl.set(coeffToken(8, 1), "000000000101");
        vbl.set(coeffToken(8, 2), "000000000100");
        vbl.set(coeffToken(8, 3), "00000000100");
        coeffTokenChromaDCY422 = vbl.getVLC();
    }

    static {
        run = new VLC[]{new VLCBuilder().set(0, "1").set(1, "0").getVLC(),
            new VLCBuilder().set(0, "1").set(1, "01").set(2, "00").getVLC(),
            new VLCBuilder().set(0, "11").set(1, "10").set(2, "01").set(3, "00").getVLC(),
            new VLCBuilder().set(0, "11").set(1, "10").set(2, "01").set(3, "001").set(4, "000").getVLC(),
            new VLCBuilder().set(0, "11").set(1, "10").set(2, "011").set(3, "010").set(4, "001").set(5, "000")
                .getVLC(),
            new VLCBuilder().set(0, "11").set(1, "000").set(2, "001").set(3, "011").set(4, "010").set(5, "101")
                .set(6, "100").getVLC(),
            new VLCBuilder().set(0, "111").set(1, "110").set(2, "101").set(3, "100").set(4, "011").set(5, "010")
                .set(6, "001").set(7, "0001").set(8, "00001").set(9, "000001").set(10, "0000001")
                .set(11, "00000001").set(12, "000000001").set(13, "0000000001").set(14, "00000000001")
                .getVLC()};
    }

    public final static VLC[] totalZeros16 = {

        new VLCBuilder().set(0, "1").set(1, "011").set(2, "010").set(3, "0011").set(4, "0010").set(5, "00011")
            .set(6, "00010").set(7, "000011").set(8, "000010").set(9, "0000011").set(10, "0000010")
            .set(11, "00000011").set(12, "00000010").set(13, "000000011").set(14, "000000010")
            .set(15, "000000001").getVLC(),

        new VLCBuilder().set(0, "111").set(1, "110").set(2, "101").set(3, "100").set(4, "011").set(5, "0101")
            .set(6, "0100").set(7, "0011").set(8, "0010").set(9, "00011").set(10, "00010").set(11, "000011")
            .set(12, "000010").set(13, "000001").set(14, "000000").getVLC(),

        new VLCBuilder().set(0, "0101").set(1, "111").set(2, "110").set(3, "101").set(4, "0100").set(5, "0011")
            .set(6, "100").set(7, "011").set(8, "0010").set(9, "00011").set(10, "00010").set(11, "000001")
            .set(12, "00001").set(13, "000000").getVLC(),

        new VLCBuilder().set(0, "00011").set(1, "111").set(2, "0101").set(3, "0100").set(4, "110").set(5, "101")
            .set(6, "100").set(7, "0011").set(8, "011").set(9, "0010").set(10, "00010").set(11, "00001")
            .set(12, "00000").getVLC(),

        new VLCBuilder().set(0, "0101").set(1, "0100").set(2, "0011").set(3, "111").set(4, "110").set(5, "101")
            .set(6, "100").set(7, "011").set(8, "0010").set(9, "00001").set(10, "0001").set(11, "00000")
            .getVLC(),

        new VLCBuilder().set(0, "000001").set(1, "00001").set(2, "111").set(3, "110").set(4, "101").set(5, "100")
            .set(6, "011").set(7, "010").set(8, "0001").set(9, "001").set(10, "000000").getVLC(),

        new VLCBuilder().set(0, "000001").set(1, "00001").set(2, "101").set(3, "100").set(4, "011").set(5, "11")
            .set(6, "010").set(7, "0001").set(8, "001").set(9, "000000").getVLC(),

        new VLCBuilder().set(0, "000001").set(1, "0001").set(2, "00001").set(3, "011").set(4, "11").set(5, "10")
            .set(6, "010").set(7, "001").set(8, "000000").getVLC(),

        new VLCBuilder().set(0, "000001").set(1, "000000").set(2, "0001").set(3, "11").set(4, "10").set(5, "001")
            .set(6, "01").set(7, "00001").getVLC(),

        new VLCBuilder().set(0, "00001").set(1, "00000").set(2, "001").set(3, "11").set(4, "10").set(5, "01")
            .set(6, "0001").getVLC(),

        new VLCBuilder().set(0, "0000").set(1, "0001").set(2, "001").set(3, "010").set(4, "1").set(5, "011")
            .getVLC(),

        new VLCBuilder().set(0, "0000").set(1, "0001").set(2, "01").set(3, "1").set(4, "001").getVLC(),

        new VLCBuilder().set(0, "000").set(1, "001").set(2, "1").set(3, "01").getVLC(),

        new VLCBuilder().set(0, "00").set(1, "01").set(2, "1").getVLC(),

        new VLCBuilder().set(0, "0").set(1, "1").getVLC()};

    public final static VLC[] totalZeros4 = {
        new VLCBuilder().set(0, "1").set(1, "01").set(2, "001").set(3, "000").getVLC(),

        new VLCBuilder().set(0, "1").set(1, "01").set(2, "00").getVLC(),

        new VLCBuilder().set(0, "1").set(1, "0").getVLC()};

    public final static VLC[] totalZeros8 = {
        new VLCBuilder().set(0, "1").set(1, "010").set(2, "011").set(3, "0010").set(4, "0011").set(5, "0001")
            .set(6, "00001").set(7, "00000").getVLC(),

        new VLCBuilder().set(0, "000").set(1, "01").set(2, "001").set(3, "100").set(4, "101").set(5, "110")
            .set(6, "111").getVLC(),

        new VLCBuilder().set(0, "000").set(1, "001").set(2, "01").set(3, "10").set(4, "110").set(5, "111").getVLC(),

        new VLCBuilder().set(0, "110").set(1, "00").set(2, "01").set(3, "10").set(4, "111").getVLC(),

        new VLCBuilder().set(0, "00").set(1, "01").set(2, "10").set(3, "11").getVLC(),

        new VLCBuilder().set(0, "00").set(1, "01").set(2, "1").getVLC(),

        new VLCBuilder().set(0, "0").set(1, "1").getVLC()};

    public enum PartPred {
        L0, L1, Bi, Direct
    }

    public final static PartPred[][] bPredModes = {null, {PartPred.L0}, {PartPred.L1}, {PartPred.Bi},
        {PartPred.L0, PartPred.L0}, {PartPred.L0, PartPred.L0}, {PartPred.L1, PartPred.L1},
        {PartPred.L1, PartPred.L1}, {PartPred.L0, PartPred.L1}, {PartPred.L0, PartPred.L1},
        {PartPred.L1, PartPred.L0}, {PartPred.L1, PartPred.L0}, {PartPred.L0, PartPred.Bi},
        {PartPred.L0, PartPred.Bi}, {PartPred.L1, PartPred.Bi}, {PartPred.L1, PartPred.Bi},
        {PartPred.Bi, PartPred.L0}, {PartPred.Bi, PartPred.L0}, {PartPred.Bi, PartPred.L1},
        {PartPred.Bi, PartPred.L1}, {PartPred.Bi, PartPred.Bi}, {PartPred.Bi, PartPred.Bi}};

    public final static MBType[] bMbTypes = {MBType.B_Direct_16x16, MBType.B_L0_16x16, MBType.B_L1_16x16,
        MBType.B_Bi_16x16, MBType.B_L0_L0_16x8, MBType.B_L0_L0_8x16, MBType.B_L1_L1_16x8, MBType.B_L1_L1_8x16,
        MBType.B_L0_L1_16x8, MBType.B_L0_L1_8x16, MBType.B_L1_L0_16x8, MBType.B_L1_L0_8x16, MBType.B_L0_Bi_16x8,
        MBType.B_L0_Bi_8x16, MBType.B_L1_Bi_16x8, MBType.B_L1_Bi_8x16, MBType.B_Bi_L0_16x8, MBType.B_Bi_L0_8x16,
        MBType.B_Bi_L1_16x8, MBType.B_Bi_L1_8x16, MBType.B_Bi_Bi_16x8, MBType.B_Bi_Bi_8x16, MBType.B_8x8};

    public final static int[] BLK_X = new int[]{0, 4, 0, 4, 8, 12, 8, 12, 0, 4, 0, 4, 8, 12, 8, 12};
    public final static int[] BLK_Y = new int[]{0, 0, 4, 4, 0, 0, 4, 4, 8, 8, 12, 12, 8, 8, 12, 12};

    public final static int[] BLK_8x8_X = new int[]{0, 8, 0, 8};
    public final static int[] BLK_8x8_Y = new int[]{0, 0, 8, 8};

    public final static int[] BLK_DISP_MAP = {0, 1, 4, 5, 2, 3, 6, 7, 8, 9, 12, 13, 10, 11, 14, 15};

    public final static int[] MB_DISP_OFF_LEFT = new int[]{0, 1, 0, 1, 2, 3, 2, 3, 0, 1, 0, 1, 2, 3, 2, 3};
    public final static int[] MB_DISP_OFF_TOP = new int[]{0, 0, 1, 1, 0, 0, 1, 1, 2, 2, 3, 3, 2, 2, 3, 3};

    public final static int[] QP_SCALE_CR = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
        21, 22, 23, 24, 25, 26, 27, 28, 29, 29, 30, 31, 32, 32, 33, 34, 34, 35, 35, 36, 36, 37, 37, 37, 38, 38, 38,
        39, 39, 39, 39};

    public static final Picture NO_PIC = Picture.createPicture(0, 0, null, null);
    public static final int[] BLK_8x8_MB_OFF_LUMA = {0, 8, 128, 136};
    public static final int[] BLK_8x8_MB_OFF_CHROMA = {0, 4, 32, 36};
    public static final int[] BLK_4x4_MB_OFF_LUMA = {0, 4, 8, 12, 64, 68, 72, 76, 128, 132, 136, 140, 192, 196, 200,
        204};
    public static final int[] BLK_8x8_IND = {0, 0, 1, 1, 0, 0, 1, 1, 2, 2, 3, 3, 2, 2, 3, 3};
    public static final int[][] BLK8x8_BLOCKS = {{0, 1, 4, 5}, {2, 3, 6, 7}, {8, 9, 12, 13},
        {10, 11, 14, 15}};
    public static final int[][] ARRAY = {{0}, {1}, {2}, {3}};

    public static final int[] CODED_BLOCK_PATTERN_INTRA_COLOR = new int[]{47, 31, 15, 0, 23, 27, 29, 30, 7, 11, 13,
        14, 39, 43, 45, 46, 16, 3, 5, 10, 12, 19, 21, 26, 28, 35, 37, 42, 44, 1, 2, 4, 8, 17, 18, 20, 24, 6, 9, 22,
        25, 32, 33, 34, 36, 40, 38, 41};

    public static final int[] CODED_BLOCK_PATTERN_INTRA_COLOR_INV = new int[]{3, 29, 30, 17, 31, 18, 37, 8, 32, 38,
        19, 9, 20, 10, 11, 2, 16, 33, 34, 21, 35, 22, 39, 4, 36, 40, 23, 5, 24, 6, 7, 1, 41, 42, 43, 25, 44, 26, 46,
        12, 45, 47, 27, 13, 28, 14, 15, 0
    };

    public static final int[] CODED_BLOCK_PATTERN_INTER_COLOR = new int[]{0, 16, 1, 2, 4, 8, 32, 3, 5, 10, 12, 15, 47,
        7, 11, 13, 14, 6, 9, 31, 35, 37, 42, 44, 33, 34, 36, 40, 39, 43, 45, 46, 17, 18, 20, 24, 19, 21, 26, 28, 23,
        27, 29, 30, 22, 25, 38, 41};

    private static int[] inverse(final int[] arr) {
        final int[] inv = new int[arr.length];
        for (int i = 0; i < inv.length; i++) {
            inv[arr[i]] = i;
        }
        return inv;
    }

    public static final int[] CODED_BLOCK_PATTERN_INTER_COLOR_INV = inverse(CODED_BLOCK_PATTERN_INTER_COLOR);

    public static final int[] sig_coeff_map_8x8 = {0, 1, 2, 3, 4, 5, 5, 4, 4, 3, 3, 4, 4, 4, 5, 5, 4, 4, 4, 4, 3, 3, 6,
        7, 7, 7, 8, 9, 10, 9, 8, 7, 7, 6, 11, 12, 13, 11, 6, 7, 8, 9, 14, 10, 9, 8, 6, 11, 12, 13, 11, 6, 9, 14, 10,
        9, 11, 12, 13, 11, 14, 10, 12};

    public static final int[] last_sig_coeff_map_8x8 = {0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2,
        2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 6, 6, 6, 6, 7,
        7, 7, 7, 8, 8, 8};

    public static final int[] identityMapping16 = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
    public static final int[] identityMapping4 = {0, 1, 2, 3};
    public static final PartPred[] bPartPredModes = {Direct, L0, L1, Bi, L0, L0, L1, L1, Bi, Bi, L0, L1, Bi};
    public static final int[] bSubMbTypes = {0, 0, 0, 0, 1, 2, 1, 2, 1, 2, 3, 3, 3};

    public static final int[] LUMA_4x4_BLOCK_LUT = new int[256];
    public static final int[] LUMA_4x4_POS_LUT = new int[256];
    public static final int[] LUMA_8x8_BLOCK_LUT = new int[256];
    public static final int[] LUMA_8x8_POS_LUT = new int[256];
    public static final int[] CHROMA_BLOCK_LUT = new int[64];
    public static final int[] CHROMA_POS_LUT = new int[64];

    public static final int[][] COMP_BLOCK_4x4_LUT = {LUMA_4x4_BLOCK_LUT, CHROMA_BLOCK_LUT, CHROMA_BLOCK_LUT};
    public static final int[][] COMP_POS_4x4_LUT = {LUMA_4x4_POS_LUT, CHROMA_POS_LUT, CHROMA_POS_LUT};

    public static final int[][] COMP_BLOCK_8x8_LUT = {LUMA_8x8_BLOCK_LUT, CHROMA_BLOCK_LUT, CHROMA_BLOCK_LUT};
    public static final int[][] COMP_POS_8x8_LUT = {LUMA_8x8_POS_LUT, CHROMA_POS_LUT, CHROMA_POS_LUT};

    static {
        int[] tmp = new int[16];

        for (int blk = 0; blk < 16; blk++) {
            for (int i = 0; i < 16; i++) {
                tmp[i] = i;
            }
            putBlk(tmp, BLK_X[blk], BLK_Y[blk], 4, 4, 16, LUMA_4x4_POS_LUT);
            Arrays.fill(tmp, blk);
            putBlk(tmp, BLK_X[blk], BLK_Y[blk], 4, 4, 16, LUMA_4x4_BLOCK_LUT);
        }
        for (int blk = 0; blk < 4; blk++) {
            for (int i = 0; i < 16; i++) {
                tmp[i] = i;
            }
            putBlk(tmp, BLK_X[blk], BLK_Y[blk], 4, 4, 8, CHROMA_POS_LUT);
            Arrays.fill(tmp, blk);
            putBlk(tmp, BLK_X[blk], BLK_Y[blk], 4, 4, 8, CHROMA_BLOCK_LUT);
        }
        tmp = new int[64];
        for (int blk = 0; blk < 4; blk++) {
            for (int i = 0; i < 64; i++) {
                tmp[i] = i;
            }
            putBlk(tmp, BLK_8x8_X[blk], BLK_8x8_Y[blk], 8, 8, 16, LUMA_8x8_POS_LUT);
            Arrays.fill(tmp, blk);
            putBlk(tmp, BLK_8x8_X[blk], BLK_8x8_Y[blk], 8, 8, 16, LUMA_8x8_BLOCK_LUT);
        }
    }

    private static void putBlk(final int[] _in, final int blkX, final int blkY, final int blkW, final int blkH, final int stride, final int[] out) {
        for (int line = 0, srcOff = 0, dstOff = blkY * stride + blkX; line < blkH; line++) {
            if (blkW > 0)
                System.arraycopy(_in, srcOff, out, dstOff, blkW);
            srcOff += blkW;
            dstOff += stride;
        }
    }

    private static int[][] buildPixSplitMap4x4() {
        final int[][] result = new int[][]{{0, 1, 2, 3, 16, 17, 18, 19, 32, 33, 34, 35, 48, 49, 50, 51}, new int[16],
            new int[16], new int[16], new int[16], new int[16], new int[16], new int[16], new int[16], new int[16],
            new int[16], new int[16], new int[16], new int[16], new int[16], new int[16]};
        for (int blkY = 0, blk = 0, off = 0; blkY < 4; ++blkY) {
            for (int blkX = 0; blkX < 4; ++blkX, ++blk, off += 4) {
                for (int i = 0; i < 16; i++)
                    result[blk][i] = result[0][i] + off;
            }
            off += 48;
        }
        return result;
    }

    private static int[][] buildPixSplitMap2x2() {
        final int[][] result = new int[][]{{0, 1, 2, 3, 8, 9, 10, 11, 16, 17, 18, 19, 24, 25, 26, 27}, new int[16],
            new int[16], new int[16]};
        for (int blkY = 0, blk = 0, off = 0; blkY < 2; ++blkY) {
            for (int blkX = 0; blkX < 2; ++blkX, ++blk, off += 4) {
                for (int i = 0; i < 16; i++)
                    result[blk][i] = result[0][i] + off;
            }
            off += 24;
        }
        return result;
    }

    public static boolean usesList(final PartPred pred, final int l) {
        return pred == Bi || (pred == L0 && l == 0 || pred == L1 && l == 1);
    }

    public static int coeffToken(final int totalCoeff, final int trailingOnes) {
        return (totalCoeff << 4) | trailingOnes;
    }

    public static final int[][] PIX_MAP_SPLIT_4x4 = buildPixSplitMap4x4();
    public static final int[][] PIX_MAP_SPLIT_2x2 = buildPixSplitMap2x2();

    public static final int PROFILE_CAVLC_INTRA = 44;
    public static final int PROFILE_BASELINE = 66;
    public static final int PROFILE_MAIN = 77;
    public static final int PROFILE_EXTENDED = 88;
    public static final int PROFILE_HIGH = 100;
    public static final int PROFILE_HIGH_10 = 110;
    public static final int PROFILE_HIGH_422 = 122;
    public static final int PROFILE_HIGH_444 = 244;

    public static final int[] defaultScalingList4x4Intra = {6, 13, 13, 20, 20, 20, 28, 28, 28, 28, 32, 32, 32, 37, 37,
        42};
    public static final int[] defaultScalingList4x4Inter = {10, 14, 14, 20, 20, 20, 24, 24, 24, 24, 27, 27, 27, 30, 30,
        34};
    public static final int[] defaultScalingList8x8Intra = {6, 10, 10, 13, 11, 13, 16, 16, 16, 16, 18, 18, 18, 18, 18,
        23, 23, 23, 23, 23, 23, 25, 25, 25, 25, 25, 25, 25, 27, 27, 27, 27, 27, 27, 27, 27, 29, 29, 29, 29, 29, 29,
        29, 31, 31, 31, 31, 31, 31, 33, 33, 33, 33, 33, 36, 36, 36, 36, 38, 38, 38, 40, 40, 42};
    public static final int[] defaultScalingList8x8Inter = {9, 13, 13, 15, 13, 15, 17, 17, 17, 17, 19, 19, 19, 19, 19,
        21, 21, 21, 21, 21, 21, 22, 22, 22, 22, 22, 22, 22, 24, 24, 24, 24, 24, 24, 24, 24, 25, 25, 25, 25, 25, 25,
        25, 27, 27, 27, 27, 27, 27, 28, 28, 28, 28, 28, 30, 30, 30, 30, 32, 32, 32, 33, 33, 35};
}
