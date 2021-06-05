package li.cil.oc2.common.serialization;

import li.cil.ceres.api.Serialized;
import net.minecraft.nbt.CompoundNBT;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public final class SerializationTests {
    @Test
    public void testSerializeFlat() {
        final Flat value = new Flat();

        final UUID uuid = UUID.randomUUID();
        value.byteValue = 123;
        value.shortValue = 234;
        value.intValue = 456;
        value.longValue = 567;
        value.floatValue = 678.9f;
        value.doubleValue = 789.0;
        value.byteArrayValue = new byte[]{1, 2, 3};
        value.intArrayValue = new int[]{4, 5, 6};
        value.longArrayValue = new long[]{7, 8, 9};
        value.stringValue = "test string";
        value.uuidValue = uuid;

        final CompoundNBT nbt = assertDoesNotThrow(() -> NBTSerialization.serialize(value));

        assertEquals(123, nbt.getByte("byteValue"));
        assertEquals(234, nbt.getShort("shortValue"));
        assertEquals(456, nbt.getInt("intValue"));
        assertEquals(567, nbt.getLong("longValue"));
        assertEquals(678.9f, nbt.getFloat("floatValue"));
        assertEquals(789.0, nbt.getDouble("doubleValue"));
        assertArrayEquals(new byte[]{1, 2, 3}, nbt.getByteArray("byteArrayValue"));
        assertArrayEquals(new int[]{4, 5, 6}, nbt.getIntArray("intArrayValue"));
        assertArrayEquals(new long[]{7, 8, 9}, nbt.getLongArray("longArrayValue"));
        assertEquals("test string", nbt.getString("stringValue"));
        assertEquals(uuid, nbt.getCompound("uuidValue").getUUID("uuidValue"));
    }

    @Test
    public void testDeserializeFlatInto() {
        final CompoundNBT nbt = new CompoundNBT();
        nbt.putByte("byteValue", (byte) 98);
        nbt.putShort("shortValue", (short) 876);
        nbt.putInt("intValue", 765);
        nbt.putLong("longValue", 654);
        nbt.putFloat("floatValue", 543.2f);
        nbt.putDouble("doubleValue", 432.1);
        nbt.putByteArray("byteArrayValue", new byte[]{9, 8, 7});
        nbt.putIntArray("intArrayValue", new int[]{8, 7, 6});
        nbt.putLongArray("longArrayValue", new long[]{7, 6, 5});
        nbt.putString("stringValue", "another test");
        final UUID uuid = UUID.randomUUID();
        final CompoundNBT uuidNBT = new CompoundNBT();
        uuidNBT.putUUID("uuidValue", uuid);
        nbt.put("uuidValue", uuidNBT);

        final Flat value = assertDoesNotThrow(() -> NBTSerialization.deserialize(nbt, Flat.class, new Flat()));

        assertEquals(98, value.byteValue);
        assertEquals(876, value.shortValue);
        assertEquals(765, value.intValue);
        assertEquals(654, value.longValue);
        assertEquals(543.2f, value.floatValue);
        assertEquals(432, .1, value.doubleValue);
        assertArrayEquals(new byte[]{9, 8, 7}, value.byteArrayValue);
        assertArrayEquals(new int[]{8, 7, 6}, value.intArrayValue);
        assertArrayEquals(new long[]{7, 6, 5}, value.longArrayValue);
        assertEquals("another test", value.stringValue);
        assertEquals(uuid, value.uuidValue);
    }

    @Test
    public void testDeserializeFlatNew() {
        final CompoundNBT nbt = new CompoundNBT();

        nbt.putByte("byteValue", (byte) 98);
        nbt.putShort("shortValue", (short) 876);
        nbt.putInt("intValue", 765);
        nbt.putLong("longValue", 654);
        nbt.putFloat("floatValue", 543.2f);
        nbt.putDouble("doubleValue", 432.1);
        nbt.putByteArray("byteArrayValue", new byte[]{9, 8, 7});
        nbt.putIntArray("intArrayValue", new int[]{8, 7, 6});
        nbt.putLongArray("longArrayValue", new long[]{7, 6, 5});
        nbt.putString("stringValue", "another test");
        final UUID uuid = UUID.randomUUID();
        final CompoundNBT uuidNBT = new CompoundNBT();
        uuidNBT.putUUID("uuidValue", uuid);
        nbt.put("uuidValue", uuidNBT);

        final Flat value = assertDoesNotThrow(() -> NBTSerialization.deserialize(nbt, Flat.class, null));

        assertEquals(98, value.byteValue);
        assertEquals(876, value.shortValue);
        assertEquals(765, value.intValue);
        assertEquals(654, value.longValue);
        assertEquals(543.2f, value.floatValue);
        assertEquals(432, .1, value.doubleValue);
        assertArrayEquals(new byte[]{9, 8, 7}, value.byteArrayValue);
        assertArrayEquals(new int[]{8, 7, 6}, value.intArrayValue);
        assertArrayEquals(new long[]{7, 6, 5}, value.longArrayValue);
        assertEquals("another test", value.stringValue);
        assertEquals(uuid, value.uuidValue);
    }

    @Test
    public void testModifiers() {
        final WithModifiers value = new WithModifiers();
        final CompoundNBT nbt = assertDoesNotThrow(() -> NBTSerialization.serialize(value));

        assertTrue(nbt.contains("nonTransientInt"));
        assertEquals(123, nbt.getInt("nonTransientInt"));
        assertFalse(nbt.contains("transientInt"));
        assertFalse(nbt.contains("finalInt"));

        nbt.putIntArray("finalIntArray", new int[]{8, 7, 6});

        assertDoesNotThrow(() -> NBTSerialization.deserialize(nbt, value));

        assertArrayEquals(new int[]{8, 7, 6}, value.finalIntArray);
    }

    @Test
    public void testSerializeNested() {
        final Nested root = new Nested();
        root.value = 123;
        root.child = new Nested();
        root.child.value = 234;

        final CompoundNBT nbt = assertDoesNotThrow(() -> NBTSerialization.serialize(root));

        assertEquals(123, nbt.getInt("value"));
        assertTrue(nbt.contains("child"));
        assertEquals(234, nbt.getCompound("child").getInt("value"));
    }

    @Test
    public void testDeserializeNestedInto() {
        final CompoundNBT nbt = new CompoundNBT();
        nbt.putInt("value", 123);
        final CompoundNBT child = new CompoundNBT();
        nbt.put("child", child);
        child.putInt("value", 234);

        final Nested value = assertDoesNotThrow(() -> NBTSerialization.deserialize(nbt, Nested.class, new Nested()));

        assertEquals(123, value.value);
        assertEquals(234, value.child.value);
        assertNull(value.child.child);
    }

    @Test
    public void testDeserializeNestedNew() {
        final CompoundNBT nbt = new CompoundNBT();
        nbt.putInt("value", 123);
        final CompoundNBT child = new CompoundNBT();
        nbt.put("child", child);
        child.putInt("value", 234);

        final Nested value = assertDoesNotThrow(() -> NBTSerialization.deserialize(nbt, Nested.class, null));

        assertEquals(123, value.value);
        assertEquals(234, value.child.value);
        assertNull(value.child.child);
    }

    @Serialized
    private static final class Flat {
        private byte byteValue;
        private short shortValue;
        private int intValue;
        private long longValue;
        private float floatValue;
        private double doubleValue;
        private byte[] byteArrayValue;
        private int[] intArrayValue;
        private long[] longArrayValue;
        private String stringValue;
        private UUID uuidValue;
    }

    @Serialized
    private static final class WithModifiers {
        private int nonTransientInt = 123;
        private transient int transientInt = 345;
        private final int finalInt = 678;
        private final int[] finalIntArray = {4, 5, 6};
    }

    @Serialized
    private static final class Nested {
        private int value;
        private Nested child;
    }
}
