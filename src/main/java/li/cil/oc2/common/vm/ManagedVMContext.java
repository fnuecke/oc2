package li.cil.oc2.common.vm;

import li.cil.oc2.api.bus.device.vm.InterruptAllocator;
import li.cil.oc2.api.bus.device.vm.MemoryAllocator;
import li.cil.oc2.api.bus.device.vm.MemoryRangeAllocator;
import li.cil.oc2.api.bus.device.vm.VMContext;
import li.cil.sedna.api.Board;
import li.cil.sedna.api.device.InterruptController;
import li.cil.sedna.api.memory.MemoryMap;

import java.util.BitSet;

public final class ManagedVMContext implements VMContext {
    private final ManagedMemoryMap memoryMap;
    private final ManagedInterruptController interruptController;
    private final ManagedMemoryRangeAllocator memoryRangeAllocator;
    private final ManagedInterruptAllocator interruptAllocator;
    private final ManagedMemoryAllocator memoryAllocator;

    ///////////////////////////////////////////////////////////////////

    public ManagedVMContext(final Board board, final BitSet claimedInterrupts, final BitSet reservedInterrupts) {
        this.memoryRangeAllocator = new ManagedMemoryRangeAllocator(board);
        this.interruptAllocator = new ManagedInterruptAllocator(claimedInterrupts, reservedInterrupts, board.getInterruptCount());
        this.memoryMap = new ManagedMemoryMap(board.getMemoryMap());
        this.interruptController = new ManagedInterruptController(board.getInterruptController(), interruptAllocator);
        this.memoryAllocator = new ManagedMemoryAllocator();
    }

    ///////////////////////////////////////////////////////////////////

    public void freeze() {
        memoryRangeAllocator.freeze();
        interruptAllocator.freeze();
        memoryAllocator.freeze();
    }

    public void invalidate() {
        memoryRangeAllocator.invalidate();
        interruptAllocator.invalidate();
        interruptController.invalidate();
        memoryAllocator.invalidate();
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
    public InterruptAllocator getInterruptAllocator() {
        return interruptAllocator;
    }

    @Override
    public MemoryAllocator getMemoryAllocator() {
        return memoryAllocator;
    }
}
