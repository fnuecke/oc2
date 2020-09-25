package li.cil.circuity.api.vm;

import li.cil.circuity.api.vm.device.memory.MemoryAccessException;
import li.cil.circuity.api.vm.device.memory.MemoryMappedDevice;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Represents a physical memory mapping of devices.
 */
public interface MemoryMap {
    /**
     * Tries to find a vacant memory range of the specified size in the specified ranged.
     *
     * @param start the minimum starting address for the memory range to find (inclusive).
     * @param end   the maximum starting address for the memory range to find (inclusive).
     * @param size  the size of the memory range to find.
     * @return the address of a free memory range, if one was found.
     */
    OptionalInt findFreeRange(final int start, final int end, final int size);

    /**
     * Tries to add a new device to the mapping at the specified address.
     * <p>
     * This can fail if the new device would overlap a memory range already occupied by
     * an existing device. In that case this method will return <code>false</code>. Use
     * {@link #findFreeRange(int, int, int)} to obtain an address the device can be
     * added at.
     *
     * @param address the address to add the device add.
     * @param device  the device to add.
     * @return <code>true</code> if the device was added; <code>false</code> otherwise.
     */
    boolean addDevice(final int address, final MemoryMappedDevice device);

    /**
     * Removes a device from the memory map.
     * <p>
     * If the device is not in this map this is a no-op.
     *
     * @param device the device to remove.
     */
    void removeDevice(final MemoryMappedDevice device);

    /**
     * Returns the memory range the specified device currently occupies in this mapping, if any.
     *
     * @param device the device to get the memory range for.
     * @return the range the device occupies, if it is in this mapping.
     */
    Optional<MemoryRange> getMemoryRange(final MemoryMappedDevice device);

    /**
     * Returns the memory range that the specified address fall into, if any.
     * <p>
     * This is useful for getting a direct reference to a {@link MemoryMappedDevice} at
     * a specific memory location, usually to perform multiple read or write operations
     * on it without having to go through the slower {@link #load(int, int)} and
     * {@link #store(int, int, int)} calls.
     *
     * @param address the address to get a memory range for.
     * @return the memory range the address falls into, if any.
     */
    @Nullable
    MemoryRange getMemoryRange(final int address);

    /**
     * Marks a location in memory dirty.
     * <p>
     * This may be called by systems in parallel to performing actual store operations
     * directly on {@link MemoryMappedDevice}s.
     *
     * @param range  the memory range in which data has changed.
     * @param offset the offset inside that memory range at which data has changed.
     */
    void setDirty(final MemoryRange range, final int offset);

    /**
     * Reads a value from the specified physical address.
     * <p>
     * When performing many operations on an address range that is known to be occupied by
     * a single device, it is more efficient to obtain a reference to that device via
     * {@link #getMemoryRange(int)} instead and operate on that device directly.
     *
     * @param address  the physical address to read a value from.
     * @param sizeLog2 the size of the value to read. See {@link li.cil.circuity.api.vm.device.memory.Sizes}.
     * @return the value read from the specified location.
     * @throws MemoryAccessException if an error occurred accessing the memory a the specified location.
     */
    int load(final int address, final int sizeLog2) throws MemoryAccessException;

    /**
     * Writes a value to the specified physical address.
     * <p>
     * When performing many operations on an address range that is known to be occupied by
     * a single device, it is more efficient to obtain a reference to that device via
     * {@link #getMemoryRange(int)} instead and operate on that device directly.
     *
     * @param address  the physical address to write a value to.
     * @param value    the value to write to the specified location.
     * @param sizeLog2 the size of the value to write. See {@link li.cil.circuity.api.vm.device.memory.Sizes}.
     * @throws MemoryAccessException if an error occurred accessing the memory a the specified location.
     */
    void store(final int address, final int value, final int sizeLog2) throws MemoryAccessException;
}
