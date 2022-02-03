/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.serialization.ceres;

import li.cil.ceres.api.DeserializationVisitor;
import li.cil.ceres.api.SerializationException;
import li.cil.ceres.api.SerializationVisitor;
import li.cil.ceres.api.Serializer;
import li.cil.oc2.common.vm.context.global.MemoryRangeList;
import li.cil.sedna.api.memory.MemoryRange;

import javax.annotation.Nullable;
import java.util.Arrays;

public final class MemoryRangeListSerializer implements Serializer<MemoryRangeList> {
    @Override
    public void serialize(final SerializationVisitor visitor, final Class<MemoryRangeList> type, final Object value) throws SerializationException {
        final MemoryRangeList list = (MemoryRangeList) value;
        visitor.putObject("value", MemoryRange[].class, list.toArray(new MemoryRange[0]));
    }

    @Nullable
    @Override
    public MemoryRangeList deserialize(final DeserializationVisitor visitor, final Class<MemoryRangeList> type, @Nullable final Object value) throws SerializationException {
        MemoryRangeList list = (MemoryRangeList) value;
        if (!visitor.exists("value")) {
            return list;
        }

        final MemoryRange[] array = (MemoryRange[]) visitor.getObject("value", MemoryRange[].class, null);
        if (array == null) {
            return null;
        }

        if (list == null) {
            list = new MemoryRangeList();
        } else {
            list.clear();
        }

        list.addAll(Arrays.asList(array));

        return list;
    }
}
