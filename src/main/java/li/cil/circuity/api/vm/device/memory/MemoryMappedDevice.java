package li.cil.circuity.api.vm.device.memory;

import li.cil.circuity.api.vm.MemoryMap;
import li.cil.circuity.api.vm.device.Device;

/**
 * {@link MemoryMappedDevice}s can be registered with a {@link MemoryMap}
 * so that they can be accessed via a memory range using the same mechanisms used for accessing RAM.
 */
public interface MemoryMappedDevice extends Device {
    int getLength();

    int load(final int offset, final int sizeLog2) throws MemoryAccessException;

    void store(final int offset, final int value, final int sizeLog2) throws MemoryAccessException;
}
