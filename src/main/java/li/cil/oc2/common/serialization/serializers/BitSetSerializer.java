package li.cil.oc2.common.serialization.serializers;

import li.cil.ceres.api.*;

import javax.annotation.Nullable;
import java.util.BitSet;

@RegisterSerializer
public final class BitSetSerializer implements Serializer<BitSet> {
    @Override
    public void serialize(final SerializationVisitor visitor, final Class<BitSet> type, final Object value) throws SerializationException {
        visitor.putObject("value", long[].class, ((BitSet) value).toLongArray());
    }

    @Override
    public BitSet deserialize(final DeserializationVisitor visitor, final Class<BitSet> type, @Nullable final Object value) throws SerializationException {
        BitSet bitSet = (BitSet) value;
        if (!visitor.exists("value")) {
            return bitSet;
        }

        final long[] longs = (long[]) visitor.getObject("value", long[].class, null);
        if (longs == null) {
            return null;
        }

        if (bitSet == null) {
            bitSet = BitSet.valueOf(longs);
        } else {
            bitSet.clear();
            bitSet.or(BitSet.valueOf(longs));
        }

        return bitSet;
    }
}
