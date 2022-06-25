/* SPDX-License-Identifier: BSD 2-Clause "Simplified" License */

package li.cil.oc2.jcodec.codecs.h264.io.model;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * <p>
 * Aspect ratio
 * <p>
 * dynamic enum
 *
 * @author The JCodec project
 */
public record AspectRatio(int value) {
    public static final AspectRatio Extended_SAR = new AspectRatio(255);

    public static AspectRatio fromValue(final int value) {
        if (value == Extended_SAR.value) {
            return Extended_SAR;
        }
        return new AspectRatio(value);
    }
}
