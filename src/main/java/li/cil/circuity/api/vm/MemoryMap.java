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

    Optional<MemoryRange> getMemoryRange(final MemoryMappedDevice device);

    @Nullable
    MemoryRange getMemoryRange(final int address);

    void setDirty(final MemoryRange range, final int offset);

    int load(final int address, final int sizeLog2) throws MemoryAccessException;

    void store(final int address, final int value, final int sizeLog2) throws MemoryAccessException;
}
