package li.cil.circuity.vm.device.memory.exception;

import li.cil.circuity.api.vm.device.memory.MemoryAccessException;

public final class LoadFaultException extends MemoryAccessException {
    public LoadFaultException(final int address) {
        super(address);
    }
}
