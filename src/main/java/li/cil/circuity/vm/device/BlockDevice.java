package li.cil.circuity.vm.device;

public interface BlockDevice {
    boolean isReadonly();

    long getCapacity();
}
