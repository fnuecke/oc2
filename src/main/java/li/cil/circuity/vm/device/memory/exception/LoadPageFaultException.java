package li.cil.circuity.vm.device.memory.exception;

import li.cil.circuity.api.vm.device.memory.MemoryAccessException;

public final class LoadPageFaultException extends MemoryAccessException {
    public LoadPageFaultException(final int address) {
        super(address);
    }
}
