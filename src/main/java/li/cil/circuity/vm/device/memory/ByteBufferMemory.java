package li.cil.circuity.vm.device.memory;

import li.cil.circuity.api.vm.device.memory.MemoryAccessException;
import li.cil.circuity.api.vm.device.memory.PhysicalMemory;
import li.cil.circuity.api.vm.device.memory.Sizes;
import li.cil.circuity.vm.device.memory.exception.LoadFaultException;
import li.cil.circuity.vm.device.memory.exception.StoreFaultException;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Simple physical memory implementation backed by a {@link ByteBuffer}.
 */
public class ByteBufferMemory implements PhysicalMemory {
    private final ByteBuffer data;

    public ByteBufferMemory(final int size) {
        if ((size & 0b11) != 0)
            throw new IllegalArgumentException("size must be a multiple of four");
        this.data = ByteBuffer.allocateDirect(size);
        data.order(ByteOrder.LITTLE_ENDIAN);
    }

    @Override
    public int getLength() {
        return data.capacity();
    }

    @Override
    public int load(final int offset, final int sizeLog2) throws MemoryAccessException {
        if (offset < 0 || offset >= data.limit()) {
            throw new LoadFaultException(offset);
        }
        switch (sizeLog2) {
            case Sizes.SIZE_8_LOG2:
                return data.get(offset);
            case Sizes.SIZE_16_LOG2:
                return data.getShort(offset);
            case Sizes.SIZE_32_LOG2:
                return data.getInt(offset);
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public void store(final int offset, final int value, final int sizeLog2) throws MemoryAccessException {
        if (offset < 0 || offset >= data.limit()) {
            throw new StoreFaultException(offset);
        }
        switch (sizeLog2) {
            case Sizes.SIZE_8_LOG2:
                data.put(offset, (byte) value);
                break;
            case Sizes.SIZE_16_LOG2:
                data.putShort(offset, (short) value);
                break;
            case Sizes.SIZE_32_LOG2:
                data.putInt(offset, value);
                break;
            default:
                throw new IllegalArgumentException();
        }
    }
}
