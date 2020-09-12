package li.cil.circuity.api.vm.device;

import li.cil.circuity.api.vm.Interrupt;

public interface InterruptSource extends Device {
    Iterable<Interrupt> getInterrupts();
}
