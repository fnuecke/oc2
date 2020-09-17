package li.cil.circuity.vm.riscv.dbt;

import li.cil.circuity.api.vm.device.memory.MemoryAccessException;
import li.cil.circuity.vm.riscv.exception.R5Exception;

public abstract class Trace {
    public abstract void execute() throws R5Exception, MemoryAccessException;
}