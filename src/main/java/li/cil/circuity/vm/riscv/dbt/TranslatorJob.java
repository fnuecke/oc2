package li.cil.circuity.vm.riscv.dbt;

import li.cil.circuity.api.vm.device.memory.MemoryMappedDevice;
import li.cil.circuity.vm.riscv.R5CPU;

public final class TranslatorJob {
    public final R5CPU cpu;
    public final int pc;
    public final MemoryMappedDevice device;
    public final int instOffset;
    public final int instEnd;
    public final int toPC;
    public final int firstInst;

    public Trace trace;

    public TranslatorJob(final R5CPU cpu, final int pc, final MemoryMappedDevice device, final int instOffset, final int instEnd, final int toPC, final int firstInst) {
        this.cpu = cpu;
        this.pc = pc;
        this.device = device;
        this.instOffset = instOffset;
        this.instEnd = instEnd;
        this.toPC = toPC;
        this.firstInst = firstInst;
    }
}
