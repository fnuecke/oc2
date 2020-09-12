package li.cil.circuity.vm.device.memory;

import li.cil.circuity.api.vm.device.memory.MemoryAccessException;
import li.cil.circuity.api.vm.device.memory.PhysicalMemory;
import li.cil.circuity.vm.device.memory.exception.LoadFaultException;
import li.cil.circuity.vm.device.memory.exception.StoreFaultException;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ByteBufferMemory implements PhysicalMemory {
    private final ByteBuffer data;

    public ByteBufferMemory(final int size) {
        if ((size & 0b11) != 0)
            throw new IllegalArgumentException("size must be a multiple of four");
        this.data = ByteBuffer.allocate(size);
        data.order(ByteOrder.LITTLE_ENDIAN);
    }

    @Override
    public int getLength() {
        return data.capacity();
    }

    @Override
    public byte load8(final int offset) throws MemoryAccessException {
        try {
            return data.get(offset);
        } catch (final IndexOutOfBoundsException e) {
            throw new LoadFaultException(offset);
        }
    }

    @Override
    public void store8(final int offset, final byte value) throws MemoryAccessException {
        try {
            data.put(offset, value);
        } catch (final IndexOutOfBoundsException e) {
            throw new StoreFaultException(offset);
        }
    }

    @Override
    public short load16(final int offset) throws MemoryAccessException {
        try {
            return data.getShort(offset);
        } catch (final IndexOutOfBoundsException e) {
            throw new LoadFaultException(offset);
        }
    }

    @Override
    public void store16(final int offset, final short value) throws MemoryAccessException {
        try {
            data.putShort(offset, value);
        } catch (final IndexOutOfBoundsException e) {
            throw new StoreFaultException(offset);
        }
    }

    @Override
    public int load32(final int offset) throws MemoryAccessException {
        try {
            return data.getInt(offset);
        } catch (final IndexOutOfBoundsException e) {
            throw new LoadFaultException(offset);
        }
    }

    @Override
    public void store32(final int offset, final int value) throws MemoryAccessException {
        try {
            data.putInt(offset, value);
        } catch (final IndexOutOfBoundsException e) {
            throw new StoreFaultException(offset);
        }
    }
}
