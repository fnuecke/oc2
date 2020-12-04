package li.cil.oc2.api.vm;

import li.cil.sedna.api.device.InterruptController;
import li.cil.sedna.api.memory.MemoryMap;

public interface VirtualMachineContext {
    MemoryMap getMemoryMap();

    InterruptAllocator getInterruptAllocator();

    InterruptController getInterruptController();
}
