package li.cil.circuity.vm.riscv.exception;

import li.cil.circuity.vm.riscv.R5;

public final class R5IllegalInstructionException extends R5Exception {
    private final int instruction;

    public R5IllegalInstructionException(final int instruction) {
        super(R5.EXCEPTION_ILLEGAL_INSTRUCTION);
        this.instruction = instruction;
    }

    @Override
    public int getExceptionValue() {
        return instruction;
    }
}
