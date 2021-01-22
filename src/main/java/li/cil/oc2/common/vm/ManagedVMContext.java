package li.cil.oc2.common.vm;

import com.google.common.eventbus.EventBus;
import li.cil.oc2.api.bus.device.vm.InterruptAllocator;
import li.cil.oc2.api.bus.device.vm.MemoryAllocator;
import li.cil.oc2.api.bus.device.vm.MemoryRangeAllocator;
import li.cil.oc2.api.bus.device.vm.VMContext;
import li.cil.oc2.api.bus.device.vm.event.VMLifecycleEventBus;
import li.cil.sedna.api.Board;
import li.cil.sedna.api.device.InterruptController;
import li.cil.sedna.api.device.MemoryMappedDevice;
import li.cil.sedna.api.memory.MemoryMap;

import java.util.BitSet;
import java.util.OptionalLong;
import java.util.function.Function;

@SuppressWarnings("UnstableApiUsage")
public final class ManagedVMContext implements VMContext {
    private final ManagedMemoryMap memoryMap;
    private final ManagedInterruptController interruptController;
    private final ManagedMemoryRangeAllocator memoryRangeAllocator;
    private final ManagedInterruptAllocator interruptAllocator;
    private final ManagedMemoryAllocator memoryAllocator;
    private final ManagedEventBus eventBus;

    ///////////////////////////////////////////////////////////////////

    public ManagedVMContext(final Board board, final BitSet claimedInterrupts, final BitSet reservedInterrupts,
                            final EventBus eventBus, final Function<MemoryMappedDevice, OptionalLong> defaultAddress) {
        this.memoryRangeAllocator = new ManagedMemoryRangeAllocator(board, defaultAddress);
        this.interruptAllocator = new ManagedInterruptAllocator(claimedInterrupts, reservedInterrupts, board.getInterruptCount());
        this.memoryMap = new ManagedMemoryMap(board.getMemoryMap());
        this.interruptController = new ManagedInterruptController(board.getInterruptController(), interruptAllocator);
        this.memoryAllocator = new ManagedMemoryAllocator();
        this.eventBus = new ManagedEventBus(eventBus);
    }

    public ManagedVMContext(final Board board, final BitSet claimedInterrupts, final BitSet reservedInterrupts, final EventBus eventBus) {
        this(board, claimedInterrupts, reservedInterrupts, eventBus, (memoryMappedDevice) -> OptionalLong.empty());
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
}
