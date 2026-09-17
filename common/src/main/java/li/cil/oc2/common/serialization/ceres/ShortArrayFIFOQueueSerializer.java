/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.serialization.ceres;

import it.unimi.dsi.fastutil.shorts.ShortArrayFIFOQueue;
import li.cil.ceres.api.DeserializationVisitor;
import li.cil.ceres.api.SerializationException;
import li.cil.ceres.api.SerializationVisitor;
import li.cil.ceres.api.Serializer;

import javax.annotation.Nullable;

public final class ShortArrayFIFOQueueSerializer implements Serializer<ShortArrayFIFOQueue> {
    @Override
    public void serialize(final SerializationVisitor visitor, final Class<ShortArrayFIFOQueue> type, final Object value) throws SerializationException {
        final ShortArrayFIFOQueue queue = (ShortArrayFIFOQueue) value;
        final short[] values = new short[queue.size()];
        // Can't iterate ShortArrayFIFOQueue, so we have to read all and then put it back :/
        for (int i = 0; i < values.length; i++) {
            values[i] = queue.dequeueShort();
        }
        for (final short element : values) {
            queue.enqueue(element);
        }
        visitor.putObject("values", short[].class, values);
    }

    @Override
    @Nullable
    public ShortArrayFIFOQueue deserialize(final DeserializationVisitor visitor, final Class<ShortArrayFIFOQueue> type, @Nullable final Object value) throws SerializationException {
        ShortArrayFIFOQueue queue = (ShortArrayFIFOQueue) value;
        if (!visitor.exists("values")) {
            return queue;
        }

        final short[] values = (short[]) visitor.getObject("values", short[].class, null);
        if (values == null) {
            return null;
        }

        if (queue == null) {
            queue = new ShortArrayFIFOQueue();
        }

        queue.clear();
        for (final short element : values) {
            queue.enqueue(element);
        }

        return queue;
    }
}
