package li.cil.oc2.api.bus.device.vm;

import li.cil.sedna.api.device.MemoryMappedDevice;

import java.util.OptionalLong;

public interface MemoryRangeAllocator {
    OptionalLong claimMemoryRange(long address, MemoryMappedDevice device);

    OptionalLong claimMemoryRange(MemoryMappedDevice device);
}
