package li.cil.oc2.serialization;

import li.cil.ceres.Ceres;
import li.cil.ceres.api.DeserializationVisitor;
import li.cil.ceres.api.SerializationException;
import li.cil.ceres.api.SerializationVisitor;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraftforge.common.util.Constants;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nullable;
import java.lang.reflect.Array;
import java.util.UUID;

public final class NBTSerialization {
    private static final String IS_NULL_SUFFIX = ".is_null";

    public static CompoundNBT serialize(final Object value) throws SerializationException {
        final CompoundNBT nbt = new CompoundNBT();
        Ceres.getSerializer(value.getClass()).serialize(new Serializer(nbt), value);
        return nbt;
    }

    @SuppressWarnings("unchecked")
    public static <T> T deserialize(final CompoundNBT nbt, final T into) throws SerializationException {
        return deserialize(nbt, (Class<T>) into.getClass(), into);
    }

    public static <T> T deserialize(final CompoundNBT nbt, final Class<T> type, @Nullable final T into) throws SerializationException {
        return Ceres.getSerializer(type).deserialize(new Deserializer(nbt), type, into);
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

            if (type == boolean[].class) {
                final boolean[] data = (boolean[]) value;
                final byte[] convertedData = new byte[data.length];
                for (int i = 0; i < data.length; i++) {
                    convertedData[i] = data[i] ? (byte) 1 : (byte) 0;
                }
                nbt.putByteArray(name, convertedData);
            } else if (type == byte[].class) {
                nbt.putByteArray(name, (byte[]) value);
            } else if (type == char[].class) {
                final char[] data = (char[]) value;
                final int[] convertedData = new int[data.length];
                for (int i = 0; i < data.length; i++) {
                    convertedData[i] = data[i];
                }
                nbt.putIntArray(name, convertedData);
            } else if (type == short[].class) {
                final short[] data = (short[]) value;
                final int[] convertedData = new int[data.length];
                for (int i = 0; i < data.length; i++) {
                    convertedData[i] = data[i];
                }
                nbt.putIntArray(name, convertedData);
            } else if (type == int[].class) {
                nbt.putIntArray(name, (int[]) value);
            } else if (type == long[].class) {
                nbt.putLongArray(name, (long[]) value);
            } else if (type == float[].class) {
                final float[] data = (float[]) value;
                final int[] convertedData = new int[data.length];
                for (int i = 0; i < data.length; i++) {
                    convertedData[i] = Float.floatToRawIntBits(data[i]);
                }
                nbt.putIntArray(name, convertedData);
            } else if (type == double[].class) {
                final double[] data = (double[]) value;
                final long[] convertedData = new long[data.length];
                for (int i = 0; i < data.length; i++) {
                    convertedData[i] = Double.doubleToRawLongBits(data[i]);
                }
                nbt.putLongArray(name, convertedData);
            } else if (type.isArray()) {
                final Class<?> componentType = type.getComponentType();
                final li.cil.ceres.api.Serializer<?> serializer = Ceres.getSerializer(componentType);
                final Object[] data = (Object[]) value;
                final ListNBT listNBT = new ListNBT();
                for (final Object datum : data) {
                    final CompoundNBT itemNBT = new CompoundNBT();
                    if (datum == null) {
                        itemNBT.putBoolean(IS_NULL_SUFFIX, true);
                    } else {
                        if (datum.getClass() != componentType) {
                            throw new SerializationException(String.format("Polymorphism detected in generic array [%s]. This is not supported.", name));
                        }
                        serializer.serialize(new Serializer(itemNBT), (Class) componentType, datum);
                    }
                    listNBT.add(itemNBT);
                }
                nbt.put(name, listNBT);
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
            nbt.putBoolean(name + IS_NULL_SUFFIX, isNull);
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

            if (type == boolean[].class) {
                final byte[] convertedData = nbt.getByteArray(name);
                final boolean[] data = new boolean[convertedData.length];
                for (int i = 0; i < convertedData.length; i++) {
                    data[i] = convertedData[i] != 0;
                }
                return data;
            } else if (type == byte[].class) {
                return nbt.getByteArray(name);
            } else if (type == char[].class) {
                final int[] convertedData = nbt.getIntArray(name);
                final char[] data = new char[convertedData.length];
                for (int i = 0; i < convertedData.length; i++) {
                    data[i] = (char) convertedData[i];
                }
                return data;
            } else if (type == short[].class) {
                final int[] convertedData = nbt.getIntArray(name);
                final short[] data = new short[convertedData.length];
                for (int i = 0; i < convertedData.length; i++) {
                    data[i] = (short) convertedData[i];
                }
                return data;
            } else if (type == int[].class) {
                return nbt.getIntArray(name);
            } else if (type == long[].class) {
                return nbt.getLongArray(name);
            } else if (type == float[].class) {
                final int[] convertedData = nbt.getIntArray(name);
                final float[] data = new float[convertedData.length];
                for (int i = 0; i < convertedData.length; i++) {
                    data[i] = Float.intBitsToFloat(convertedData[i]);
                }
                return data;
            } else if (type == double[].class) {
                final long[] convertedData = nbt.getLongArray(name);
                final double[] data = new double[convertedData.length];
                for (int i = 0; i < convertedData.length; i++) {
                    data[i] = Double.longBitsToDouble(convertedData[i]);
                }
                return data;
            } else if (type.isArray()) {
                final Class<?> componentType = type.getComponentType();
                final li.cil.ceres.api.Serializer<?> serializer = Ceres.getSerializer(componentType);
                final ListNBT listNBT = nbt.getList(name, Constants.NBT.TAG_COMPOUND);
                final Object[] data = (Object[]) Array.newInstance(componentType, listNBT.size());
                for (int i = 0; i < listNBT.size(); i++) {
                    final CompoundNBT itemNBT = listNBT.getCompound(i);
                    if (itemNBT.contains(IS_NULL_SUFFIX)) {
                        continue;
                    }

                    data[i] = serializer.deserialize(new Deserializer(itemNBT), (Class) componentType, null);
                }
                return data;
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
            return nbt.contains(name) || nbt.contains(name + IS_NULL_SUFFIX);
        }

        private boolean isNull(final String name) {
            return nbt.getBoolean(name + IS_NULL_SUFFIX);
        }
    }
}
