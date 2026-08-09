/* SPDX-License-Identifier: BSD 2-Clause "Simplified" License */

package li.cil.oc2.jcodec.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.System.arraycopy;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * <p>
 * Internal storage is a list of primitive arrays
 *
 * @author The JCodec project
 */
public final class IntArrayList {
    private static final int DEFAULT_GROW_AMOUNT = 1 << 8;

    private int[] storage;
    private int _start;
    private int _size;
    private final List<int[]> chunks;
    private final int growAmount;

    public static IntArrayList createIntArrayList() {
        return new IntArrayList(DEFAULT_GROW_AMOUNT);
    }

    public IntArrayList(final int growAmount) {
        this.chunks = new ArrayList<>();
        this.growAmount = growAmount;
        this.storage = new int[growAmount];
    }

    public int[] toArray() {
        final int[] result = new int[_size + chunks.size() * growAmount - _start];
        int off = 0;
        for (int i = 0; i < chunks.size(); i++) {
            final int[] chunk = chunks.get(i);
            final int aoff = i == 0 ? _start : 0;
            arraycopy(chunk, aoff, result, off, growAmount - aoff);
            off += growAmount;
        }
        final int aoff = chunks.size() == 0 ? _start : 0;
        arraycopy(storage, aoff, result, off, _size - aoff);
        return result;
    }

    public void add(final int val) {
        if (_size >= storage.length) {
            chunks.add(storage);
            storage = new int[growAmount];
            _size = 0;
        }
        storage[_size++] = val;
    }

    public void push(final int id) {
        this.add(id);
    }

    public void pop() {
        if (_size == 0) {
            if (chunks.size() == 0)
                return;
            storage = chunks.remove(chunks.size() - 1);
            _size = growAmount;
        }
        if (chunks.size() == 0 && _size == _start)
            return;
        _size--;
    }

    public void set(int index, final int value) {
        index += _start;
        final int chunk = index / growAmount;
        final int off = index % growAmount;

        if (chunk < chunks.size())
            chunks.get(chunk)[off] = value;
        else
            storage[off] = value;
    }

    public int get(int index) {
        index += _start;
        final int chunk = index / growAmount;
        final int off = index % growAmount;
        return chunk < chunks.size() ? chunks.get(chunk)[off] : storage[off];
    }

    public int shift() {
        if (chunks.size() == 0 && _start >= _size) {
            throw new IllegalStateException();
        }
        final int ret = get(0);
        ++_start;
        if (chunks.size() != 0 && _start >= growAmount) {
            chunks.remove(0);
            _start = 0;
        }
        return ret;
    }

    public void fill(int start, int end, final int val) {
        start += _start;
        end += _start;
        while (start < end) {
            final int chunk = start / growAmount;
            final int off = start % growAmount;
            if (chunk < chunks.size()) {
                final int toFill = Math.min(end - start, growAmount - off);
                Arrays.fill(chunks.get(chunk), off, off + toFill, val);
                start += toFill;
            } else if (chunk == chunks.size()) {
                final int toFill = Math.min(end - start, growAmount - off);
                Arrays.fill(storage, off, off + toFill, val);
                _size = Math.max(_size, off + toFill);
                start += toFill;
                if (_size == growAmount) {
                    chunks.add(storage);
                    _size = 0;
                    storage = new int[growAmount];
                }
            } else {
                chunks.add(storage);
                _size = 0;
                storage = new int[growAmount];
            }
        }
    }

    public int size() {
        return chunks.size() * growAmount + _size - _start;
    }

    public void addAll(final int[] other) {
        int otherOff = 0;
        while (otherOff < other.length) {
            final int copyAmount = Math.min(other.length - otherOff, growAmount - _size);
            if (copyAmount < 32) {
                for (int i = 0; i < copyAmount; i++)
                    storage[_size++] = other[otherOff++];
            } else {
                arraycopy(other, otherOff, storage, _size, copyAmount);
                _size += copyAmount;
                otherOff += copyAmount;
            }
            if (otherOff < other.length) {
                chunks.add(storage);
                storage = new int[growAmount];
                _size = 0;
            }
        }
    }

    public void clear() {
        chunks.clear();
        _size = 0;
        _start = 0;
    }

    public boolean contains(final int needle) {
        for (int c = 0; c < chunks.size(); c++) {
            final int[] chunk = chunks.get(c);
            final int coff = c == 0 ? _start : 0;
            for (int i = coff; i < growAmount; i++) {
                if (chunk[i] == needle)
                    return true;
            }
        }
        final int coff = chunks.size() == 0 ? _start : 0;
        for (int i = coff; i < _size; i++)
            if (storage[i] == needle)
                return true;
        return false;
    }
}
