package li.cil.oc2.common.serialization;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import li.cil.ceres.Ceres;
import li.cil.ceres.api.DeserializationVisitor;
import li.cil.ceres.api.SerializationException;
import li.cil.ceres.api.SerializationVisitor;
import li.cil.oc2.common.util.NBTTagIds;
import net.minecraft.nbt.*;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nullable;
import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class NBTSerialization {
    public static <T> void serialize(final CompoundTag tag, final T value, final Class<T> type) throws SerializationException {
        Ceres.getSerializer(type).serialize(new Serializer(tag), type, value);
    }

    public static <T> void serialize(final CompoundTag tag, final T value) throws SerializationException {
        @SuppressWarnings("unchecked") final Class<T> type = (Class<T>) value.getClass();
        serialize(tag, value, type);
    }

    public static <T> CompoundTag serialize(final T value, final Class<T> type) throws SerializationException {
        final CompoundTag tag = new CompoundTag();
        serialize(tag, value, type);
        return tag;
    }

    public static <T> CompoundTag serialize(final T value) throws SerializationException {
        final CompoundTag tag = new CompoundTag();
        serialize(tag, value);
        return tag;
    }

    public static <T> T deserialize(final CompoundTag tag, final Class<T> type, @Nullable final T into) throws SerializationException {
        return Ceres.getSerializer(type).deserialize(new Deserializer(tag), type, into);
    }

    public static <T> T deserialize(final CompoundTag tag, final Class<T> type) throws SerializationException {
        return deserialize(tag, type, null);
    }

    public static <T> T deserialize(final CompoundTag tag, final T into) throws SerializationException {
        @SuppressWarnings("unchecked") final Class<T> type = (Class<T>) into.getClass();
        return deserialize(tag, type, into);
    }

    ///////////////////////////////////////////////////////////////////

    private static final String IS_NULL_KEY = "<is_null>";
    private static final Map<Class<?>, ArraySerializer> ARRAY_SERIALIZERS;

    static {
        ARRAY_SERIALIZERS = new HashMap<>();
        ARRAY_SERIALIZERS.put(boolean.class, new BooleanArraySerializer());
        ARRAY_SERIALIZERS.put(byte.class, new ByteArraySerializer());
        ARRAY_SERIALIZERS.put(char.class, new CharArraySerializer());
        ARRAY_SERIALIZERS.put(short.class, new ShortArraySerializer());
        ARRAY_SERIALIZERS.put(int.class, new IntArraySerializer());
        ARRAY_SERIALIZERS.put(long.class, new LongArraySerializer());
        ARRAY_SERIALIZERS.put(float.class, new FloatArraySerializer());
        ARRAY_SERIALIZERS.put(double.class, new DoubleArraySerializer());
        ARRAY_SERIALIZERS.put(Enum.class, new EnumArraySerializer());
        ARRAY_SERIALIZERS.put(String.class, new StringArraySerializer());
        ARRAY_SERIALIZERS.put(UUID.class, new UUIDArraySerializer());
    }

    private static final class Serializer implements SerializationVisitor {
        private final CompoundTag tag;

        private Serializer(final CompoundTag tag) {
            this.tag = tag;
        }

        @Override
        public void putBoolean(final String name, final boolean value) {
            tag.putBoolean(name, value);
        }

        @Override
        public void putByte(final String name, final byte value) {
            tag.putByte(name, value);
        }

        @Override
        public void putChar(final String name, final char value) {
            tag.putInt(name, value);
        }

        @Override
        public void putShort(final String name, final short value) {
            tag.putShort(name, value);
        }

        @Override
        public void putInt(final String name, final int value) {
            tag.putInt(name, value);
        }

        @Override
        public void putLong(final String name, final long value) {
            tag.putLong(name, value);
        }

        @Override
        public void putFloat(final String name, final float value) {
            tag.putFloat(name, value);
        }

        @Override
        public void putDouble(final String name, final double value) {
            tag.putDouble(name, value);
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        @Override
        public void putObject(final String name, final Class<?> type, @Nullable final Object value) throws SerializationException {
            if (putIsNull(name, value)) {
                return;
            }

            if (type.isArray()) {
                tag.put(name, putArray(name, type, value));
            } else if (type.isEnum()) {
                tag.putString(name, ((Enum) value).name());
            } else if (type == String.class) {
                tag.putString(name, (String) value);
            } else if (type == UUID.class) {
                final CompoundTag uuidTag = new CompoundTag();
                uuidTag.putUUID(name, (UUID) value);
                tag.put(name, uuidTag);
            } else {
                final CompoundTag valueTag = new CompoundTag();
                Ceres.getSerializer(type).serialize(new Serializer(valueTag), (Class) type, value);
                if (!valueTag.isEmpty()) {
                    tag.put(name, valueTag);
                }
            }
        }

        @FunctionalInterface
        private interface ArrayComponentSerializer {
            Tag serialize(Class<?> type, Object value);
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private Tag putArray(final String name, final Class<?> type, final Object value) {
            final Class<?> componentType = type.getComponentType();

            final ArraySerializer arraySerializer = ARRAY_SERIALIZERS.get(componentType);
            if (arraySerializer != null) {
                return arraySerializer.serialize(value);
            } else {
                final ArrayComponentSerializer componentSerializer;
                if (componentType.isArray()) {
                    componentSerializer = (t, v) -> putArray(name, t, v);
                } else {
                    final li.cil.ceres.api.Serializer<?> serializer = Ceres.getSerializer(componentType);
                    componentSerializer = (t, v) -> {
                        final CompoundTag tag = new CompoundTag();
                        serializer.serialize(new Serializer(tag), (Class) t, v);
                        return tag;
                    };
                }

                final ListTag listTag = new ListTag();
                final IntArrayList nullIndices = new IntArrayList();

                final Object[] data = (Object[]) value;
                for (int i = 0; i < data.length; i++) {
                    final Object datum = data[i];
                    if (datum == null) {
                        nullIndices.add(i);
                    } else {
                        if (datum.getClass() != componentType) {
                            throw new SerializationException(String.format("Polymorphism detected in generic array [%s]. This is not supported.", name));
                        }
                        listTag.add(componentSerializer.serialize(componentType, datum));
                    }
                }

                if (nullIndices.isEmpty()) {
                    return listTag;
                } else {
                    final CompoundTag arrayTag = new CompoundTag();
                    arrayTag.put("value", listTag);
                    arrayTag.putIntArray("nulls", nullIndices);

                    return arrayTag;
                }
            }
        }

        @Contract(value = "_, null -> true")
        private boolean putIsNull(final String name, @Nullable final Object value) {
            final boolean isNull = value == null;
            if (isNull) {
                final CompoundTag nullTag = new CompoundTag();
                nullTag.putBoolean(IS_NULL_KEY, true);
                tag.put(name, nullTag);
            }
            return isNull;
        }
    }

    private static final class Deserializer implements DeserializationVisitor {
        private final CompoundTag tag;

        private Deserializer(final CompoundTag tag) {
            this.tag = tag;
        }

        @Override
        public boolean getBoolean(final String name) {
            return tag.getBoolean(name);
        }

        @Override
        public byte getByte(final String name) {
            return tag.getByte(name);
        }

        @Override
        public char getChar(final String name) {
            return (char) tag.getInt(name);
        }

        @Override
        public short getShort(final String name) {
            return tag.getShort(name);
        }

        @Override
        public int getInt(final String name) {
            return tag.getInt(name);
        }

        @Override
        public long getLong(final String name) {
            return tag.getLong(name);
        }

        @Override
        public float getFloat(final String name) {
            return tag.getFloat(name);
        }

        @Override
        public double getDouble(final String name) {
            return tag.getDouble(name);
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        @Nullable
        @Override
        public Object getObject(final String name, final Class<?> type, @Nullable final Object into) throws SerializationException {
            if (isNull(name)) {
                return null;
            }

            // Do not overwrite values which were not serialized before.
            if (!tag.contains(name)) {
                return into;
            }

            if (type.isArray()) {
                final Tag arrayTag = tag.get(name);
                assert arrayTag != null;
                return getArray(arrayTag, type, into);
            } else if (type.isEnum()) {
                return Enum.valueOf((Class) type, tag.getString(name));
            } else if (type == String.class) {
                return tag.getString(name);
            } else if (type == UUID.class) {
                return tag.getCompound(name).getUUID(name);
            } else {
                final CompoundTag valueTag = tag.getCompound(name);
                return Ceres.getSerializer(type).deserialize(new Deserializer(valueTag), (Class) type, into);
            }
        }

        @FunctionalInterface
        private interface ArrayComponentDeserializer {
            @Nullable
            Object deserialize(Tag tag, Class<?> type, @Nullable Object into);
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        @Nullable
        private static Object getArray(final Tag tag, final Class<?> type, final @Nullable Object into) {
            final Class<?> componentType = type.getComponentType();

            final ArraySerializer arraySerializer = ARRAY_SERIALIZERS.get(componentType);
            if (arraySerializer != null) {
                return arraySerializer.deserialize(tag, type, into);
            } else {
                final ArrayComponentDeserializer componentDeserializer;
                if (componentType.isArray()) {
                    componentDeserializer = Deserializer::getArray;
                } else {
                    final li.cil.ceres.api.Serializer<?> serializer = Ceres.getSerializer(componentType);
                    componentDeserializer = (n, t, i) -> serializer.deserialize(new Deserializer((CompoundTag) n), (Class) t, i);
                }

                Object[] data = (Object[]) into;
                final ListTag listTag;
                final int[] nulls;
                int nullsIndex = 0;
                if (tag instanceof final ListTag plainListTag) {
                    listTag = plainListTag;
                    nulls = new int[0];
                } else if (tag instanceof final CompoundTag compoundTag) {
                    listTag = (ListTag) compoundTag.get("value");
                    nulls = compoundTag.getIntArray("nulls");
                } else {
                    return data;
                }

                if (listTag == null) {
                    return data;
                }

                final int length = listTag.size() + nulls.length;
                if (data == null || data.length != length) {
                    data = (Object[]) Array.newInstance(componentType, length);
                }

                for (int i = 0; i < length; i++) {
                    if (nullsIndex < nulls.length && i == nulls[nullsIndex]) {
                        nullsIndex++;
                        continue;
                    }

                    final Tag itemTag = listTag.get(i - nullsIndex);
                    if (itemTag == null) {
                        continue;
                    }

                    data[i] = componentDeserializer.deserialize(itemTag, componentType, data[i]);
                }

                return data;
            }
        }

        @Override
        public boolean exists(final String name) {
            return tag.contains(name);
        }

        private boolean isNull(final String name) {
            return tag.getCompound(name).getBoolean(IS_NULL_KEY);
        }
    }

    ///////////////////////////////////////////////////////////////////

    private interface ArraySerializer {
        Tag serialize(Object value);

        @Nullable
        Object deserialize(Tag tag, final Class<?> type, @Nullable final Object into);
    }

    private static final class BooleanArraySerializer implements ArraySerializer {
        @Override
        public Tag serialize(final Object value) {
            final boolean[] data = (boolean[]) value;
            final byte[] convertedData = new byte[data.length];
            for (int i = 0; i < data.length; i++) {
                convertedData[i] = data[i] ? (byte) 1 : (byte) 0;
            }
            return new ByteArrayTag(convertedData);
        }

        @Override
        public Object deserialize(final Tag tag, final Class<?> type, @Nullable final Object into) {
            boolean[] data = (boolean[]) into;
            if (tag instanceof final ByteArrayTag byteArrayTag) {
                final byte[] convertedData = byteArrayTag.getAsByteArray();
                if (data == null || data.length != convertedData.length) {
                    data = new boolean[convertedData.length];
                }
                for (int i = 0; i < convertedData.length; i++) {
                    data[i] = convertedData[i] != 0;
                }
            }
            return data;
        }
    }

    private static final class ByteArraySerializer implements ArraySerializer {
        @Override
        public Tag serialize(final Object value) {
            return new ByteArrayTag((byte[]) value);
        }

        @Override
        public Object deserialize(final Tag tag, final Class<?> type, @Nullable final Object into) {
            final byte[] data = (byte[]) into;
            if (tag instanceof final ByteArrayTag byteArrayTag) {
                final byte[] serializedData = byteArrayTag.getAsByteArray();
                if (data == null || data.length != serializedData.length) {
                    return serializedData;
                }
                System.arraycopy(serializedData, 0, data, 0, serializedData.length);
            }
            return data;
        }
    }

    private static final class CharArraySerializer implements ArraySerializer {
        @Override
        public Tag serialize(final Object value) {
            final char[] data = (char[]) value;
            final int[] convertedData = new int[data.length];
            for (int i = 0; i < data.length; i++) {
                convertedData[i] = data[i];
            }
            return new IntArrayTag(convertedData);
        }

        @Override
        public Object deserialize(final Tag tag, final Class<?> type, @Nullable final Object into) {
            char[] data = (char[]) into;
            if (tag instanceof final IntArrayTag intArrayTag) {
                final int[] convertedData = intArrayTag.getAsIntArray();
                if (data == null || data.length != convertedData.length) {
                    data = new char[convertedData.length];
                }
                for (int i = 0; i < convertedData.length; i++) {
                    data[i] = (char) convertedData[i];
                }
            }
            return data;
        }
    }

    private static final class ShortArraySerializer implements ArraySerializer {
        @Override
        public Tag serialize(final Object value) {
            final short[] data = (short[]) value;
            final int[] convertedData = new int[data.length];
            for (int i = 0; i < data.length; i++) {
                convertedData[i] = data[i];
            }
            return new IntArrayTag(convertedData);
        }

        @Override
        public Object deserialize(final Tag tag, final Class<?> type, @Nullable final Object into) {
            short[] data = (short[]) into;
            if (tag instanceof final IntArrayTag intArrayTag) {
                final int[] convertedData = intArrayTag.getAsIntArray();
                if (data == null || data.length != convertedData.length) {
                    data = new short[convertedData.length];
                }
                for (int i = 0; i < convertedData.length; i++) {
                    data[i] = (short) convertedData[i];
                }
            }
            return data;
        }
    }

    private static final class IntArraySerializer implements ArraySerializer {
        @Override
        public Tag serialize(final Object value) {
            return new IntArrayTag((int[]) value);
        }

        @Override
        public Object deserialize(final Tag tag, final Class<?> type, @Nullable final Object into) {
            final int[] data = (int[]) into;
            if (tag instanceof final IntArrayTag intArrayTag) {
                final int[] serializedData = intArrayTag.getAsIntArray();
                if (data == null || data.length != serializedData.length) {
                    return serializedData;
                }
                System.arraycopy(serializedData, 0, data, 0, serializedData.length);
            }
            return data;
        }
    }

    private static final class LongArraySerializer implements ArraySerializer {
        @Override
        public Tag serialize(final Object value) {
            return new LongArrayTag((long[]) value);
        }

        @Override
        public Object deserialize(final Tag tag, final Class<?> type, @Nullable final Object into) {
            final long[] data = (long[]) into;
            if (tag instanceof final LongArrayTag longArrayTag) {
                final long[] serializedData = longArrayTag.getAsLongArray();
                if (data == null || data.length != serializedData.length) {
                    return serializedData;
                }
                System.arraycopy(serializedData, 0, data, 0, serializedData.length);
            }
            return data;
        }
    }

    private static final class FloatArraySerializer implements ArraySerializer {
        @Override
        public Tag serialize(final Object value) {
            final float[] data = (float[]) value;
            final int[] convertedData = new int[data.length];
            for (int i = 0; i < data.length; i++) {
                convertedData[i] = Float.floatToRawIntBits(data[i]);
            }
            return new IntArrayTag(convertedData);
        }

        @Override
        public Object deserialize(final Tag tag, final Class<?> type, @Nullable final Object into) {
            float[] data = (float[]) into;
            if (tag instanceof final IntArrayTag intArrayTag) {
                final int[] convertedData = intArrayTag.getAsIntArray();
                if (data == null || data.length != convertedData.length) {
                    data = new float[convertedData.length];
                }
                for (int i = 0; i < convertedData.length; i++) {
                    data[i] = Float.intBitsToFloat(convertedData[i]);
                }
            }
            return data;
        }
    }

    private static final class DoubleArraySerializer implements ArraySerializer {
        @Override
        public Tag serialize(final Object value) {
            final double[] data = (double[]) value;
            final long[] convertedData = new long[data.length];
            for (int i = 0; i < data.length; i++) {
                convertedData[i] = Double.doubleToRawLongBits(data[i]);
            }
            return new LongArrayTag(convertedData);
        }

        @Override
        public Object deserialize(final Tag tag, final Class<?> type, @Nullable final Object into) {
            double[] data = (double[]) into;
            if (tag instanceof final LongArrayTag longArrayTag) {
                final long[] convertedData = longArrayTag.getAsLongArray();
                if (data == null || data.length != convertedData.length) {
                    data = new double[convertedData.length];
                }
                for (int i = 0; i < convertedData.length; i++) {
                    data[i] = Double.longBitsToDouble(convertedData[i]);
                }
            }
            return data;
        }
    }

    @SuppressWarnings("rawtypes")
    private static final class EnumArraySerializer implements ArraySerializer {
        @Override
        public Tag serialize(final Object value) {
            final Enum[] data = (Enum[]) value;
            final int[] convertedData = new int[data.length];
            for (int i = 0; i < data.length; i++) {
                convertedData[i] = data[i].ordinal();
            }
            return new IntArrayTag(convertedData);
        }

        @Override
        public Object deserialize(final Tag tag, final Class<?> type, @Nullable final Object into) {
            final Class<?> componentType = type.getComponentType();
            final Object[] enumConstants = componentType.getEnumConstants();

            Enum[] data = (Enum[]) into;
            if (tag instanceof final IntArrayTag intArrayTag) {
                final int[] serializedData = intArrayTag.getAsIntArray();
                if (data == null || data.length != serializedData.length) {
                    data = (Enum[]) Array.newInstance(componentType, serializedData.length);
                }
                for (int i = 0; i < serializedData.length; i++) {
                    data[i] = (Enum) enumConstants[serializedData[i]];
                }
            }
            return data;
        }
    }

    private static final class StringArraySerializer implements ArraySerializer {
        @Override
        public Tag serialize(final Object value) {
            final String[] data = (String[]) value;
            final ListTag list = new ListTag();
            for (final String datum : data) {
                list.add(StringTag.valueOf(datum));
            }
            return list;
        }

        @Override
        public Object deserialize(final Tag tag, final Class<?> type, @Nullable final Object into) {
            String[] data = (String[]) into;
            if (tag instanceof final ListTag serializedData) {
                if (serializedData.isEmpty() || serializedData.getElementType() == NBTTagIds.TAG_STRING) {
                    if (data == null || data.length != serializedData.size()) {
                        data = new String[serializedData.size()];
                    }
                    for (int i = 0; i < serializedData.size(); i++) {
                        data[i] = serializedData.getString(i);
                    }
                }
            }
            return data;
        }
    }

    private static final class UUIDArraySerializer implements ArraySerializer {
        @Override
        public Tag serialize(final Object value) {
            final UUID[] data = (UUID[]) value;
            final ListTag list = new ListTag();
            for (final UUID datum : data) {
                list.add(StringTag.valueOf(datum.toString()));
            }
            return list;
        }

        @Override
        public Object deserialize(final Tag tag, final Class<?> type, @Nullable final Object into) {
            UUID[] data = (UUID[]) into;
            if (tag instanceof final ListTag serializedData) {
                if (serializedData.isEmpty() || serializedData.getElementType() == NBTTagIds.TAG_STRING) {
                    if (data == null || data.length != serializedData.size()) {
                        data = new UUID[serializedData.size()];
                    }
                    for (int i = 0; i < serializedData.size(); i++) {
                        data[i] = UUID.fromString(serializedData.getString(i));
                    }
                }
            }
            return data;
        }
    }
}
