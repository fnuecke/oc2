package li.cil.oc2.api.vm;

import java.util.OptionalInt;

public interface InterruptAllocator {
    OptionalInt claimInterrupt(int interrupt);

    OptionalInt claimInterrupt();
}
