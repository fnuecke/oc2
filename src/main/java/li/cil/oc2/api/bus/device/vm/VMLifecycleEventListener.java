package li.cil.oc2.api.bus.device.vm;

public interface VMLifecycleEventListener extends VMDevice {
    void handleLifecycleEvent(VMLifecycleEventType event);
}
