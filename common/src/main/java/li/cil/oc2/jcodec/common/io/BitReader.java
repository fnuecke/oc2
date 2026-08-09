/* SPDX-License-Identifier: BSD 2-Clause "Simplified" License */

package li.cil.oc2.jcodec.common.io;

import java.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
public final class BitReader {
    public static BitReader createBitReader(final ByteBuffer bb) {
        final BitReader r = new BitReader(bb);
        r.curInt = r.readInt();
        r.deficit = 0;
        return r;
    }

    private final ByteBuffer bb;
    private final int initPos;
    private int deficit = -1;
    private int curInt = -1;

    private BitReader(final ByteBuffer bb) {
        this.bb = bb;
        this.initPos = bb.position();
    }

    public int readInt() {
        if (bb.remaining() >= 4) {
            deficit -= 32;
            return ((bb.get() & 0xff) << 24) | ((bb.get() & 0xff) << 16) | ((bb.get() & 0xff) << 8) | (bb.get() & 0xff);
        } else
            return readIntSafe();
    }

    private int readIntSafe() {
        deficit -= (bb.remaining() << 3);
        int res = 0;
        if (bb.hasRemaining())
            res |= bb.get() & 0xff;
        res <<= 8;
        if (bb.hasRemaining())
            res |= bb.get() & 0xff;
        res <<= 8;
        if (bb.hasRemaining())
            res |= bb.get() & 0xff;
        res <<= 8;
        if (bb.hasRemaining())
            res |= bb.get() & 0xff;
        return res;
    }

    public int read1Bit() {

        final int ret = curInt >>> 31;
        curInt <<= 1;
        ++deficit;
        if (deficit == 32) {
            curInt = readInt();
        }

        return ret;
    }

    public int readNBitSigned(final int n) {
        final int v = readNBit(n);
        return read1Bit() == 0 ? v : -v;
    }

    public int readNBit(int n) {
        if (n > 32)
            throw new IllegalArgumentException("Can not read more then 32 bit");

        int ret = 0;
        if (n + deficit > 31) {
            ret |= (curInt >>> deficit);
            n -= 32 - deficit;
            ret <<= n;
            deficit = 32;
            curInt = readInt();
        }

        if (n != 0) {
            ret |= curInt >>> (32 - n);
            curInt <<= n;
            deficit += n;
        }

        return ret;
    }

    public boolean moreData() {
        final int remaining = bb.remaining() + 4 - ((deficit + 7) >> 3);
        return remaining > 1 || (remaining == 1 && curInt != 0);
    }

    public int remaining() {
        return (bb.remaining() << 3) + 32 - deficit;
    }

    public boolean isByteAligned() {
        return (deficit & 0x7) == 0;
    }

    public int skip(final int bits) {
        int left = bits;

        if (left + deficit > 31) {
            left -= 32 - deficit;
            deficit = 32;
            if (left > 31) {
                final int skip = Math.min(left >> 3, bb.remaining());
                bb.position(bb.position() + skip);
                left -= skip << 3;
            }
            curInt = readInt();
        }

        deficit += left;
        curInt <<= left;

        return bits;
    }

    public int skipFast(final int bits) {
        deficit += bits;
        curInt <<= bits;

        return bits;
    }

    public int bitsToAlign() {
        return (deficit & 0x7) > 0 ? 8 - (deficit & 0x7) : 0;
    }

    public int align() {
        return (deficit & 0x7) > 0 ? skip(8 - (deficit & 0x7)) : 0;
    }

    public int check24Bits() {
        if (deficit > 16) {
            deficit -= 16;
            curInt |= nextIgnore16() << deficit;
        }

        if (deficit > 8) {
            deficit -= 8;
            curInt |= nextIgnore() << deficit;
        }

        return curInt >>> 8;
    }

    public int check16Bits() {
        if (deficit > 16) {
            deficit -= 16;
            curInt |= nextIgnore16() << deficit;
        }
        return curInt >>> 16;
    }

    public int readFast16(final int n) {
        if (n == 0)
            return 0;
        if (deficit > 16) {
            deficit -= 16;
            curInt |= nextIgnore16() << deficit;
        }

        final int ret = curInt >>> (32 - n);
        deficit += n;
        curInt <<= n;

        return ret;
    }

    public int checkNBit(final int n) {
        if (n > 24) {
            throw new IllegalArgumentException("Can not check more then 24 bit");
        }

        return checkNBitDontCare(n);
    }

    public int checkNBitDontCare(final int n) {
        while (deficit + n > 32) {
            deficit -= 8;
            curInt |= nextIgnore() << deficit;
        }
        return curInt >>> (32 - n);
    }

    private int nextIgnore16() {
        return bb.remaining() > 1 ? bb.getShort() & 0xffff : (bb.hasRemaining() ? ((bb.get() & 0xff) << 8) : 0);
    }

    private int nextIgnore() {
        return bb.hasRemaining() ? bb.get() & 0xff : 0;
    }

    public int curBit() {
        return deficit & 0x7;
    }

    public boolean lastByte() {
        return bb.remaining() + 4 - (deficit >> 3) <= 1;
    }

    public void terminate() {
        final int putBack = (32 - deficit) >> 3;
        bb.position(bb.position() - putBack);
    }

    public int position() {
        return ((bb.position() - initPos - 4) << 3) + deficit;
    }

    /**
     * Stops this bit reader. Returns underlying ByteBuffer pointer to the next
     * byte unread byte
     */
    public void stop() {
        bb.position(bb.position() - ((32 - deficit) >> 3));
    }

    public boolean readBool() {
        return read1Bit() == 1;
    }
}
