package li.cil.oc2.common.vm;

import li.cil.oc2.api.bus.device.vm.InterruptAllocator;
import li.cil.oc2.api.bus.device.vm.VMContext;
import li.cil.sedna.api.device.InterruptController;
import li.cil.sedna.api.memory.MemoryMap;
import li.cil.sedna.riscv.device.R5PlatformLevelInterruptController;

import java.util.BitSet;

public final class ManagedVMContext implements VMContext {
    private final ManagedMemoryMap memoryMap;
    private final ManagedInterruptAllocator interruptAllocator;
    private final ManagedInterruptController interruptController;

    ///////////////////////////////////////////////////////////////////

    public ManagedVMContext(final MemoryMap memoryMap, final InterruptController interruptController, final BitSet allocatedInterrupts, final BitSet reservedInterrupts) {
        this.memoryMap = new ManagedMemoryMap(memoryMap);
        this.interruptAllocator = new ManagedInterruptAllocator(allocatedInterrupts, reservedInterrupts, R5PlatformLevelInterruptController.INTERRUPT_COUNT);
        this.interruptController = new ManagedInterruptController(interruptController, interruptAllocator);
    }

    public void freeze() {
        memoryMap.freeze();
        interruptAllocator.freeze();
    }

    public void invalidate() {
        memoryMap.invalidate();
        interruptAllocator.invalidate();
        interruptController.invalidate();
    }

    @Override
    public MemoryMap getMemoryMap() {
        return memoryMap;
    }

    @Override
    public InterruptAllocator getInterruptAllocator() {
        return interruptAllocator;
    }

    @Override
    public InterruptController getInterruptController() {
        return interruptController;
    }
}
