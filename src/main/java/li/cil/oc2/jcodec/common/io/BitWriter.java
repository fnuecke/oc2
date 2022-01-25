/* SPDX-License-Identifier: BSD 2-Clause "Simplified" License */

package li.cil.oc2.jcodec.common.io;

import java.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * <p>
 * Bitstream writer
 *
 * @author The JCodec project
 */
public final class BitWriter {
    private final ByteBuffer buf;
    private int curInt;
    private int curBit;
    private int initPos;

    public BitWriter(final ByteBuffer buf) {
        this.buf = buf;
        initPos = buf.position();
    }

    public BitWriter fork() {
        final BitWriter fork = new BitWriter(buf.duplicate());
        fork.curBit = this.curBit;
        fork.curInt = this.curInt;
        fork.initPos = this.initPos;
        return fork;
    }

    public void writeOther(final BitWriter bw) {
        if (curBit >= 8) {
            final int shift = 32 - curBit;
            for (int i = initPos; i < bw.buf.position(); i++) {
                buf.put((byte) (curInt >> 24));
                curInt <<= 8;
                curInt |= (bw.buf.get(i) & 0xff) << shift;
            }
        } else {
            final int shift = 24 - curBit;
            for (int i = initPos; i < bw.buf.position(); i++) {
                curInt |= (bw.buf.get(i) & 0xff) << shift;
                buf.put((byte) (curInt >> 24));
                curInt <<= 8;
            }
        }
        writeNBit(bw.curInt >> (32 - bw.curBit), bw.curBit);
    }

    public void flush() {
        final int toWrite = (curBit + 7) >> 3;
        for (int i = 0; i < toWrite; i++) {
            buf.put((byte) (curInt >>> 24));
            curInt <<= 8;
        }
    }

    private void putInt(final int i) {
        buf.put((byte) (i >>> 24));
        buf.put((byte) (i >> 16));
        buf.put((byte) (i >> 8));
        buf.put((byte) i);
    }

    public void writeNBit(int value, final int n) {
        if (n > 32)
            throw new IllegalArgumentException("Max 32 bit to write");
        if (n == 0)
            return;
        value &= -1 >>> (32 - n);
        if (32 - curBit >= n) {
            curInt |= value << (32 - curBit - n);
            curBit += n;
            if (curBit == 32) {
                putInt(curInt);
                curBit = 0;
                curInt = 0;
            }
        } else {
            final int secPart = n - (32 - curBit);
            curInt |= value >>> secPart;
            putInt(curInt);
            curInt = value << (32 - secPart);
            curBit = secPart;
        }
    }

    public void write1Bit(final int bit) {
        curInt |= bit << (32 - curBit - 1);
        ++curBit;
        if (curBit == 32) {
            putInt(curInt);
            curBit = 0;
            curInt = 0;
        }
    }

    public int curBit() {
        return curBit & 0x7;
    }

    public int position() {
        return ((buf.position() - initPos) << 3) + curBit;
    }

    public ByteBuffer getBuffer() {
        return buf;
    }
}
