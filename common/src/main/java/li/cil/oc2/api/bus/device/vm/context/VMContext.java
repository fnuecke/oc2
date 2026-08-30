/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.bus.device.vm.context;

import li.cil.oc2.api.bus.DeviceBus;
import li.cil.oc2.api.bus.device.vm.VMDevice;
import li.cil.oc2.api.util.Invalidatable;
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
     * Allows adding devices that are not memory to wherever this machine maps its devices.
     * <p>
     * On a machine that addresses everything through its memory map this is the same thing as
     * {@link #getMemoryRangeAllocator()}. On one with a separate space for devices, such as a Z80's
     * ports, it is that space instead.
     * <p>
     * Prefer this over {@link #getMemoryRangeAllocator()} for anything that is not memory, so the
     * device lands wherever the machine keeps devices.
     * <p>
     * Other than that, same rules as for {@link #getMemoryRangeAllocator()} apply.
     *
     * @return the device range allocator.
     */
    MemoryRangeAllocator getDeviceRangeAllocator();

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
     * Provides access to the execution state of the virtual machine.
     * <p>
     * The returned wrapper is valid from {@link VMDevice#mount(VMContext)} until the device is
     * unloaded, e.g. because it is removed from the {@link DeviceBus} or the VM stopped.
     * <p>
     * Both the wrapper and the {@link VMRuntime} it holds must only be used from the server thread.
     *
     * @return the runtime of the virtual machine.
     */
    Invalidatable<VMRuntime> getRuntime();

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
