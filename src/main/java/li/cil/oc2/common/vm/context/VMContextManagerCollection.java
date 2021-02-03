package li.cil.oc2.common.vm.context;

public interface VMContextManagerCollection {
    InterruptManager getInterruptManager();

    MemoryRangeManager getMemoryRangeManager();

    EventManager getEventManager();
}
