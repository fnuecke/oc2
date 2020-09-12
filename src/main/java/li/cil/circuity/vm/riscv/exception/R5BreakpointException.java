package li.cil.circuity.vm.riscv.exception;

import li.cil.circuity.vm.riscv.R5;

public final class R5BreakpointException extends R5Exception {
    public R5BreakpointException() {
        super(R5.EXCEPTION_BREAKPOINT);
    }
}
