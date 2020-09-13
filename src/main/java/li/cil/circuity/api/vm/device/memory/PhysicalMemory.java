package li.cil.circuity.api.vm.device.memory;

import li.cil.circuity.api.vm.MemoryMap;

/**
 * Instances marked with this interface can be treated as random-access memory.
 * <p>
 * {@link MemoryMap}s may use this to decide whether a memory
 * region can be stored in a translation lookaside buffer.
 */
public interface PhysicalMemory extends MemoryMappedDevice {
}
