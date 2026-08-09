/* SPDX-License-Identifier: BSD 2-Clause "Simplified" License */

package li.cil.oc2.jcodec.scale;

import li.cil.oc2.jcodec.common.model.Picture;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 *
 * @author The JCodec project
 */
public interface Transform {
    void transform(Picture src, Picture dst);
}
