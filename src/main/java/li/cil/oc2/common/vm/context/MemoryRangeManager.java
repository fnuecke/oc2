package li.cil.oc2.common.vm.context;

import li.cil.sedna.api.device.MemoryMappedDevice;

import java.util.OptionalLong;

public interface MemoryRangeManager {
    OptionalLong findMemoryRange(MemoryMappedDevice device, long start);

    void releaseMemoryRange(MemoryMappedDevice device);
}
