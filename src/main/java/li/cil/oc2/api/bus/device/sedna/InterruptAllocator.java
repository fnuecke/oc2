package li.cil.oc2.api.bus.device.sedna;

import java.util.OptionalInt;

public interface InterruptAllocator {
    OptionalInt claimInterrupt(int interrupt);

    OptionalInt claimInterrupt();
}
