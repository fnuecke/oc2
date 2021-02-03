package li.cil.oc2.common.vm.context.managed;

import li.cil.oc2.api.bus.device.vm.VMLifecycleEventBus;
import li.cil.oc2.common.vm.context.EventManager;

import java.util.ArrayList;

final class ManagedEventBus implements VMLifecycleEventBus {
    private final VMLifecycleEventBus parent;
    private final EventManager manager;
    private final ArrayList<Object> subscribers = new ArrayList<>();
    private boolean isFrozen;

    ///////////////////////////////////////////////////////////////////

    public ManagedEventBus(final VMLifecycleEventBus parent, final EventManager manager) {
        this.parent = parent;
        this.manager = manager;
    }

    ///////////////////////////////////////////////////////////////////

    public void freeze() {
        isFrozen = true;
    }

    public void invalidate() {
        for (final Object subscriber : subscribers) {
            manager.unregister(subscriber);
        }
        subscribers.clear();
    }

    @Override
    public void register(final Object subscriber) {
        if (isFrozen) {
            throw new IllegalStateException();
        }

        parent.register(subscriber);
        subscribers.add(subscriber);
    }
}
