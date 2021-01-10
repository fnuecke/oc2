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
    public static <T> void serialize(final CompoundNBT nbt, final T value, final Class<T> type) throws SerializationException {
        Ceres.getSerializer(type).serialize(new Serializer(nbt), type, value);
    }

    public static <T> void serialize(final CompoundNBT nbt, final T value) throws SerializationException {
        @SuppressWarnings("unchecked") final Class<T> type = (Class<T>) value.getClass();
        serialize(nbt, value, type);
    }

    public static <T> CompoundNBT serialize(final T value, final Class<T> type) throws SerializationException {
        final CompoundNBT nbt = new CompoundNBT();
        serialize(nbt, value, type);
        return nbt;
    }

    public static <T> CompoundNBT serialize(final T value) throws SerializationException {
        final CompoundNBT nbt = new CompoundNBT();
        serialize(nbt, value);
        return nbt;
    }

    public static <T> T deserialize(final CompoundNBT nbt, final Class<T> type, @Nullable final T into) throws SerializationException {
        return Ceres.getSerializer(type).deserialize(new Deserializer(nbt), type, into);
    }

    public static <T> T deserialize(final CompoundNBT nbt, final Class<T> type) throws SerializationException {
        return deserialize(nbt, type, null);
    }

    public static <T> T deserialize(final CompoundNBT nbt, final T into) throws SerializationException {
        @SuppressWarnings("unchecked") final Class<T> type = (Class<T>) into.getClass();
        return deserialize(nbt, type, into);
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
        private final CompoundNBT nbt;

        private Serializer(final CompoundNBT nbt) {
            this.nbt = nbt;
        }

        @Override
        public void putBoolean(final String name, final boolean value) {
            nbt.putBoolean(name, value);
        }

        @Override
        public void putByte(final String name, final byte value) {
            nbt.putByte(name, value);
        }

        @Override
        public void putChar(final String name, final char value) {
            nbt.putInt(name, value);
        }

        @Override
        public void putShort(final String name, final short value) {
            nbt.putShort(name, value);
        }

        @Override
        public void putInt(final String name, final int value) {
            nbt.putInt(name, value);
        }

        @Override
        public void putLong(final String name, final long value) {
            nbt.putLong(name, value);
        }

        @Override
        public void putFloat(final String name, final float value) {
            nbt.putFloat(name, value);
        }

        @Override
        public void putDouble(final String name, final double value) {
            nbt.putDouble(name, value);
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        @Override
        public void putObject(final String name, final Class<?> type, @Nullable final Object value) throws SerializationException {
            if (putIsNull(name, value)) {
                return;
            }

            if (type.isArray()) {
                nbt.put(name, putArray(name, type, value));
            } else if (type.isEnum()) {
                nbt.putString(name, ((Enum) value).name());
            } else if (type == String.class) {
                nbt.putString(name, (String) value);
            } else if (type == UUID.class) {
                final CompoundNBT uuidNBT = new CompoundNBT();
                uuidNBT.putUniqueId(name, (UUID) value);
                nbt.put(name, uuidNBT);
            } else {
                final CompoundNBT valueNBT = new CompoundNBT();
                Ceres.getSerializer(type).serialize(new Serializer(valueNBT), (Class) type, value);
                if (!valueNBT.isEmpty()) {
                    nbt.put(name, valueNBT);
                }
            }
        }

        @FunctionalInterface
        private interface ArrayComponentSerializer {
            INBT serialize(Class<?> type, Object value);
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private INBT putArray(final String name, final Class<?> type, final Object value) {
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
                        final CompoundNBT nbt = new CompoundNBT();
                        serializer.serialize(new Serializer(nbt), (Class) t, v);
                        return nbt;
                    };
                }

                final ListNBT listNBT = new ListNBT();
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
                        listNBT.add(componentSerializer.serialize(componentType, datum));
                    }
                }

                if (nullIndices.isEmpty()) {
                    return listNBT;
                } else {
                    final CompoundNBT arrayNbt = new CompoundNBT();
                    arrayNbt.put("value", listNBT);
                    arrayNbt.putIntArray("nulls", nullIndices);

                    return arrayNbt;
                }
            }
        }

        @Contract(value = "_, null -> true")
        private boolean putIsNull(final String name, @Nullable final Object value) {
            final boolean isNull = value == null;
            if (isNull) {
                final CompoundNBT nullNBT = new CompoundNBT();
                nullNBT.putBoolean(IS_NULL_KEY, true);
                nbt.put(name, nullNBT);
            }
            return isNull;
        }
    }

    private static final class Deserializer implements DeserializationVisitor {
        private final CompoundNBT nbt;

        private Deserializer(final CompoundNBT nbt) {
            this.nbt = nbt;
        }

        @Override
        public boolean getBoolean(final String name) {
            return nbt.getBoolean(name);
        }

        @Override
        public byte getByte(final String name) {
            return nbt.getByte(name);
        }

        @Override
        public char getChar(final String name) {
            return (char) nbt.getInt(name);
        }

        @Override
        public short getShort(final String name) {
            return nbt.getShort(name);
        }

        @Override
        public int getInt(final String name) {
            return nbt.getInt(name);
        }

        @Override
        public long getLong(final String name) {
            return nbt.getLong(name);
        }

        @Override
        public float getFloat(final String name) {
            return nbt.getFloat(name);
        }

        @Override
        public double getDouble(final String name) {
            return nbt.getDouble(name);
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        @Nullable
        @Override
        public Object getObject(final String name, final Class<?> type, @Nullable final Object into) throws SerializationException {
            if (isNull(name)) {
                return null;
            }

            // Do not overwrite values which were not serialized before.
            if (!nbt.contains(name)) {
                return into;
            }

            if (type.isArray()) {
                final INBT arrayNbt = nbt.get(name);
                assert arrayNbt != null;
                return getArray(arrayNbt, type, into);
            } else if (type.isEnum()) {
                return Enum.valueOf((Class) type, nbt.getString(name));
            } else if (type == String.class) {
                return nbt.getString(name);
            } else if (type == UUID.class) {
                return nbt.getCompound(name).getUniqueId(name);
            } else {
                final CompoundNBT valueNBT = nbt.getCompound(name);
                return Ceres.getSerializer(type).deserialize(new Deserializer(valueNBT), (Class) type, into);
            }
        }

        @FunctionalInterface
        private interface ArrayComponentDeserializer {
            @Nullable
            Object deserialize(INBT nbt, Class<?> type, @Nullable Object into);
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        @Nullable
        private static Object getArray(final INBT nbt, final Class<?> type, final @Nullable Object into) {
            final Class<?> componentType = type.getComponentType();

            final ArraySerializer arraySerializer = ARRAY_SERIALIZERS.get(componentType);
            if (arraySerializer != null) {
                return arraySerializer.deserialize(nbt, type, into);
            } else {
                final ArrayComponentDeserializer componentDeserializer;
                if (componentType.isArray()) {
                    componentDeserializer = Deserializer::getArray;
                } else {
                    final li.cil.ceres.api.Serializer<?> serializer = Ceres.getSerializer(componentType);
                    componentDeserializer = (n, t, i) -> serializer.deserialize(new Deserializer((CompoundNBT) n), (Class) t, i);
                }

                Object[] data = (Object[]) into;
                final ListNBT listNBT;
                final int[] nulls;
                int nullsIndex = 0;
                if (nbt instanceof ListNBT) {
                    listNBT = (ListNBT) nbt;
                    nulls = new int[0];
                } else if (nbt instanceof CompoundNBT) {
                    listNBT = (ListNBT) ((CompoundNBT) nbt).get("value");
                    nulls = ((CompoundNBT) nbt).getIntArray("nulls");
                } else {
                    return data;
                }

                if (listNBT == null) {
                    return data;
                }

                final int length = listNBT.size() + nulls.length;
                if (data == null || data.length != length) {
                    data = (Object[]) Array.newInstance(componentType, length);
                }

                for (int i = 0; i < length; i++) {
                    if (nullsIndex < nulls.length && i == nulls[nullsIndex]) {
                        nullsIndex++;
                        continue;
                    }

                    final INBT itemNBT = listNBT.get(i - nullsIndex);
                    if (itemNBT == null) {
                        continue;
                    }

                    data[i] = componentDeserializer.deserialize(itemNBT, componentType, data[i]);
                }

                return data;
            }
        }

        @Override
        public boolean exists(final String name) {
            return nbt.contains(name);
        }

        private boolean isNull(final String name) {
            return nbt.getCompound(name).getBoolean(IS_NULL_KEY);
        }
    }

    ///////////////////////////////////////////////////////////////////

    private interface ArraySerializer {
        INBT serialize(Object value);

        @Nullable
        Object deserialize(INBT nbt, final Class<?> type, @Nullable final Object into);
    }

    private static final class BooleanArraySerializer implements ArraySerializer {
        @Override
        public INBT serialize(final Object value) {
            final boolean[] data = (boolean[]) value;
            final byte[] convertedData = new byte[data.length];
            for (int i = 0; i < data.length; i++) {
                convertedData[i] = data[i] ? (byte) 1 : (byte) 0;
            }
            return new ByteArrayNBT(convertedData);
        }

        @Override
        public Object deserialize(final INBT nbt, final Class<?> type, @Nullable final Object into) {
            boolean[] data = (boolean[]) into;
            if (nbt instanceof ByteArrayNBT) {
                final byte[] convertedData = ((ByteArrayNBT) nbt).getByteArray();
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
        public INBT serialize(final Object value) {
            return new ByteArrayNBT((byte[]) value);
        }

        @Override
        public Object deserialize(final INBT nbt, final Class<?> type, @Nullable final Object into) {
            final byte[] data = (byte[]) into;
            if (nbt instanceof ByteArrayNBT) {
                final byte[] serializedData = ((ByteArrayNBT) nbt).getByteArray();
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
        public INBT serialize(final Object value) {
            final char[] data = (char[]) value;
            final int[] convertedData = new int[data.length];
            for (int i = 0; i < data.length; i++) {
                convertedData[i] = data[i];
            }
            return new IntArrayNBT(convertedData);
        }

        @Override
        public Object deserialize(final INBT nbt, final Class<?> type, @Nullable final Object into) {
            char[] data = (char[]) into;
            if (nbt instanceof IntArrayNBT) {
                final int[] convertedData = ((IntArrayNBT) nbt).getIntArray();
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
        public INBT serialize(final Object value) {
            final short[] data = (short[]) value;
            final int[] convertedData = new int[data.length];
            for (int i = 0; i < data.length; i++) {
                convertedData[i] = data[i];
            }
            return new IntArrayNBT(convertedData);
        }

        @Override
        public Object deserialize(final INBT nbt, final Class<?> type, @Nullable final Object into) {
            short[] data = (short[]) into;
            if (nbt instanceof IntArrayNBT) {
                final int[] convertedData = ((IntArrayNBT) nbt).getIntArray();
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
        public INBT serialize(final Object value) {
            return new IntArrayNBT((int[]) value);
        }

        @Override
        public Object deserialize(final INBT nbt, final Class<?> type, @Nullable final Object into) {
            final int[] data = (int[]) into;
            if (nbt instanceof IntArrayNBT) {
                final int[] serializedData = ((IntArrayNBT) nbt).getIntArray();
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
        public INBT serialize(final Object value) {
            return new LongArrayNBT((long[]) value);
        }

        @Override
        public Object deserialize(final INBT nbt, final Class<?> type, @Nullable final Object into) {
            final long[] data = (long[]) into;
            if (nbt instanceof LongArrayNBT) {
                final long[] serializedData = ((LongArrayNBT) nbt).getAsLongArray();
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
        public INBT serialize(final Object value) {
            final float[] data = (float[]) value;
            final int[] convertedData = new int[data.length];
            for (int i = 0; i < data.length; i++) {
                convertedData[i] = Float.floatToRawIntBits(data[i]);
            }
            return new IntArrayNBT(convertedData);
        }

        @Override
        public Object deserialize(final INBT nbt, final Class<?> type, @Nullable final Object into) {
            float[] data = (float[]) into;
            if (nbt instanceof IntArrayNBT) {
                final int[] convertedData = ((IntArrayNBT) nbt).getIntArray();
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
        public INBT serialize(final Object value) {
            final double[] data = (double[]) value;
            final long[] convertedData = new long[data.length];
            for (int i = 0; i < data.length; i++) {
                convertedData[i] = Double.doubleToRawLongBits(data[i]);
            }
            return new LongArrayNBT(convertedData);
        }

        @Override
        public Object deserialize(final INBT nbt, final Class<?> type, @Nullable final Object into) {
            double[] data = (double[]) into;
            if (nbt instanceof LongArrayNBT) {
                final long[] convertedData = ((LongArrayNBT) nbt).getAsLongArray();
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
        public INBT serialize(final Object value) {
            final Enum[] data = (Enum[]) value;
            final int[] convertedData = new int[data.length];
            for (int i = 0; i < data.length; i++) {
                convertedData[i] = data[i].ordinal();
            }
            return new IntArrayNBT(convertedData);
        }

        @Override
        public Object deserialize(final INBT nbt, final Class<?> type, @Nullable final Object into) {
            final Class<?> componentType = type.getComponentType();
            final Object[] enumConstants = componentType.getEnumConstants();

            Enum[] data = (Enum[]) into;
            if (nbt instanceof IntArrayNBT) {
                final int[] serializedData = ((IntArrayNBT) nbt).getIntArray();
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
        public INBT serialize(final Object value) {
            final String[] data = (String[]) value;
            final ListNBT list = new ListNBT();
            for (final String datum : data) {
                list.add(StringNBT.valueOf(datum));
            }
            return list;
        }

        @Override
        public Object deserialize(final INBT nbt, final Class<?> type, @Nullable final Object into) {
            String[] data = (String[]) into;
            if (nbt instanceof ListNBT) {
                final ListNBT serializedData = (ListNBT) nbt;
                if (serializedData.isEmpty() || serializedData.getTagType() == NBTTagIds.TAG_STRING) {
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
        public INBT serialize(final Object value) {
            final UUID[] data = (UUID[]) value;
            final ListNBT list = new ListNBT();
            for (final UUID datum : data) {
                list.add(StringNBT.valueOf(datum.toString()));
            }
            return list;
        }

        @Override
        public Object deserialize(final INBT nbt, final Class<?> type, @Nullable final Object into) {
            UUID[] data = (UUID[]) into;
            if (nbt instanceof ListNBT) {
                final ListNBT serializedData = (ListNBT) nbt;
                if (serializedData.isEmpty() || serializedData.getTagType() == NBTTagIds.TAG_STRING) {
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
