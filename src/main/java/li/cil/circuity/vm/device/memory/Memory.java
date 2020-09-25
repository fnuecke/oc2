package li.cil.circuity.vm.device.memory;

import li.cil.circuity.api.vm.device.memory.PhysicalMemory;

import java.nio.ByteOrder;

public final class Memory {
    public static PhysicalMemory create(final int sizeInBytes) {
        if (ByteOrder.nativeOrder() != ByteOrder.LITTLE_ENDIAN) {
            return new ByteBufferMemory(sizeInBytes);
        } else {
            return new UnsafeMemory(sizeInBytes);
        }
    }
}
