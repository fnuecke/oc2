package li.cil.circuity.vm.device.memory.exception;

import li.cil.circuity.api.vm.device.memory.MemoryAccessException;

public final class StorePageFaultException extends MemoryAccessException {
    public StorePageFaultException(final int address) {
        super(address);
    }
}
