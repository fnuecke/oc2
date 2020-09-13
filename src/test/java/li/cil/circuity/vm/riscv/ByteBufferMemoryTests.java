package li.cil.circuity.vm.riscv;

import li.cil.circuity.api.vm.device.memory.Sizes;
import li.cil.circuity.vm.device.memory.ByteBufferMemory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ByteBufferMemoryTests {
    private ByteBufferMemory memory;

    @BeforeEach
    public void initialize() throws Exception {
        memory = new ByteBufferMemory(4 * 1024);
        memory.store(0x00, 0x11223344, Sizes.SIZE_32_LOG2);
        memory.store(0x10, 0x55667788, Sizes.SIZE_32_LOG2);
        memory.store(0x20, 0x99AABBCC, Sizes.SIZE_32_LOG2);
    }

    @Test
    public void testLoad8() throws Exception {
        Assertions.assertEquals((byte) 0x44, memory.load(0x00 + 0, Sizes.SIZE_8_LOG2));
        Assertions.assertEquals((byte) 0x33, memory.load(0x00 + 1, Sizes.SIZE_8_LOG2));
        Assertions.assertEquals((byte) 0x22, memory.load(0x00 + 2, Sizes.SIZE_8_LOG2));
        Assertions.assertEquals((byte) 0x11, memory.load(0x00 + 3, Sizes.SIZE_8_LOG2));

        Assertions.assertEquals((byte) 0x88, memory.load(0x10 + 0, Sizes.SIZE_8_LOG2));
        Assertions.assertEquals((byte) 0x77, memory.load(0x10 + 1, Sizes.SIZE_8_LOG2));
        Assertions.assertEquals((byte) 0x66, memory.load(0x10 + 2, Sizes.SIZE_8_LOG2));
        Assertions.assertEquals((byte) 0x55, memory.load(0x10 + 3, Sizes.SIZE_8_LOG2));

        Assertions.assertEquals((byte) 0xCC, memory.load(0x20 + 0, Sizes.SIZE_8_LOG2));
        Assertions.assertEquals((byte) 0xBB, memory.load(0x20 + 1, Sizes.SIZE_8_LOG2));
        Assertions.assertEquals((byte) 0xAA, memory.load(0x20 + 2, Sizes.SIZE_8_LOG2));
        Assertions.assertEquals((byte) 0x99, memory.load(0x20 + 3, Sizes.SIZE_8_LOG2));
    }

    @Test
    public void testStore8() throws Exception {
        memory.store(0x00 + 0, (byte) 0x11, Sizes.SIZE_8_LOG2);
        memory.store(0x00 + 1, (byte) 0x22, Sizes.SIZE_8_LOG2);
        memory.store(0x00 + 2, (byte) 0x33, Sizes.SIZE_8_LOG2);
        memory.store(0x00 + 3, (byte) 0x44, Sizes.SIZE_8_LOG2);

        Assertions.assertEquals(0x44332211, memory.load(0x00 + 0, Sizes.SIZE_32_LOG2));
    }

    @Test
    public void testLoad16() throws Exception {
        Assertions.assertEquals((short) 0x3344, memory.load(0x00 + 0, Sizes.SIZE_16_LOG2));
        Assertions.assertEquals((short) 0x1122, memory.load(0x00 + 2, Sizes.SIZE_16_LOG2));

        Assertions.assertEquals((short) 0x7788, memory.load(0x10 + 0, Sizes.SIZE_16_LOG2));
        Assertions.assertEquals((short) 0x5566, memory.load(0x10 + 2, Sizes.SIZE_16_LOG2));

        Assertions.assertEquals((short) 0x99AA, memory.load(0x20 + 2, Sizes.SIZE_16_LOG2));
        Assertions.assertEquals((short) 0xBBCC, memory.load(0x20 + 0, Sizes.SIZE_16_LOG2));
    }

    @Test
    public void testStore16() throws Exception {
        memory.store(0x00 + 0, (short) 0x2211, Sizes.SIZE_16_LOG2);
        memory.store(0x00 + 2, (short) 0x4433, Sizes.SIZE_16_LOG2);

        Assertions.assertEquals(0x44332211, memory.load(0x00 + 0, Sizes.SIZE_32_LOG2));
    }

    @Test
    public void testLoad32() throws Exception {
        Assertions.assertEquals(0x11223344, memory.load(0x00, Sizes.SIZE_32_LOG2));
        Assertions.assertEquals(0x55667788, memory.load(0x10, Sizes.SIZE_32_LOG2));
        Assertions.assertEquals(0x99AABBCC, memory.load(0x20, Sizes.SIZE_32_LOG2));
    }

    @Test
    public void testStore32() throws Exception {
        memory.store(0, 0x44332211, Sizes.SIZE_32_LOG2);

        Assertions.assertEquals(0x44332211, memory.load(0x00, Sizes.SIZE_32_LOG2));
    }
}
