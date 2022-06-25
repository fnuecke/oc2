/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.bus.device.vm.context;

import li.cil.oc2.api.bus.DeviceBus;
import li.cil.oc2.api.bus.device.vm.VMDevice;
import li.cil.sedna.api.device.InterruptController;
import li.cil.sedna.api.device.MemoryMappedDevice;
import li.cil.sedna.api.memory.MemoryMap;

/**
 * Provides access to a virtual machine to low level devices.
 *
 * @see VMDevice
 */
public interface VMContext {
    /**
     * The memory of the virtual machine.
     * <p>
     * Adding or removing {@link MemoryMappedDevice}s directly is not supported.
     * Use the {@link MemoryRangeAllocator} provided by {@link #getMemoryRangeAllocator()}
     * to add devices.
     *
     * @return the memory map of the virtual machine.
     */
    MemoryMap getMemoryMap();

    /**
     * The interrupt controller of the virtual machine.
     * <p>
     * Raising or lowering interrupts that have not been claimed using the {@link InterruptAllocator}
     * made available through this instance will result in an exception.
     * <p>
     * Interrupts raised will automatically be lowered when the {@link VMDevice} that
     * raised them is unloaded, e.g. because it is removed from the {@link DeviceBus}
     * or the VM stopped.
     *
     * @return the interrupt controller of the virtual machine.
     */
    InterruptController getInterruptController();

    /**
     * Allows adding {@link MemoryMappedDevice}s to the VM's {@link MemoryMap}.
     * <p>
     * {@link MemoryMappedDevice}s can only be added inside {@link VMDevice#mount(VMContext)}.
     * Trying to add devices after that method has returned will result in an exception.
     * <p>
     * Added devices will be automatically removed when the {@link VMDevice} that added it
     * is unloaded, e.g. because it has been removed from the {@link DeviceBus} or the VM
     * stopped.
     *
     * @return the memory range allocator.
     */
    MemoryRangeAllocator getMemoryRangeAllocator();

    /**
     * Allows claiming interrupts for use with the VM's {@link InterruptController}.
     * <p>
     * Interrupts can only be claimed inside {@link VMDevice#mount(VMContext)}.
     * Trying to claim interrupts after that method has returned will result in an exception.
     * <p>
     * Claimed interrupts will automatically be released when the {@link VMDevice} that
     * claimed them is unloaded, e.g. because it is removed from the {@link DeviceBus}.
     *
     * @return the interrupt allocator.
     */
    InterruptAllocator getInterruptAllocator();

    /**
     * Allows reserving fixed amounts of memory respecting sandbox constraints.
     * <p>
     * It is strongly advised to use this allocator to make known large memory
     * uses, e.g. when allocating large blobs for memory or block devices. This
     * allows respecting the built-in limits for overall memory usage of
     * running VMs.
     * <p>
     * Devices failing to reserve the memory they would use should fail their
     * {@link VMDevice#mount(VMContext)}.
     * <p>
     * Memory will automatically be released when the {@link VMDevice} that claimed
     * it is unloaded, e.g. because it is removed from the {@link DeviceBus} or the
     * VM stopped.
     *
     * @return the memory allocator.
     */
    MemoryAllocator getMemoryAllocator();

    /**
     * Allows registering to VM lifecycle events.
     * <p>
     * Registered subscribers will automatically be unsubscribed when the {@link VMDevice}
     * that registered them is unloaded, e.g. because it is removed from the {@link DeviceBus}
     * of the VM stopped.
     *
     * @return the event bus.
     */
    VMLifecycleEventBus getEventBus();
}
