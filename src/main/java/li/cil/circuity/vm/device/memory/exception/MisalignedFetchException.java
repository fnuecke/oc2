package li.cil.circuity.vm.device.memory.exception;

import li.cil.circuity.api.vm.device.memory.MemoryAccessException;

public final class MisalignedFetchException extends MemoryAccessException {
    public MisalignedFetchException(final int address) {
        super(address);
    }
}
