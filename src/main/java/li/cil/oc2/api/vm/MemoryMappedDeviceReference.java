package li.cil.oc2.api.vm;

import li.cil.sedna.api.device.InterruptController;
import li.cil.sedna.api.memory.MemoryMap;

public interface MemoryMappedDeviceReference {
    boolean load(final MemoryMap memoryMap, final InterruptController interruptController);

    void unload();
}
