package li.cil.circuity.vm.riscv.dbt;

import li.cil.circuity.api.vm.device.memory.MemoryAccessException;
import li.cil.circuity.vm.riscv.R5CPU;
import li.cil.circuity.vm.riscv.exception.R5Exception;

@FunctionalInterface
public interface Trace {
    void execute(final R5CPU cpu) throws R5Exception, MemoryAccessException;
}
