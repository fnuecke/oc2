package li.cil.circuity.vm.device.memory.exception;

import li.cil.circuity.api.vm.device.memory.MemoryAccessException;

public final class FetchFaultException extends MemoryAccessException {
    public FetchFaultException(final int address) {
        super(address);
    }
}
