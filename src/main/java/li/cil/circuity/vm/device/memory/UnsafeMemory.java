package li.cil.circuity.vm.device.memory;

import li.cil.circuity.api.vm.device.memory.MemoryAccessException;
import li.cil.circuity.api.vm.device.memory.PhysicalMemory;
import li.cil.circuity.api.vm.device.memory.Sizes;
import sun.misc.Cleaner;
import sun.misc.Unsafe;
import sun.misc.VM;

import java.lang.reflect.Field;

public final class UnsafeMemory implements PhysicalMemory {
    private static final Unsafe unsafe;
    private static int pageSize = -1;

    static {
        final Field f;
        Unsafe instance = null;
        try {
            f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            instance = (Unsafe) f.get(null);
        } catch (final NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }

        unsafe = instance;
    }

    private final long address;
    private final long size;
    private final Cleaner cleaner;

    public UnsafeMemory(final int size) {
        if ((size & 0b11) != 0)
            throw new IllegalArgumentException("size must be a multiple of four");

        // Handling for page aligned memory taken from DirectByteBuffer.

        final boolean isAligned = VM.isDirectMemoryPageAligned();
        final int pageSize = unsafe.pageSize();

        this.size = Math.max(4L, (long) size + (isAligned ? pageSize : 0));

        final long base = unsafe.allocateMemory(this.size);

        unsafe.setMemory(base, this.size, (byte) 0);

        if (isAligned && (base % pageSize != 0)) {
            address = base + pageSize - (base & (pageSize - 1));
        } else {
            address = base;
        }

        cleaner = Cleaner.create(this, new Deallocator(address));
    }

    public void dispose() {
        cleaner.clean();
    }

    @Override
    public int getLength() {
        return (int) size;
    }

    @Override
    public int load(final int offset, final int sizeLog2) throws MemoryAccessException {
        assert  offset >= 0 && offset < size;
        switch (sizeLog2) {
            case Sizes.SIZE_8_LOG2:
                return unsafe.getByte(address + offset);
            case Sizes.SIZE_16_LOG2:
                assert (offset & 0b1) == 0;
                return unsafe.getShort(address + offset);
            case Sizes.SIZE_32_LOG2:
                assert (offset & 0b11) == 0;
                return unsafe.getInt(address + offset);
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public void store(final int offset, final int value, final int sizeLog2) throws MemoryAccessException {
        assert  offset >= 0 && offset < size;
        switch (sizeLog2) {
            case Sizes.SIZE_8_LOG2:
                unsafe.putByte(address + offset, (byte) value);
                break;
            case Sizes.SIZE_16_LOG2:
                assert (offset & 0b1) == 0;
                unsafe.putShort(address + offset, (short) value);
                break;
            case Sizes.SIZE_32_LOG2:
                assert (offset & 0b11) == 0;
                unsafe.putInt(address + offset, value);
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    private static final class Deallocator implements Runnable {
        private long address;

        public Deallocator(final long address) {
            this.address = address;
        }

        @Override
        public void run() {
            if (address != 0) {
                unsafe.freeMemory(address);
                address = 0;
            }
        }
    }
}
