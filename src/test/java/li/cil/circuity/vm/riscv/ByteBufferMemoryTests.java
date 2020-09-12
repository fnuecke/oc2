package li.cil.circuity.vm.riscv;

import li.cil.circuity.vm.device.memory.ByteBufferMemory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ByteBufferMemoryTests {
    private ByteBufferMemory memory;

    @BeforeEach
    public void initialize() throws Exception {
        memory = new ByteBufferMemory(4 * 1024);
        memory.store32(0x00, 0x11223344);
        memory.store32(0x10, 0x55667788);
        memory.store32(0x20, 0x99AABBCC);
    }

    @Test
    public void testLoad8() throws Exception {
        Assertions.assertEquals((byte) 0x44, memory.load8(0x00 + 0));
        Assertions.assertEquals((byte) 0x33, memory.load8(0x00 + 1));
        Assertions.assertEquals((byte) 0x22, memory.load8(0x00 + 2));
        Assertions.assertEquals((byte) 0x11, memory.load8(0x00 + 3));

        Assertions.assertEquals((byte) 0x88, memory.load8(0x10 + 0));
        Assertions.assertEquals((byte) 0x77, memory.load8(0x10 + 1));
        Assertions.assertEquals((byte) 0x66, memory.load8(0x10 + 2));
        Assertions.assertEquals((byte) 0x55, memory.load8(0x10 + 3));

        Assertions.assertEquals((byte) 0xCC, memory.load8(0x20 + 0));
        Assertions.assertEquals((byte) 0xBB, memory.load8(0x20 + 1));
        Assertions.assertEquals((byte) 0xAA, memory.load8(0x20 + 2));
        Assertions.assertEquals((byte) 0x99, memory.load8(0x20 + 3));
    }

    @Test
    public void testStore8() throws Exception {
        memory.store8(0x00 + 0, (byte) 0x11);
        memory.store8(0x00 + 1, (byte) 0x22);
        memory.store8(0x00 + 2, (byte) 0x33);
        memory.store8(0x00 + 3, (byte) 0x44);

        Assertions.assertEquals(0x44332211, memory.load32(0x00 + 0));
    }

    @Test
    public void testLoad16() throws Exception {
        Assertions.assertEquals((short) 0x3344, memory.load16(0x00 + 0));
        Assertions.assertEquals((short) 0x1122, memory.load16(0x00 + 2));

        Assertions.assertEquals((short) 0x7788, memory.load16(0x10 + 0));
        Assertions.assertEquals((short) 0x5566, memory.load16(0x10 + 2));

        Assertions.assertEquals((short) 0x99AA, memory.load16(0x20 + 2));
        Assertions.assertEquals((short) 0xBBCC, memory.load16(0x20 + 0));
    }

    @Test
    public void testStore16() throws Exception {
        memory.store16(0x00 + 0, (short) 0x2211);
        memory.store16(0x00 + 2, (short) 0x4433);

        Assertions.assertEquals(0x44332211, memory.load32(0x00 + 0));
    }

    @Test
    public void testLoad32() throws Exception {
        Assertions.assertEquals(0x11223344, memory.load32(0x00));
        Assertions.assertEquals(0x55667788, memory.load32(0x10));
        Assertions.assertEquals(0x99AABBCC, memory.load32(0x20));
    }

    @Test
    public void testStore32() throws Exception {
        memory.store32(0, 0x44332211);

        Assertions.assertEquals(0x44332211, memory.load32(0x00));
    }
}
