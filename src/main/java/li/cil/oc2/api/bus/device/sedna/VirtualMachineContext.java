package li.cil.oc2.api.bus.device.sedna;

import li.cil.oc2.api.bus.DeviceBus;
import li.cil.sedna.api.device.InterruptController;
import li.cil.sedna.api.device.MemoryMappedDevice;
import li.cil.sedna.api.memory.MemoryMap;

/**
 * Provides access to a virtual machine to low level devices.
 *
 * @see SednaDevice
 */
public interface VirtualMachineContext {
    /**
     * The memory of the virtual machine.
     * <p>
     * {@link MemoryMappedDevice}s can only be added inside {@link SednaDevice#load(VirtualMachineContext)}.
     * Trying to add devices after that method has returned will result in an exception.
     * <p>
     * Removing {@link MemoryMappedDevice}s is not supported. Added devices will
     * automatically removed when the {@link SednaDevice} that added it is unloaded,
     * e.g. because it has been removed from the {@link DeviceBus}.
     *
     * @return the memory map of the virtual machine.
     */
    MemoryMap getMemoryMap();

    /**
     * An object that allows claiming interrupts for use with the {@link InterruptController}.
     * <p>
     * Interrupts can only be claimed inside {@link SednaDevice#load(VirtualMachineContext)}.
     * Trying to claim interrupts after that method has returned will result in an exception.
     * <p>
     * Claimed interrupts will automatically be released when the {@link SednaDevice} that
     * claimed them is unloaded, e.g. because it is removed from the {@link DeviceBus}.
     *
     * @return the interrupt allocator.
     */
    InterruptAllocator getInterruptAllocator();

    /**
     * The interrupt controller of the virtual machine devices should attach to.
     * <p>
     * Raising or lowering interrupts that have not been claimed using the {@link InterruptAllocator}
     * made available through this instance will result in an exception.
     * <p>
     * Interrupts raised will automatically be lowered when the {@link SednaDevice} that
     * raised them is unloaded, e.g. because it is removed from the {@link DeviceBus}.
     *
     * @return the interrupt controller of the virtual machine.
     */
    InterruptController getInterruptController();
}
