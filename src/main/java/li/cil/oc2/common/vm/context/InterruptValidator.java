package li.cil.oc2.common.vm.context;

public interface InterruptValidator {
    boolean isMaskValid(int mask);

    int getMaskedInterrupts(int interrupts);
}
