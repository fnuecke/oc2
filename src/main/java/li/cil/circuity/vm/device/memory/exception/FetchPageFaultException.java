package li.cil.circuity.vm.device.memory.exception;

import li.cil.circuity.api.vm.device.memory.MemoryAccessException;

public final class FetchPageFaultException extends MemoryAccessException {
    public FetchPageFaultException(final int address) {
        super(address);
    }
}
