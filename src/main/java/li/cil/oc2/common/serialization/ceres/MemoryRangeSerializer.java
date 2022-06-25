/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.serialization.ceres;

import li.cil.ceres.api.DeserializationVisitor;
import li.cil.ceres.api.SerializationException;
import li.cil.ceres.api.SerializationVisitor;
import li.cil.ceres.api.Serializer;
import li.cil.sedna.api.memory.MemoryRange;

import javax.annotation.Nullable;

public final class MemoryRangeSerializer implements Serializer<MemoryRange> {
    @Override
    public void serialize(final SerializationVisitor visitor, final Class<MemoryRange> type, final Object value) throws SerializationException {
        final MemoryRange range = (MemoryRange) value;
        visitor.putLong("start", range.start);
        visitor.putLong("end", range.end);
    }

    @Nullable
    @Override
    public MemoryRange deserialize(final DeserializationVisitor visitor, final Class<MemoryRange> type, @Nullable final Object value) throws SerializationException {
        if (!visitor.exists("start") || !visitor.exists("end")) {
            return (MemoryRange) value;
        }

        return MemoryRange.of(visitor.getLong("start"), visitor.getLong("end"));
    }
}
