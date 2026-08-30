/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.vm.context.managed;

import li.cil.oc2.api.bus.device.vm.context.*;
import li.cil.oc2.api.util.Invalidatable;
import li.cil.oc2.common.vm.context.VMContextManagerCollection;
import li.cil.sedna.api.device.InterruptController;
import li.cil.sedna.api.memory.MemoryMap;

import java.util.OptionalLong;
import java.util.function.Function;
import java.util.function.Supplier;

public final class ManagedVMContext implements VMContext {
    private final ManagedMemoryMap memoryMap;
    private final ManagedInterruptController interruptController;
    private final ManagedMemoryRangeAllocator memoryRangeAllocator;
    private final ManagedMemoryRangeAllocator deviceRangeAllocator;
    private final ManagedInterruptAllocator interruptAllocator;
    private final ManagedMemoryAllocator memoryAllocator;
    private final Invalidatable<VMRuntime> runtime;
    private final ManagedEventBus eventBus;

    // --------------------------------------------------------------------- //

    public ManagedVMContext(final VMContext parent, final VMContextManagerCollection managers, final Supplier<OptionalLong> baseAddressSupplier) {
        this.memoryRangeAllocator = new ManagedMemoryRangeAllocator(parent.getMemoryRangeAllocator(), managers.getMemoryRangeManager(), baseAddressSupplier);
        this.deviceRangeAllocator = new ManagedMemoryRangeAllocator(parent.getDeviceRangeAllocator(), managers.getDeviceRangeManager(), baseAddressSupplier);
        this.interruptAllocator = new ManagedInterruptAllocator(parent.getInterruptAllocator(), managers.getInterruptManager());
        this.memoryMap = new ManagedMemoryMap(parent.getMemoryMap());
        this.interruptController = new ManagedInterruptController(parent.getInterruptController(), interruptAllocator);
        this.memoryAllocator = new ManagedMemoryAllocator();
        this.runtime = parent.getRuntime().mapWithDependency(Function.identity());
        this.eventBus = new ManagedEventBus(parent.getEventBus(), managers.getEventManager());
    }

    // --------------------------------------------------------------------- //

    public void freeze() {
        memoryRangeAllocator.freeze();
        deviceRangeAllocator.freeze();
        interruptAllocator.freeze();
        memoryAllocator.freeze();
        eventBus.freeze();
    }

    public void invalidate() {
        memoryMap.invalidate();
        memoryRangeAllocator.invalidate();
        deviceRangeAllocator.invalidate();
        interruptController.invalidate();
        interruptAllocator.invalidate();
        memoryAllocator.invalidate();
        runtime.invalidate();
        eventBus.invalidate();
    }

    @Override
    public MemoryMap getMemoryMap() {
        return memoryMap;
    }

    @Override
    public InterruptController getInterruptController() {
        return interruptController;
    }

    @Override
    public MemoryRangeAllocator getMemoryRangeAllocator() {
        return memoryRangeAllocator;
    }

    @Override
    public MemoryRangeAllocator getDeviceRangeAllocator() {
        return deviceRangeAllocator;
    }

    @Override
    public InterruptAllocator getInterruptAllocator() {
        return interruptAllocator;
    }

    @Override
    public MemoryAllocator getMemoryAllocator() {
        return memoryAllocator;
    }

    @Override
    public Invalidatable<VMRuntime> getRuntime() {
        return runtime;
    }

    @Override
    public VMLifecycleEventBus getEventBus() {
        return eventBus;
    }
}
