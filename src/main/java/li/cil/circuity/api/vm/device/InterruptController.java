package li.cil.circuity.api.vm.device;

/**
 * Interrupt controllers expose an API to set and unset some interrupts from the outside.
 */
public interface InterruptController extends Device {
    /**
     * Mark the interrupts represented by the set bits in the specified mask as active.
     * <p>
     * In the mask each registered interrupt is represented by a single bit, at the location of its id. So if interrupts
     * 1 and 4 are to be made active, the mask would be <code>0b00000101</code>.
     *
     * @param mask the mask of interrupts to set active.
     */
    void raiseInterrupts(final int mask);

    /**
     * Mark the interrupts represented by the set bits in the specified mask as inactive.
     * <p>
     * In the mask each registered interrupt is represented by a single bit, at the location of its id. So if interrupts
     * 1 and 4 are to be made inactive, the mask would be <code>0b00000101</code>.
     *
     * @param mask the mask of interrupts to set inactive.
     */
    void lowerInterrupts(final int mask);

    /**
     * Mask representing the currently raised interrupts via active bits.
     * <p>
     * Each set bit represents a raised interrupt, e.g. in the mask 0b0101 the interrupts 0 and 2 are raised.
     *
     * @return the mask containing the currently raised interrupts.
     */
    int getRaisedInterrupts();
}
