package li.cil.circuity.vm.device.memory.exception;

import li.cil.circuity.api.vm.device.memory.MemoryAccessException;

public final class MisalignedStoreException extends MemoryAccessException {
    public MisalignedStoreException(final int address) {
        super(address);
    }
}
