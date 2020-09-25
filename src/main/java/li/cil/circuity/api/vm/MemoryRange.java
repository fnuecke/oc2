package li.cil.circuity.api.vm;

import li.cil.circuity.api.vm.device.memory.MemoryMappedDevice;

import java.util.Objects;

/**
 * Represents a segment of memory mapped by a {@link MemoryMap}.
 */
public final class MemoryRange {
    /**
     * The device assigned to this memory range.
     */
    public final MemoryMappedDevice device;

    /**
     * The first byte-aligned address inside this memory range (inclusive).
     */
    public final int start;

    /**
     * The last byte-aligned address inside this memory range (inclusive).
     */
    public final int end;

    public MemoryRange(final MemoryMappedDevice device, final int start, final int end) {
        if (Integer.compareUnsigned(start, end) > 0) {
            throw new IllegalArgumentException();
        }

        this.device = device;
        this.start = start;
        this.end = end;
    }

    public MemoryRange(final MemoryMappedDevice device, final int address) {
        this(device, address, address + device.getLength() - 1);
    }

    /**
     * The address of this memory range.
     * <p>
     * This is the same as {@link #start}.
     *
     * @return the address of this memory range.
     */
    public int address() {
        return start;
    }

    /**
     * The size of this memory range, in bytes.
     *
     * @return the size of this memory range.
     */
    public final int size() {
        return end - start + 1;
    }

    /**
     * Checks if the specified address is contained within this memory range.
     *
     * @param address the address to check for.
     * @return {@code true} if the address falls into this memory range; {@code false} otherwise.
     */
    public boolean contains(final int address) {
        return Integer.compareUnsigned(address, start) >= 0 && Integer.compareUnsigned(address, end) <= 0;
    }

    /**
     * Checks if the specified memory range intersects with this memory range.
     *
     * @param other the memory range to check for.
     * @return {@code true} if the memory range intersects this memory range; {@code false} otherwise.
     */
    public boolean intersects(final MemoryRange other) {
        return Integer.compareUnsigned(start, other.end) <= 0 && Integer.compareUnsigned(end, other.start) >= 0;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final MemoryRange that = (MemoryRange) o;
        return start == that.start &&
               end == that.end &&
               device.equals(that.device);
    }

    @Override
    public int hashCode() {
        return Objects.hash(device, start, end);
    }

    @Override
    public String toString() {
        return String.format("%s@[%x-%x]", device, start, end);
    }
}
