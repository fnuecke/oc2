/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.bus.device.vm.event;

/**
 * Fired when the VM is paused, typically before state is persisted.
 * <p>
 * Allows devices that offer interaction to external code-flow to suspend
 * such interactions until {@link VMResumedRunningEvent} is fired. This is required
 * if such interactions may modify VM state, to prevent corrupting data being
 * serialized asynchronously.
 * <p>
 * Note that this is called right before the worker thread used to execute the
 * virtual machine is joined to the main thread. As such, only devices that run
 * their own threads modifying observable state will need to synchronize these
 * threads here.
 */
public final class VMSynchronizeEvent { }
