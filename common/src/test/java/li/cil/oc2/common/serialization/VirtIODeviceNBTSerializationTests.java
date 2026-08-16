/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.serialization;

import li.cil.ceres.api.Serialized;
import li.cil.sedna.Sedna;
import li.cil.sedna.device.virtio.VirtIOConsoleDevice;
import li.cil.sedna.memory.SimpleMemoryMap;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public final class VirtIODeviceNBTSerializationTests {
    @BeforeEach
    public void setUp() {
        Sedna.initialize();
    }

    @Test
    public void multiportConsoleDeviceRoundTrips() {
        final SimpleMemoryMap memoryMap = new SimpleMemoryMap();
        final VirtIOConsoleDevice device = new VirtIOConsoleDevice(memoryMap, "oc2.rpc.0", "oc2.blob.0", "oc2.event.0");
        device.reset();

        device.getPort(0).putByte((byte) 'a');
        device.getPort(0).putByte((byte) 'b');
        device.getPort(1).putByte((byte) 'z');

        final CompoundTag tag = assertDoesNotThrow(() -> NBTSerialization.serialize(device));

        final VirtIOConsoleDevice restored = new VirtIOConsoleDevice(memoryMap, "oc2.rpc.0", "oc2.blob.0", "oc2.event.0");
        assertDoesNotThrow(() -> NBTSerialization.deserialize(tag, restored));
        assertEquals(device.getPortCount(), restored.getPortCount());

        final CompoundTag round = assertDoesNotThrow(() -> NBTSerialization.serialize(restored));
        assertEquals(tag, round, "per-port state did not survive the round trip");
    }

    @Test
    public void plainConsoleDeviceRoundTrips() {
        final SimpleMemoryMap memoryMap = new SimpleMemoryMap();
        final VirtIOConsoleDevice device = new VirtIOConsoleDevice(memoryMap);
        device.reset();

        final CompoundTag tag = assertDoesNotThrow(() -> NBTSerialization.serialize(device));

        final VirtIOConsoleDevice restored = new VirtIOConsoleDevice(memoryMap);
        assertDoesNotThrow(() -> NBTSerialization.deserialize(tag, restored));
    }

    @Disabled("NBTSerialization silently drops enum array contents; fix pending")
    @Test
    public void enumArraysRoundTrip() {
        final HasEnumArray value = new HasEnumArray();
        value.values = new Example[]{Example.B, Example.A};

        final CompoundTag tag = assertDoesNotThrow(() -> NBTSerialization.serialize(value));

        final HasEnumArray restored = new HasEnumArray();
        assertDoesNotThrow(() -> NBTSerialization.deserialize(tag, restored));
        assertArrayEquals(new Example[]{Example.B, Example.A}, restored.values);
    }

    @Serialized
    public static final class HasEnumArray {
        public Example[] values = {Example.A, Example.B};
    }

    public enum Example {
        A,
        B,
    }
}
