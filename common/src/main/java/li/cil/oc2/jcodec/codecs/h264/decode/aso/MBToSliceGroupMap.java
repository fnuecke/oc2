/* SPDX-License-Identifier: BSD 2-Clause "Simplified" License */

package li.cil.oc2.jcodec.codecs.h264.decode.aso;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * <p>
 * Contains a mapping of macroblocks to slice groups. Groups is an array of
 * group slice group indices having a dimension picWidthInMbs x picHeightInMbs
 *
 * @author The JCodec project
 */
public record MBToSliceGroupMap(int[] groups, int[] indices, int[][] inverse) { }
