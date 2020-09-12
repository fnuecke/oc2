package li.cil.circuity.api.vm;

import li.cil.circuity.api.vm.device.memory.MemoryMappedDevice;

import java.util.Objects;

public final class MemoryRange {
    public final MemoryMappedDevice device;
    public final int start, end; // both are inclusive

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

    public boolean contains(final int address) {
        return Integer.compareUnsigned(address, start) >= 0 && Integer.compareUnsigned(address, end) <= 0;
    }

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
