package li.cil.oc2.common.vm.context.global;

import li.cil.ceres.api.Serialized;
import li.cil.oc2.api.bus.device.vm.context.*;
import li.cil.oc2.common.vm.context.EventManager;
import li.cil.oc2.common.vm.context.InterruptManager;
import li.cil.oc2.common.vm.context.MemoryRangeManager;
import li.cil.oc2.common.vm.context.VMContextManagerCollection;
import li.cil.sedna.api.Board;
import li.cil.sedna.api.device.InterruptController;
import li.cil.sedna.api.memory.MemoryMap;

import java.util.BitSet;

public final class GlobalVMContext implements VMContext, VMContextManagerCollection {
    private final GlobalMemoryMap memoryMap;
    private final GlobalMemoryRangeAllocator memoryRangeAllocator;
    private final GlobalInterruptAllocator interruptAllocator;
    private final GlobalInterruptController interruptController;
    private final GlobalMemoryAllocator memoryAllocator;
    private final GlobalEventBus eventBus;
    private final Runnable joinWorkerThread;

    ///////////////////////////////////////////////////////////////////

    // We track currently claimed interrupts and memory ranges so that after loading we
    // avoid potentially new devices (due external code changes, etc.) to grab interrupts
    // or memory ranges previously used by other devices. Only claiming interrupts and
    // memory ranges explicitly will allow grabbing reserved ones.

    @Serialized @SuppressWarnings("FieldMayBeFinal")
    private BitSet reservedInterrupts = new BitSet();

    @Serialized @SuppressWarnings("FieldMayBeFinal")
    private MemoryRangeList reservedMemoryRanges = new MemoryRangeList();

    ///////////////////////////////////////////////////////////////////

    public GlobalVMContext(final Board board, final Runnable joinWorkerThread) {
        this.memoryMap = new GlobalMemoryMap(board.getMemoryMap());
        this.memoryRangeAllocator = new GlobalMemoryRangeAllocator(board, reservedMemoryRanges);
        this.interruptAllocator = new GlobalInterruptAllocator(board.getInterruptCount(), reservedInterrupts);
        this.interruptController = new GlobalInterruptController(board.getInterruptController(), interruptAllocator);
        this.memoryAllocator = new GlobalMemoryAllocator();
        this.eventBus = new GlobalEventBus();
        this.joinWorkerThread = joinWorkerThread;
    }

    ///////////////////////////////////////////////////////////////////

    public void updateReservations() {
        reservedInterrupts.clear();
        reservedInterrupts.or(interruptAllocator.getClaimedInterrupts());

        reservedMemoryRanges.clear();
        reservedMemoryRanges.addAll(memoryRangeAllocator.getClaimedMemoryRanges());
    }

    public void postEvent(final Object event) {
        eventBus.post(event);
    }

    public void invalidate() {
        memoryRangeAllocator.invalidate();
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

    @Override
    public VMLifecycleEventBus getEventBus() {
        return eventBus;
    }

    @Override
    public void joinWorkerThread() {
        joinWorkerThread.run();
    }

    @Override
    public InterruptManager getInterruptManager() {
        return interruptAllocator;
    }

    @Override
    public MemoryRangeManager getMemoryRangeManager() {
        return memoryRangeAllocator;
    }

    @Override
    public EventManager getEventManager() {
        return eventBus;
    }
}
