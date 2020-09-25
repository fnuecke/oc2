package li.cil.circuity.api.vm.device;

/**
 * Steppable devices can be advanced by some number of <em>cycles</em>.
 * <p>
 * Cycles in this context are an abstract concept and interpretation is up to the device
 * implementing this interface. This can be actual emulated clock cycles, number of
 * instructions processed or simply a generic value representing time.
 * <p>
 * Devices implementing this must not make any assumptions on the number of cycles
 * they are allowed at a single time. This number may change wildly between calls.
 * However, while they should not, devices may run for more cycles than specified.
 */
public interface Steppable extends Device {
    /**
     * Advance this device by the given number of cycles.
     *
     * @param cycles the number of cycles to advance the device by.
     */
    void step(final int cycles);
}
