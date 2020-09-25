package li.cil.circuity.api.vm.device.memory;

/**
 * Instances marked with this interface can be treated as random-access memory.
 * <p>
 * For example, CPUs may use this to decide whether a memory region can be stored
 * in a translation look-aside buffer.
 * <p>
 * In particular, implementing this interface communicates that values written to
 * this device can be read back from the same address, and that the {@link li.cil.circuity.api.vm.MemoryRange}
 * they occupy can be used as a continuous whole, without any inaccessible areas in
 * it.
 */
public interface PhysicalMemory extends MemoryMappedDevice {
}
