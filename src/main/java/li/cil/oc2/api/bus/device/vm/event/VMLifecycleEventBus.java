package li.cil.oc2.api.bus.device.vm.event;

public interface VMLifecycleEventBus {
    void register(Object object);

    void unregister(Object object);
}
