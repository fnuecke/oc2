package li.cil.circuity.vm.device.memory.exception;

import li.cil.circuity.api.vm.device.memory.MemoryAccessException;

public final class LoadPageFaultException extends MemoryAccessException {
    private final int address;

    public LoadPageFaultException(final int address) {
        super(address);
        this.address = address;
    }

    public int getAddress() {
        return address;
    }
}
