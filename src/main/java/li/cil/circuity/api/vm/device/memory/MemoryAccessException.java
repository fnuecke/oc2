package li.cil.circuity.api.vm.device.memory;

/**
 * Base class for all memory related exceptions.
 * <p>
 * This exception may be thrown whenever memory mapped in a {@link li.cil.circuity.api.vm.MemoryMap}
 * is accessed, specifically any {@link MemoryMappedDevice} may throw these exceptions to signal an
 * invalid access.
 */
public class MemoryAccessException extends Exception {
    private final int address;

    public MemoryAccessException(final int address) {
        this.address = address;
    }

    public int getAddress() {
        return address;
    }
}
