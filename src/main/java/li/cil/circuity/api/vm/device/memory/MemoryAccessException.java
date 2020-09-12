package li.cil.circuity.api.vm.device.memory;

public class MemoryAccessException extends Exception {
    private final int address;

    public MemoryAccessException(final int address) {
        this.address = address;
    }

    public int getAddress() {
        return address;
    }
}
