package li.cil.oc2.common.util;

import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * A set of integers that is more effective with ranges of integers.
 */
public class IntegerSpace {
    private final NavigableMap<Integer, Integer> ranges = new TreeMap<>();

    public final boolean put(final int element) {
        return put(element, element);
    }

    public final boolean put(final int begin, final int end) {
        if (end < begin) {
            return put(end, begin);
        }

        ranges.subMap(begin, false, end, false).entrySet()
                .removeIf(range -> range.getKey() > begin && range.getValue() < end);

        final Map.Entry<Integer, Integer> floorBegin = ranges.floorEntry(begin);
        final Map.Entry<Integer, Integer> higherEnd = ranges.ceilingEntry(end);

        if (floorBegin != null
            && floorBegin.getKey() <= begin && floorBegin.getValue() >= end) {
            // Already exists in the space
            // [---------]
            // [---------]
            //   [---]
            // [---------]
            return false;
        } else if (floorBegin != null && higherEnd != null
                   && floorBegin.getKey() <= begin && floorBegin.getValue() + 1 >= begin
                   && higherEnd.getKey() - 1 <= end && higherEnd.getValue() >= end) {
            // Remove whitespace between 2 ranges
            // [---------]      [----------]
            //           [------]
            //        [------------]
            //        [---------]
            //           [---------]
            ranges.entrySet().remove(higherEnd);
            ranges.put(floorBegin.getKey(), higherEnd.getValue());
        } else if (higherEnd != null
                   && higherEnd.getKey() - 1 <= end && higherEnd.getValue() >= end) {
            // Change higher range start position
            // [---------]       [---------]
            //              [----]
            //              [---------]
            ranges.entrySet().remove(higherEnd);
            ranges.put(begin, higherEnd.getValue());
        } else if (floorBegin != null
                   && floorBegin.getKey() <= begin && floorBegin.getValue() + 1 >= begin) {
            // New range after some other range
            // [---------]
            //           [--]
            //        [------]
            ranges.put(floorBegin.getKey(), end);
        } else {
            // New range in empty space or new range before all others
            //          [---------]
            // [-----]
            ranges.put(begin, end);
        }
        return true;
    }

    public final boolean contains(final int element) {
        final Map.Entry<Integer, Integer> floorRange = ranges.floorEntry(element);
        return floorRange != null && element >= floorRange.getKey() && element <= floorRange.getValue();
    }

    public final boolean isEmpty() {
        return ranges.isEmpty();
    }

    public final int rangeCount() {
        return ranges.size();
    }

    public final int count() {
        return ranges.entrySet().stream()
                .map(range -> range.getValue() - range.getKey() + 1)
                .reduce(0, Integer::sum);
    }

    protected void elementToString(final StringBuilder builder, final int element) {
        builder.append(element);
    }

    private void appendRangeToString(final StringBuilder builder, final Map.Entry<Integer, Integer> range) {
        final int begin = range.getKey();
        final int end = range.getValue();
        elementToString(builder, begin);
        if (begin != end) {
            builder.append('-');
            elementToString(builder, range.getValue());
        }
    }

    @Override
    public String toString() {
        final Iterator<Map.Entry<Integer, Integer>> iterator = ranges.entrySet().iterator();
        if (iterator.hasNext()) {
            final StringBuilder builder = new StringBuilder();
            builder.append('[');
            final Map.Entry<Integer, Integer> first = iterator.next();
            appendRangeToString(builder, first);
            while (iterator.hasNext()) {
                builder.append(", ");
                final Map.Entry<Integer, Integer> range = iterator.next();
                appendRangeToString(builder, range);
            }
            builder.append(']');
            return builder.toString();
        } else {
            return "[]";
        }
    }
}
