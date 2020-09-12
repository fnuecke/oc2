package li.cil.circuity.vm.riscv.exception;

public class R5Exception extends Exception {
    private final int exceptionCause;

    public R5Exception(final int exceptionCause) {
        this.exceptionCause = exceptionCause;
    }

    public int getExceptionCause() {
        return exceptionCause;
    }

    public int getExceptionValue() {
        return 0;
    }
}
