/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.bus.device.vm.context;

import li.cil.oc2.api.bus.device.vm.VMDevice;

/**
 * Provides access to the execution state of a running virtual machine.
 *
 * @see VMContext#getRuntime()
 */
public interface VMRuntime {
    /**
     * Waits for the virtual machine's worker thread to finish its current execution.
     * <p>
     * The virtual machine executes on a worker thread. Any resource a {@link VMDevice} has handed to
     * the virtual machine, such as a mapped memory range or a block device, may be accessed from that
     * worker thread at any time while the machine is running. Devices that need to modify or release
     * such a resource outside the regular mount/unmount lifecycle, e.g. to swap removable media, must
     * await the next pause of the worker thread first using this method.
     * <p>
     * After this method returns, the virtual machine will not execute until it is next ticked,
     * which also occurs on the server thread. It is therefore safe to modify resources the
     * machine may access, such as replacing or closing a block device, as long as this happens
     * before returning. Note that when the calling code itself runs from within the machine's
     * tick, e.g. from a synchronized RPC callback, the machine may kick off another worker thread
     * as soon as the calling method returns.
     * <p>
     * This method must be called from the server thread.
     *
     * @throws IllegalStateException when called from a virtual machine worker thread; joining
     *                               from there could deadlock.
     */
    void join();
}
