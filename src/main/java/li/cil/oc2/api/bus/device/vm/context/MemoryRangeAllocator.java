/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.bus.device.vm.context;

import li.cil.oc2.api.bus.device.vm.VMDevice;
import li.cil.sedna.api.device.MemoryMappedDevice;

import java.util.OptionalLong;

/**
 * Allows adding {@link MemoryMappedDevice}s to the memory map of a virtual machine
 * during a {@link VMDevice#mount(VMContext)} call.
 * <p>
 * Allocated addresses should be persisted and used in {@link #claimMemoryRange(long, MemoryMappedDevice)}
 * when restoring from a saved state to ensure correct behaviour of the loaded virtual
 * machine.
 */
public interface MemoryRangeAllocator {
    /**
     * Tries to add a {@link MemoryMappedDevice} to the memory map at the specified
     * address. This may fail if some other device is already mapped to part of the
     * range. Use {@link #claimMemoryRange(MemoryMappedDevice)} to claim an unused
     * memory range.
     *
     * @param address the address to add the specified device at.
     * @param device  the device to add at the specified address.
     * @return {@code true} if the memory range could be claimed; {@code false} otherwise.
     */
    boolean claimMemoryRange(long address, MemoryMappedDevice device);

    /**
     * Tries to add a {@link MemoryMappedDevice} to the memory map at an address
     * determined by the virtual machine.
     * <p>
     * This may take into account the type of device being added. For example,
     * {@link li.cil.sedna.api.device.PhysicalMemory} devices will typically be
     * allocated in a different memory region than regular devices.
     * <p>
     * If the device could not fit into the memory map at all, this will return
     * {@link OptionalLong#empty()}.
     *
     * @param device the device to add to the memory map.
     * @return the address the device was added at, if any.
     */
    OptionalLong claimMemoryRange(MemoryMappedDevice device);
}
