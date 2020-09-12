package li.cil.circuity.vm.riscv;

import li.cil.circuity.vm.device.memory.ByteBufferMemory;
import org.junit.jupiter.api.Test;

public class VirtualMachineTests {
    @Test
    public void testVM() {
        final R5Board virtualMachine = new R5Board();
        virtualMachine.addDevice(new ByteBufferMemory(4 * 1024 * 1024));
        virtualMachine.reset();
    }
}
