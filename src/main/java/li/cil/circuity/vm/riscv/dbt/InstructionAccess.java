package li.cil.circuity.vm.riscv.dbt;

import li.cil.circuity.api.vm.device.memory.MemoryAccessException;
import li.cil.circuity.vm.riscv.R5CPU;

@FunctionalInterface
public interface InstructionAccess {
    R5CPU.TLBEntry fetchPage(final int pc) throws MemoryAccessException;
}
