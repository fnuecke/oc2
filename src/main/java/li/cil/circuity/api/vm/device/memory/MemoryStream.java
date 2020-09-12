package li.cil.circuity.api.vm.device.memory;

public interface MemoryStream {
    boolean canLoad16();

    short load16() throws MemoryAccessException;

    boolean canLoad32();

    int load32() throws MemoryAccessException;
}
