/* SPDX-License-Identifier: BSD 2-Clause "Simplified" License */

package li.cil.oc2.jcodec.common;

import java.util.Arrays;

import static java.lang.System.arraycopy;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
public final class IntObjectMap<T> {
    private static final int GROW_BY = 128;
    private Object[] storage;
    private int _size;

    public IntObjectMap() {
        this.storage = new Object[GROW_BY];
    }

    public void put(final int key, final T val) {
        if (storage.length <= key) {
            final Object[] ns = new Object[key + GROW_BY];
            arraycopy(storage, 0, ns, 0, storage.length);
            storage = ns;
        }
        if (storage[key] == null)
            _size++;
        storage[key] = val;
    }

    @SuppressWarnings("unchecked")
    public T get(final int key) {
        return key >= storage.length ? null : (T) storage[key];
    }

    public int[] keys() {
        final int[] result = new int[_size];
        for (int i = 0, r = 0; i < storage.length; i++) {
            if (storage[i] != null)
                result[r++] = i;
        }
        return result;
    }

    public void clear() {
        Arrays.fill(storage, null);
        _size = 0;
    }

    public int size() {
        return _size;
    }

    public void remove(final int key) {
        if (storage[key] != null)
            _size--;
        storage[key] = null;
    }
}
