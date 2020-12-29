package li.cil.oc2.api.bus.device.vm;

import li.cil.sedna.api.device.MemoryMappedDevice;

import java.util.OptionalLong;

/**
 * Allows adding {@link MemoryMappedDevice}s to the memory map of a virtual machine
 * during a {@link VMDevice#load(VMContext)} call.
 * <p>
 * Allocated addresses should be persisted and used in {@link #claimMemoryRange(long, MemoryMappedDevice)}
 * when restoring from a saved state to ensure correct behaviour of the loaded virtual
 * machine.
 */
public interface MemoryRangeAllocator {
    /**
     * Tries to add a {@link MemoryMappedDevice} to the memory map at the specified
     * address. The returned address may differ from the address provided, if the
     * device cannot fit into the memory map at the specified address. In this case,
     * the result will be the same as calling {@link #claimMemoryRange(MemoryMappedDevice)}.
     *
     * @param address the address to add the specified device at.
     * @param device  the device to add at the specified address.
     * @return the address the device was added at, if any.
     */
    OptionalLong claimMemoryRange(long address, MemoryMappedDevice device);

    /**
     * Tries to add a {@link MemoryMappedDevice} to the memory map at an address
     * determined by the virtual machine. This may take into account the type of
     * device being added. Typically, {@link li.cil.sedna.api.device.PhysicalMemory}
     * devices will be allocated in a different memory region than regular devices.
     * <p>
     * If the device could not fit into the memory map at all, this will return
     * {@link OptionalLong#empty()}.
     *
     * @param device the device to add to the memory map.
     * @return the address the device was added at, if any.
     */
    OptionalLong claimMemoryRange(MemoryMappedDevice device);
}
