package li.cil.circuity.vm.device.memory.exception;

import li.cil.circuity.api.vm.device.memory.MemoryAccessException;

public final class StoreFaultException extends MemoryAccessException {
    public StoreFaultException(final int address) {
        super(address);
    }
}
