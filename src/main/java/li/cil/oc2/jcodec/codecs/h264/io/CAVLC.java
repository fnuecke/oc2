/* SPDX-License-Identifier: BSD 2-Clause "Simplified" License */

package li.cil.oc2.jcodec.codecs.h264.io;

import li.cil.oc2.jcodec.codecs.h264.H264Const;
import li.cil.oc2.jcodec.codecs.h264.decode.CAVLCReader;
import li.cil.oc2.jcodec.codecs.h264.io.model.MBType;
import li.cil.oc2.jcodec.codecs.h264.io.model.SeqParameterSet;
import li.cil.oc2.jcodec.common.io.BitReader;
import li.cil.oc2.jcodec.common.io.BitWriter;
import li.cil.oc2.jcodec.common.io.VLC;
import li.cil.oc2.jcodec.common.model.ColorSpace;
import li.cil.oc2.jcodec.common.tools.MathUtil;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * <p>
 * Non-CABAC H.264 symbols read/write routines
 *
 * @author Jay Codec
 */
public final class CAVLC {
    private final ColorSpace color;
    private final VLC chromaDCVLC;

    private final int[] tokensLeft;
    private final int[] tokensTop;

    private final int mbWidth;
    private final int mbMask;
    private final int mbW;
    private final int mbH;

    public CAVLC(final SeqParameterSet sps, final int mbW, final int mbH) {
        this(sps.chromaFormatIdc, sps.picWidthInMbsMinus1 + 1, mbW, mbH);
    }

    private CAVLC(final ColorSpace color, final int mbWidth, final int mbW, final int mbH) {
        this.color = color;
        this.chromaDCVLC = codeTableChromaDC();
        this.mbWidth = mbWidth;
        this.mbMask = (1 << mbH) - 1;
        this.mbW = mbW;
        this.mbH = mbH;

        tokensLeft = new int[4];
        tokensTop = new int[mbWidth << mbW];
    }

    public CAVLC fork() {
        final CAVLC ret = new CAVLC(color, mbWidth, mbW, mbH);
        System.arraycopy(tokensLeft, 0, ret.tokensLeft, 0, tokensLeft.length);
        System.arraycopy(tokensTop, 0, ret.tokensTop, 0, tokensTop.length);
        return ret;
    }

    public int writeACBlock(final BitWriter out, final int blkIndX, final int blkIndY, final MBType leftMBType, final MBType topMBType, final int[] coeff,
                            final VLC[] totalZerosTab, final int firstCoeff, final int maxCoeff, final int[] scan) {
        final VLC coeffTokenTab = getCoeffTokenVLCForLuma(blkIndX != 0, leftMBType, tokensLeft[blkIndY & mbMask],
            blkIndY != 0, topMBType, tokensTop[blkIndX]);

        final int coeffToken = writeBlockGen(out, coeff, totalZerosTab, firstCoeff, maxCoeff, scan, coeffTokenTab);

        tokensLeft[blkIndY & mbMask] = coeffToken;
        tokensTop[blkIndX] = coeffToken;

        return coeffToken;
    }

    public void writeChrDCBlock(final BitWriter out, final int[] coeff, final VLC[] totalZerosTab, final int firstCoeff, final int maxCoeff,
                                final int[] scan) {
        writeBlockGen(out, coeff, totalZerosTab, firstCoeff, maxCoeff, scan, getCoeffTokenVLCForChromaDC());
    }

    public void writeLumaDCBlock(final BitWriter out, final int blkIndX, final int blkIndY, final MBType leftMBType, final MBType topMBType,
                                 final int[] coeff, final VLC[] totalZerosTab, final int firstCoeff, final int maxCoeff, final int[] scan) {
        final VLC coeffTokenTab = getCoeffTokenVLCForLuma(blkIndX != 0, leftMBType, tokensLeft[blkIndY & mbMask],
            blkIndY != 0, topMBType, tokensTop[blkIndX]);

        writeBlockGen(out, coeff, totalZerosTab, firstCoeff, maxCoeff, scan, coeffTokenTab);
    }

    private int writeBlockGen(final BitWriter out, final int[] coeff, final VLC[] totalZerosTab, final int firstCoeff, final int maxCoeff,
                              final int[] scan, final VLC coeffTokenTab) {
        int trailingOnes, totalCoeff = 0, totalZeros = 0;
        final int[] runBefore = new int[maxCoeff];
        final int[] levels = new int[maxCoeff];
        for (int i = 0; i < maxCoeff; i++) {
            final int c = coeff[scan[i + firstCoeff]];
            if (c == 0) {
                runBefore[totalCoeff]++;
                totalZeros++;
            } else {
                levels[totalCoeff++] = c;
            }
        }
        if (totalCoeff < maxCoeff)
            totalZeros -= runBefore[totalCoeff];

        for (trailingOnes = 0; trailingOnes < totalCoeff && trailingOnes < 3
            && Math.abs(levels[totalCoeff - trailingOnes - 1]) == 1; trailingOnes++) { }

        final int coeffToken = H264Const.coeffToken(totalCoeff, trailingOnes);

        coeffTokenTab.writeVLC(out, coeffToken);

        if (totalCoeff > 0) {
            writeTrailingOnes(out, levels, totalCoeff, trailingOnes);
            writeLevels(out, levels, totalCoeff, trailingOnes);

            if (totalCoeff < maxCoeff) {
                totalZerosTab[totalCoeff - 1].writeVLC(out, totalZeros);
                writeRuns(out, runBefore, totalCoeff, totalZeros);
            }
        }
        return coeffToken;
    }

    private void writeTrailingOnes(final BitWriter out, final int[] levels, final int totalCoeff, final int trailingOne) {
        for (int i = totalCoeff - 1; i >= totalCoeff - trailingOne; i--) {
            out.write1Bit(levels[i] >>> 31);
        }
    }

    private void writeLevels(final BitWriter out, final int[] levels, final int totalCoeff, final int trailingOnes) {

        int suffixLen = totalCoeff > 10 && trailingOnes < 3 ? 1 : 0;
        for (int i = totalCoeff - trailingOnes - 1; i >= 0; i--) {
            int absLev = unsigned(levels[i]);
            if (i == totalCoeff - trailingOnes - 1 && trailingOnes < 3)
                absLev -= 2;

            final int prefix = absLev >> suffixLen;
            if (suffixLen == 0 && prefix < 14 || suffixLen > 0 && prefix < 15) {
                out.writeNBit(1, prefix + 1);
                out.writeNBit(absLev, suffixLen);
            } else if (suffixLen == 0 && absLev < 30) {
                out.writeNBit(1, 15);
                out.writeNBit(absLev - 14, 4);
            } else {
                if (suffixLen == 0)
                    absLev -= 15;
                int len, code;
                for (len = 12; (code = absLev - (len + 3 << suffixLen) - (1 << len) + 4096) >= (1 << len); len++) { }
                out.writeNBit(1, len + 4);
                out.writeNBit(code, len);
            }
            if (suffixLen == 0)
                suffixLen = 1;
            if (MathUtil.abs(levels[i]) > (3 << (suffixLen - 1)) && suffixLen < 6)
                suffixLen++;
        }
    }

    private int unsigned(final int signed) {
        final int sign = signed >>> 31;
        final int s = signed >> 31;

        return (((signed ^ s) - s) << 1) + sign - 2;
    }

    private void writeRuns(final BitWriter out, final int[] run, final int totalCoeff, int totalZeros) {
        for (int i = totalCoeff - 1; i > 0 && totalZeros > 0; i--) {
            H264Const.run[Math.min(6, totalZeros - 1)].writeVLC(out, run[i]);
            totalZeros -= run[i];
        }
    }

    public VLC getCoeffTokenVLCForLuma(final boolean leftAvailable, final MBType leftMBType, final int leftToken, final boolean topAvailable,
                                       final MBType topMBType, final int topToken) {

        final int nc = codeTableLuma(leftAvailable, leftMBType, leftToken, topAvailable, topMBType, topToken);

        return H264Const.CoeffToken[Math.min(nc, 8)];
    }

    public VLC getCoeffTokenVLCForChromaDC() {
        return chromaDCVLC;
    }

    private int codeTableLuma(final boolean leftAvailable, final MBType leftMBType, final int leftToken, final boolean topAvailable,
                              final MBType topMBType, final int topToken) {

        final int nA = leftMBType == null ? 0 : totalCoeff(leftToken);
        final int nB = topMBType == null ? 0 : totalCoeff(topToken);

        if (leftAvailable && topAvailable)
            return (nA + nB + 1) >> 1;
        else if (leftAvailable)
            return nA;
        else if (topAvailable)
            return nB;
        else
            return 0;
    }

    private VLC codeTableChromaDC() {
        if (color == ColorSpace.YUV420J) {
            return H264Const.coeffTokenChromaDCY420;
        } else if (color == ColorSpace.YUV422) {
            return H264Const.coeffTokenChromaDCY422;
        } else if (color == ColorSpace.YUV444) {
            return H264Const.CoeffToken[0];
        }
        return null;
    }

    public int readCoeffs(final BitReader _in, final VLC coeffTokenTab, final int[] coeffLevel, final int firstCoeff,
                          final int nCoeff, final int[] zigzag) {
        final int coeffToken = coeffTokenTab.readVLC(_in);
        final int totalCoeff = totalCoeff(coeffToken);
        final int trailingOnes = trailingOnes(coeffToken);

        if (totalCoeff > 0) {
            int suffixLength = totalCoeff > 10 && trailingOnes < 3 ? 1 : 0;

            final int[] level = new int[totalCoeff];
            int i;
            for (i = 0; i < trailingOnes; i++) {
                final int read1Bit = _in.read1Bit();
                level[i] = 1 - 2 * read1Bit;
            }

            for (; i < totalCoeff; i++) {
                final int level_prefix = CAVLCReader.readZeroBitCount(_in);
                int levelSuffixSize = suffixLength;
                if (level_prefix == 14 && suffixLength == 0)
                    levelSuffixSize = 4;
                if (level_prefix >= 15)
                    levelSuffixSize = level_prefix - 3;

                int levelCode = (Math.min(15, level_prefix) << suffixLength);
                if (levelSuffixSize > 0) {
                    final int level_suffix = CAVLCReader.readU(_in, levelSuffixSize); // RB: level_suffix
                    levelCode += level_suffix;
                }
                if (level_prefix >= 15 && suffixLength == 0)
                    levelCode += 15;
                if (level_prefix >= 16)
                    levelCode += (1 << (level_prefix - 3)) - 4096;
                if (i == trailingOnes && trailingOnes < 3)
                    levelCode += 2;

                if (levelCode % 2 == 0)
                    level[i] = (levelCode + 2) >> 1;
                else
                    level[i] = (-levelCode - 1) >> 1;

                if (suffixLength == 0)
                    suffixLength = 1;
                if (Math.abs(level[i]) > (3 << (suffixLength - 1)) && suffixLength < 6)
                    suffixLength++;
            }

            int zerosLeft;
            if (totalCoeff < nCoeff) {
                if (coeffLevel.length == 4) {
                    zerosLeft = H264Const.totalZeros4[totalCoeff - 1].readVLC(_in);
                } else if (coeffLevel.length == 8) {
                    zerosLeft = H264Const.totalZeros8[totalCoeff - 1].readVLC(_in);
                } else {
                    zerosLeft = H264Const.totalZeros16[totalCoeff - 1].readVLC(_in);
                }
            } else
                zerosLeft = 0;

            final int[] runs = new int[totalCoeff];
            int r;
            for (r = 0; r < totalCoeff - 1 && zerosLeft > 0; r++) {
                final int run = H264Const.run[Math.min(6, zerosLeft - 1)].readVLC(_in);
                zerosLeft -= run;
                runs[r] = run;
            }
            runs[r] = zerosLeft;

            for (int j = totalCoeff - 1, cn = 0; j >= 0 && cn < nCoeff; j--, cn++) {
                cn += runs[j];
                coeffLevel[zigzag[cn + firstCoeff]] = level[j];
            }
        }

        return coeffToken;
    }

    public static int totalCoeff(final int coeffToken) {
        return coeffToken >> 4;
    }

    public static int trailingOnes(final int coeffToken) {
        return coeffToken & 0xf;
    }

    public static final int[] NO_ZIGZAG = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};

    public void readChromaDCBlock(final BitReader reader, final int[] coeff) {
        final VLC coeffTokenTab = getCoeffTokenVLCForChromaDC();

        readCoeffs(reader, coeffTokenTab, coeff, 0, coeff.length,
            NO_ZIGZAG);
    }

    public void readLumaDCBlock(final BitReader reader, final int[] coeff, final int mbX, final boolean leftAvailable, final MBType leftMbType,
                                final boolean topAvailable, final MBType topMbType, final int[] zigzag4x4) {
        final VLC coeffTokenTab = getCoeffTokenVLCForLuma(leftAvailable, leftMbType, tokensLeft[0], topAvailable, topMbType,
            tokensTop[mbX << 2]);

        readCoeffs(reader, coeffTokenTab, coeff, 0, 16, zigzag4x4);
    }

    public int readACBlock(final BitReader reader, final int[] coeff, final int blkIndX, final int blkIndY, final boolean leftAvailable,
                           final MBType leftMbType, final boolean topAvailable, final MBType topMbType, final int firstCoeff, final int nCoeff, final int[] zigzag4x4) {
        final VLC coeffTokenTab = getCoeffTokenVLCForLuma(leftAvailable, leftMbType, tokensLeft[blkIndY & mbMask],
            topAvailable, topMbType, tokensTop[blkIndX]);

        final int readCoeffs = readCoeffs(reader, coeffTokenTab, coeff, firstCoeff, nCoeff, zigzag4x4);
        tokensLeft[blkIndY & mbMask] = tokensTop[blkIndX] = readCoeffs;

        return totalCoeff(readCoeffs);
    }

    public void setZeroCoeff(final int blkIndX, final int blkIndY) {
        tokensLeft[blkIndY & mbMask] = tokensTop[blkIndX] = 0;
    }
}
