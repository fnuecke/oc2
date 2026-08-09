/* SPDX-License-Identifier: BSD 2-Clause "Simplified" License */

package li.cil.oc2.jcodec.codecs.h264.io.model;

import java.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * <p>
 * Network abstraction layer (NAL) unit
 *
 * @author The JCodec project
 */
public final class NALUnit {
    public final NALUnitType type;
    public final int nal_ref_idc;

    public NALUnit(final NALUnitType type, final int nal_ref_idc) {
        this.type = type;
        this.nal_ref_idc = nal_ref_idc;
    }

    public static NALUnit read(final ByteBuffer _in) {
        final int nalu = _in.get() & 0xff;
        final int nal_ref_idc = (nalu >> 5) & 0x3;
        final int nb = nalu & 0x1f;

        final NALUnitType type = NALUnitType.fromValue(nb);
        return new NALUnit(type, nal_ref_idc);
    }

    public void write(final ByteBuffer out) {
        final int nalu = type.getValue() | (nal_ref_idc << 5);
        out.put((byte) nalu);
    }
}
