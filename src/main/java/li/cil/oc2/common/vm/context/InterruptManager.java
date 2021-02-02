package li.cil.oc2.common.vm.context;

import java.util.BitSet;

public interface InterruptManager {
    int getInterruptCount();

    void releaseInterrupts(BitSet interrupts);
}
