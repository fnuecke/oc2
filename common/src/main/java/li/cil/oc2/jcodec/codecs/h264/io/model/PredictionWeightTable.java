/* SPDX-License-Identifier: BSD 2-Clause "Simplified" License */

package li.cil.oc2.jcodec.codecs.h264.io.model;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
public final class PredictionWeightTable {
    // luma_log2_weight_denom
    public int lumaLog2WeightDenom;
    // chroma_log2_weight_denom
    public int chromaLog2WeightDenom;

    // luma_weight
    public final int[][] lumaWeight = new int[2][];
    // chroma_weight
    public final int[][][] chromaWeight = new int[2][][];

    // luma_offset
    public final int[][] lumaOffset = new int[2][];
    // chroma_offset
    public final int[][][] chromaOffset = new int[2][][];
}
