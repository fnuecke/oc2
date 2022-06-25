/* SPDX-License-Identifier: BSD 2-Clause "Simplified" License */

package li.cil.oc2.jcodec.codecs.h264;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
public final class H264Utils {
    public static ByteBuffer nextNALUnit(final ByteBuffer buf) {
        skipToNALUnit(buf);

        if (buf.hasArray())
            return gotoNALUnitWithArray(buf);
        else
            return gotoNALUnit(buf);
    }

    public static void skipToNALUnit(final ByteBuffer buf) {
        if (!buf.hasRemaining())
            return;

        int val = 0xffffffff;
        while (buf.hasRemaining()) {
            val <<= 8;
            val |= (buf.get() & 0xff);
            if ((val & 0xffffff) == 1) {
                buf.position(buf.position());
                break;
            }
        }
    }

    /**
     * Finds next Nth H.264 bitstream NAL unit (0x00000001) and returns the data
     * that precedes it as a ByteBuffer slice
     * <p>
     * Segment byte order is always little endian
     */
    public static ByteBuffer gotoNALUnit(final ByteBuffer buf) {
        if (!buf.hasRemaining())
            return null;

        final int from = buf.position();
        final ByteBuffer result = buf.slice();
        result.order(ByteOrder.BIG_ENDIAN);

        int val = 0xffffffff;
        while (buf.hasRemaining()) {
            val <<= 8;
            val |= (buf.get() & 0xff);
            if ((val & 0xffffff) == 1) {
                buf.position(buf.position() - (val == 1 ? 4 : 3));
                result.limit(buf.position() - from);
                break;
            }
        }
        return result;
    }

    /**
     * Finds next Nth H.264 bitstream NAL unit (0x00000001) and returns the data
     * that precedes it as a ByteBuffer slice
     * <p>
     * Segment byte order is always little endian
     */
    public static ByteBuffer gotoNALUnitWithArray(final ByteBuffer buf) {

        if (!buf.hasRemaining())
            return null;

        final int from = buf.position();
        final ByteBuffer result = buf.slice();
        result.order(ByteOrder.BIG_ENDIAN);

        final byte[] arr = buf.array();
        int pos = from + buf.arrayOffset();
        final int posFrom = pos;
        final int lim = buf.limit() + buf.arrayOffset();

        while (pos < lim) {
            byte b = arr[pos];

            if ((b & 254) == 0) {
                while (b == 0 && ++pos < lim)
                    b = arr[pos];

                if (b == 1) {
                    if (pos - posFrom >= 2 && arr[pos - 1] == 0 && arr[pos - 2] == 0) {
                        final int lenSize = (pos - posFrom >= 3 && arr[pos - 3] == 0) ? 4 : 3;

                        buf.position(pos + 1 - buf.arrayOffset() - lenSize);
                        result.limit(buf.position() - from);

                        return result;
                    }
                }
            }

            pos += 3;
        }

        buf.position(buf.limit());

        return result;
    }

    public static void unescapeNAL(final ByteBuffer _buf) {
        if (_buf.remaining() < 2)
            return;
        final ByteBuffer _in = _buf.duplicate();
        final ByteBuffer out = _buf.duplicate();
        byte p1 = _in.get();
        out.put(p1);
        byte p2 = _in.get();
        out.put(p2);
        while (_in.hasRemaining()) {
            final byte b = _in.get();
            if (p1 != 0 || p2 != 0 || b != 3)
                out.put(b);
            p1 = p2;
            p2 = b;
        }
        _buf.limit(out.position());
    }

    public static void escapeNAL(final ByteBuffer src, final ByteBuffer dst) {
        byte p1 = src.get(), p2 = src.get();
        dst.put(p1);
        dst.put(p2);
        while (src.hasRemaining()) {
            final byte b = src.get();
            if (p1 == 0 && p2 == 0 && (b & 0xff) <= 3) {
                dst.put((byte) 3);
                p2 = 3;
            }
            dst.put(b);
            p1 = p2;
            p2 = b;
        }
    }

    public static List<ByteBuffer> splitFrame(final ByteBuffer frame) {
        final ArrayList<ByteBuffer> result = new ArrayList<>();

        ByteBuffer segment;
        while ((segment = H264Utils.nextNALUnit(frame)) != null) {
            result.add(segment);
        }

        return result;
    }

    public static int golomb2Signed(int val) {
        final int sign = ((val & 0x1) << 1) - 1;
        val = ((val >> 1) + (val & 0x1)) * sign;
        return val;
    }

    /**
     * A collection of functions to work with a compact representation of a motion vector.
     * <p>
     * Motion vector is represented as long:
     * <p>
     * <pre>||rrrrrr|vvvvvvvvvvvv|hhhhhhhhhhhhhh||</pre>
     */
    public static final class Mv {
        public static int mvX(final int mv) {
            return (mv << 18) >> 18;
        }

        public static int mvY(final int mv) {
            return ((mv << 6) >> 20);
        }

        public static int mvRef(final int mv) {
            return (mv >> 26);
        }

        public static int packMv(final int mvx, final int mvy, final int r) {
            return ((r & 0x3f) << 26) | ((mvy & 0xfff) << 14) | (mvx & 0x3fff);
        }

        public static int mvC(final int mv, final int comp) {
            return comp == 0 ? mvX(mv) : mvY(mv);
        }
    }

    /**
     * A collection of functions to work with a compact representation of a
     * motion vector list.
     * <p>
     * Motion vector list contains interleaved pairs of forward and backward
     * motion vectors packed into integers.
     */
    public static class MvList {
        private final int[] list;
        private static final int NA = Mv.packMv(0, 0, -1);

        public MvList(final int size) {
            list = new int[size << 1];
            clear();
        }

        public void clear() {
            for (int i = 0; i < list.length; i += 2) {
                list[i] = list[i + 1] = NA;
            }
        }

        public int mv0X(final int off) {
            return Mv.mvX(list[off << 1]);
        }

        public int mv0Y(final int off) {
            return Mv.mvY(list[off << 1]);
        }

        public int mv0R(final int off) {
            return Mv.mvRef(list[off << 1]);
        }

        public int mv1X(final int off) {
            return Mv.mvX(list[(off << 1) + 1]);
        }

        public int mv1Y(final int off) {
            return Mv.mvY(list[(off << 1) + 1]);
        }

        public int mv1R(final int off) {
            return Mv.mvRef(list[(off << 1) + 1]);
        }

        public int getMv(final int off, final int forward) {
            return list[(off << 1) + forward];
        }

        public void setMv(final int off, final int forward, final int mv) {
            list[(off << 1) + forward] = mv;
        }

        public void setPair(final int off, final int mv0, final int mv1) {
            list[(off << 1)] = mv0;
            list[(off << 1) + 1] = mv1;
        }

        public void copyPair(final int off, final MvList other, final int otherOff) {
            list[(off << 1)] = other.list[otherOff << 1];
            list[(off << 1) + 1] = other.list[(otherOff << 1) + 1];
        }
    }

    public static class MvList2D {
        private final int[] list;
        private final int stride;
        private final int width;
        private final int height;
        private static final int NA = Mv.packMv(0, 0, -1);

        public MvList2D(final int width, final int height) {
            list = new int[(width << 1) * height];
            stride = width << 1;
            this.width = width;
            this.height = height;
            clear();
        }

        public void clear() {
            for (int i = 0; i < list.length; i += 2) {
                list[i] = list[i + 1] = NA;
            }
        }

        public int getMv(final int offX, final int offY, final int forward) {
            return list[(offX << 1) + stride * offY + forward];
        }

        public void setMv(final int offX, final int offY, final int forward, final int mv) {
            list[(offX << 1) + stride * offY + forward] = mv;
        }

        public int getHeight() {
            return height;
        }

        public int getWidth() {
            return width;
        }
    }
}
