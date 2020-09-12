package li.cil.circuity.api.vm.device;

public interface Steppable extends Device {
    void step(final int cycles);
}
