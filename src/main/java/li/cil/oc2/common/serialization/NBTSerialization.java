package li.cil.oc2.common.serialization;

import li.cil.ceres.Ceres;
import li.cil.ceres.api.DeserializationVisitor;
import li.cil.ceres.api.SerializationException;
import li.cil.ceres.api.SerializationVisitor;
import li.cil.oc2.common.util.NBTTagIds;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nullable;
import java.lang.reflect.Array;
import java.util.UUID;

public final class NBTSerialization {
    public static <T> void serialize(final CompoundNBT nbt, final T value) throws SerializationException {
        @SuppressWarnings("unchecked") final Class<T> type = (Class<T>) value.getClass();
        Ceres.getSerializer(type).serialize(new Serializer(nbt), type, value);
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
                final Class<?> componentType = type.getComponentType();

                if (componentType == boolean.class) {
                    final boolean[] data = (boolean[]) value;
                    final byte[] convertedData = new byte[data.length];
                    for (int i = 0; i < data.length; i++) {
                        convertedData[i] = data[i] ? (byte) 1 : (byte) 0;
                    }
                    nbt.putByteArray(name, convertedData);
                } else if (componentType == byte.class) {
                    nbt.putByteArray(name, (byte[]) value);
                } else if (componentType == char.class) {
                    final char[] data = (char[]) value;
                    final int[] convertedData = new int[data.length];
                    for (int i = 0; i < data.length; i++) {
                        convertedData[i] = data[i];
                    }
                    nbt.putIntArray(name, convertedData);
                } else if (componentType == short.class) {
                    final short[] data = (short[]) value;
                    final int[] convertedData = new int[data.length];
                    for (int i = 0; i < data.length; i++) {
                        convertedData[i] = data[i];
                    }
                    nbt.putIntArray(name, convertedData);
                } else if (componentType == int.class) {
                    nbt.putIntArray(name, (int[]) value);
                } else if (componentType == long.class) {
                    nbt.putLongArray(name, (long[]) value);
                } else if (componentType == float.class) {
                    final float[] data = (float[]) value;
                    final int[] convertedData = new int[data.length];
                    for (int i = 0; i < data.length; i++) {
                        convertedData[i] = Float.floatToRawIntBits(data[i]);
                    }
                    nbt.putIntArray(name, convertedData);
                } else if (componentType == double.class) {
                    final double[] data = (double[]) value;
                    final long[] convertedData = new long[data.length];
                    for (int i = 0; i < data.length; i++) {
                        convertedData[i] = Double.doubleToRawLongBits(data[i]);
                    }
                    nbt.putLongArray(name, convertedData);
                } else if (componentType.isEnum()) {
                    final Enum[] data = (Enum[]) value;
                    final int[] convertedData = new int[data.length];
                    for (int i = 0; i < data.length; i++) {
                        convertedData[i] = data[i].ordinal();
                    }
                    nbt.putIntArray(name, convertedData);
                } else if (componentType == UUID.class) {
                    final UUID[] data = (UUID[]) value;
                    final ListNBT list = new ListNBT();
                    for (final UUID datum : data) {
                        list.add(StringNBT.valueOf(datum.toString()));
                    }
                    nbt.put(name, list);
                } else if (componentType == String.class) {
                    final String[] data = (String[]) value;
                    final ListNBT list = new ListNBT();
                    for (final String datum : data) {
                        list.add(StringNBT.valueOf(datum));
                    }
                    nbt.put(name, list);
                } else {
                    final li.cil.ceres.api.Serializer<?> serializer = Ceres.getSerializer(componentType);
                    final Object[] data = (Object[]) value;
                    final ListNBT listNBT = new ListNBT();
                    for (final Object datum : data) {
                        final CompoundNBT itemNBT = new CompoundNBT();
                        if (datum == null) {
                            itemNBT.putBoolean(IS_NULL_KEY, true);
                        } else {
                            if (datum.getClass() != componentType) {
                                throw new SerializationException(String.format("Polymorphism detected in generic array [%s]. This is not supported.", name));
                            }
                            serializer.serialize(new Serializer(itemNBT), (Class) componentType, datum);
                        }
                        listNBT.add(itemNBT);
                    }
                    nbt.put(name, listNBT);
                }
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
                final Class<?> componentType = type.getComponentType();

                if (componentType == boolean.class) {
                    boolean[] data = (boolean[]) into;
                    if (nbt.contains(name, NBTTagIds.TAG_BYTE_ARRAY)) {
                        final byte[] convertedData = nbt.getByteArray(name);
                        if (data == null || data.length != convertedData.length) {
                            data = new boolean[convertedData.length];
                        }
                        for (int i = 0; i < convertedData.length; i++) {
                            data[i] = convertedData[i] != 0;
                        }
                    }
                    return data;
                } else if (componentType == byte.class) {
                    final byte[] data = (byte[]) into;
                    if (nbt.contains(name, NBTTagIds.TAG_BYTE_ARRAY)) {
                        final byte[] serializedData = nbt.getByteArray(name);
                        if (data == null || data.length != serializedData.length) {
                            return serializedData;
                        }
                        System.arraycopy(serializedData, 0, data, 0, serializedData.length);
                    }
                    return data;
                } else if (componentType == char.class) {
                    char[] data = (char[]) into;
                    if (nbt.contains(name, NBTTagIds.TAG_INT_ARRAY)) {
                        final int[] convertedData = nbt.getIntArray(name);
                        if (data == null || data.length != convertedData.length) {
                            data = new char[convertedData.length];
                        }
                        for (int i = 0; i < convertedData.length; i++) {
                            data[i] = (char) convertedData[i];
                        }
                    }
                    return data;
                } else if (componentType == short.class) {
                    short[] data = (short[]) into;
                    if (nbt.contains(name, NBTTagIds.TAG_INT_ARRAY)) {
                        final int[] convertedData = nbt.getIntArray(name);
                        if (data == null || data.length != convertedData.length) {
                            data = new short[convertedData.length];
                        }
                        for (int i = 0; i < convertedData.length; i++) {
                            data[i] = (short) convertedData[i];
                        }
                    }
                    return data;
                } else if (componentType == int.class) {
                    final int[] data = (int[]) into;
                    if (nbt.contains(name, NBTTagIds.TAG_INT_ARRAY)) {
                        final int[] serializedData = nbt.getIntArray(name);
                        if (data == null || data.length != serializedData.length) {
                            return serializedData;
                        }
                        System.arraycopy(serializedData, 0, data, 0, serializedData.length);
                    }
                    return data;
                } else if (componentType == long.class) {
                    final long[] data = (long[]) into;
                    if (nbt.contains(name, NBTTagIds.TAG_LONG_ARRAY)) {
                        final long[] serializedData = nbt.getLongArray(name);
                        if (data == null || data.length != serializedData.length) {
                            return serializedData;
                        }
                        System.arraycopy(serializedData, 0, data, 0, serializedData.length);
                    }
                    return data;
                } else if (componentType == float.class) {
                    float[] data = (float[]) into;
                    if (nbt.contains(name, NBTTagIds.TAG_INT_ARRAY)) {
                        final int[] convertedData = nbt.getIntArray(name);
                        if (data == null || data.length != convertedData.length) {
                            data = new float[convertedData.length];
                        }
                        for (int i = 0; i < convertedData.length; i++) {
                            data[i] = Float.intBitsToFloat(convertedData[i]);
                        }
                    }
                    return data;
                } else if (componentType == double.class) {
                    double[] data = (double[]) into;
                    if (nbt.contains(name, NBTTagIds.TAG_LONG_ARRAY)) {
                        final long[] convertedData = nbt.getLongArray(name);
                        if (data == null || data.length != convertedData.length) {
                            data = new double[convertedData.length];
                        }
                        for (int i = 0; i < convertedData.length; i++) {
                            data[i] = Double.longBitsToDouble(convertedData[i]);
                        }
                    }
                    return data;
                } else if (componentType.isEnum()) {
                    Enum[] data = (Enum[]) into;
                    if (nbt.contains(name, NBTTagIds.TAG_INT_ARRAY)) {
                        final int[] serializedData = nbt.getIntArray(name);
                        if (data == null || data.length != serializedData.length) {
                            data = (Enum[]) Array.newInstance(componentType, serializedData.length);
                        }
                        for (int i = 0; i < serializedData.length; i++) {
                            data[i] = (Enum) componentType.getEnumConstants()[serializedData[i]];
                        }
                    }
                    return data;
                } else if (componentType == String.class) {
                    String[] data = (String[]) into;
                    if (nbt.contains(name, NBTTagIds.TAG_LIST)) {
                        final ListNBT serializedData = nbt.getList(name, NBTTagIds.TAG_STRING);
                        if (data == null || data.length != serializedData.size()) {
                            data = new String[serializedData.size()];
                        }
                        for (int i = 0; i < serializedData.size(); i++) {
                            data[i] = serializedData.getString(i);
                        }
                    }
                    return data;
                } else if (componentType == UUID.class) {
                    UUID[] data = (UUID[]) into;
                    if (nbt.contains(name, NBTTagIds.TAG_LIST)) {
                        final ListNBT serializedData = nbt.getList(name, NBTTagIds.TAG_STRING);
                        if (data == null || data.length != serializedData.size()) {
                            data = new UUID[serializedData.size()];
                        }
                        for (int i = 0; i < serializedData.size(); i++) {
                            data[i] = UUID.fromString(serializedData.getString(i));
                        }
                    }
                    return data;
                } else {
                    Object[] data = (Object[]) into;
                    if (nbt.contains(name, NBTTagIds.TAG_LIST)) {
                        final ListNBT listNBT = nbt.getList(name, NBTTagIds.TAG_COMPOUND);
                        final int length = listNBT.size();
                        if (data == null || data.length != length) {
                            data = (Object[]) Array.newInstance(componentType, length);
                        }

                        final li.cil.ceres.api.Serializer<?> serializer = Ceres.getSerializer(componentType);
                        for (int i = 0; i < length; i++) {
                            final CompoundNBT itemNBT = listNBT.getCompound(i);
                            if (itemNBT.contains(IS_NULL_KEY)) {
                                continue;
                            }

                            data[i] = serializer.deserialize(new Deserializer(itemNBT), (Class) componentType, data[i]);
                        }
                    }
                    return data;
                }
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

        @Override
        public boolean exists(final String name) {
            return nbt.contains(name);
        }

        private boolean isNull(final String name) {
            return nbt.getCompound(name).getBoolean(IS_NULL_KEY);
        }
    }
}
