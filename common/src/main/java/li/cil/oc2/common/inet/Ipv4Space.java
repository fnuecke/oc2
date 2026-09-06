/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.inet;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public final class Ipv4Space {
    public static final long MAX_ADDRESS = 0xFFFFFFFFL;
    private static final long[] EMPTY = new long[0];

    // --------------------------------------------------------------------- //

    private final NavigableMap<Long, Long> ranges = new TreeMap<>();
    private long[] starts = EMPTY;
    private long[] ends = EMPTY;

    // --------------------------------------------------------------------- //

    public void add(final long address) {
        add(address, address);
    }

    public void add(final long begin, final long end) {
        if (end < begin) {
            add(end, begin);
            return;
        }
        if (begin < 0 || end > MAX_ADDRESS) {
            throw new IllegalArgumentException("Address out of range: " + begin + ".." + end);
        }

        // Absorb every range that starts at or before end + 1, so that adjacent ranges merge.
        long mergedBegin = begin;
        long mergedEnd = end;

        final Map.Entry<Long, Long> floor = ranges.floorEntry(begin);
        if (floor != null && floor.getValue() >= begin - 1) {
            mergedBegin = floor.getKey();
            mergedEnd = Math.max(mergedEnd, floor.getValue());
            ranges.remove(floor.getKey());
        }

        final Iterator<Map.Entry<Long, Long>> overlapping =
            ranges.subMap(mergedBegin, true, Math.min(end + 1, MAX_ADDRESS), true).entrySet().iterator();
        while (overlapping.hasNext()) {
            final Map.Entry<Long, Long> range = overlapping.next();
            mergedEnd = Math.max(mergedEnd, range.getValue());
            overlapping.remove();
        }

        ranges.put(mergedBegin, mergedEnd);
        flatten();
    }

    public void addSubnet(final long address, final int prefix) {
        if (prefix < 0 || prefix > 32) {
            throw new IllegalArgumentException("Prefix length out of range: " + prefix);
        }
        if (address < 0 || address > MAX_ADDRESS) {
            throw new IllegalArgumentException("Address out of range: " + address);
        }
        // Shifting by 32 is a no-op in Java, so the whole-space case needs spelling out.
        final long mask = prefix == 0 ? 0L : (MAX_ADDRESS << (32 - prefix)) & MAX_ADDRESS;
        add(address & mask, (address & mask) | (~mask & MAX_ADDRESS));
    }

    public boolean contains(final long address) {
        final int index = Arrays.binarySearch(starts, address);
        if (index >= 0) {
            return true;
        }
        // Ranges never overlap, so only the one starting just before the address can hold it.
        final int candidate = -index - 2;
        return candidate >= 0 && address <= ends[candidate];
    }

    public boolean isEmpty() {
        return ranges.isEmpty();
    }

    public long size() {
        long total = 0;
        for (final Map.Entry<Long, Long> range : ranges.entrySet()) {
            total += range.getValue() - range.getKey() + 1;
        }
        return total;
    }

    public int rangeCount() {
        return ranges.size();
    }

    @Override
    public String toString() {
        if (ranges.isEmpty()) {
            return "[]";
        }
        final StringBuilder builder = new StringBuilder("[");
        for (final Map.Entry<Long, Long> range : ranges.entrySet()) {
            if (builder.length() > 1) {
                builder.append(", ");
            }
            InetUtils.ipv4AddressToString(builder, (int) (long) range.getKey());
            if (!range.getKey().equals(range.getValue())) {
                builder.append('-');
                InetUtils.ipv4AddressToString(builder, (int) (long) range.getValue());
            }
        }
        return builder.append(']').toString();
    }

    // --------------------------------------------------------------------- //

    private void flatten() {
        if (starts.length != ranges.size()) {
            starts = new long[ranges.size()];
            ends = new long[ranges.size()];
        }
        int i = 0;
        for (final Map.Entry<Long, Long> range : ranges.entrySet()) {
            starts[i] = range.getKey();
            ends[i] = range.getValue();
            ++i;
        }
    }
}
