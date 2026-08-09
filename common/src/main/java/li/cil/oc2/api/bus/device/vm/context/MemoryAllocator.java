/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.bus.device.vm.context;

/**
 * A memory allocator used to ensure sandbox limits when loading devices.
 */
public interface MemoryAllocator {
    /**
     * Tries to reserve the specified amount of memory, in bytes.
     *
     * @param size the amount of memory to reserve, in bytes.
     * @return {@code true} when the memory was claimed successfully; {@code false} otherwise.
     */
    boolean claimMemory(int size);
}
