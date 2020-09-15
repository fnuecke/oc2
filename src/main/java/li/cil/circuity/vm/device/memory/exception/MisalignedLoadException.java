package li.cil.circuity.vm.device.memory.exception;

import li.cil.circuity.api.vm.device.memory.MemoryAccessException;

public final class MisalignedLoadException extends MemoryAccessException {
    public MisalignedLoadException(final int address) {
        super(address);
    }
}
