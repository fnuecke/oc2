package li.cil.circuity.api.vm.device;

/**
 * An interrupter is an interrupt controller that will raise some interrupt based on interrupts
 * it received.
 * <p>
 * Implementations track a list of registered interrupts to allow devices being connected to the
 * controller without these devices having to know of each other. Additionally, this list
 * of registered interrupt ids shall be persisted to allow devices to reclaim their interrupt
 * ids after a load. Implementations may decide to only retain ids for reclaiming for one
 * load iteration, i.e. any ids that have not been reclaimed after a load when the next save
 * occurs may be freed for regular registration again.
 */
public interface Interrupter extends InterruptController {
    /**
     * Register a new interrupt.
     * <p>
     * Use this to reserve a new interrupt with an arbitrary id.
     *
     * @return the id of the registered interrupt; -1 if no more interrupts can be registered.
     */
    int registerInterrupt();

    /**
     * Register an interrupt with a specific id.
     * <p>
     * Use this to reclaim an interrupt ID after state has been restored for example, i.e. a savegame has been loaded.
     *
     * @param id the id to reclaim.
     * @return <code>true</code> if this id had not yet been claimed; <code>false</code> otherwise.
     */
    boolean registerInterrupt(final int id);

    /**
     * Release an interrupt id.
     * <p>
     * This can be used to return the lease on an interrupt id to the controller so it can be handed out in future
     * {@link #registerInterrupt()} calls. Typically this should be called by devices that have been disconnected
     * from the controller for any reason.
     *
     * @param id the id to return to the pool of available ids.
     */
    void releaseInterrupt(final int id);
}
