package li.cil.oc2.common.vm;

import com.google.common.eventbus.EventBus;
import li.cil.oc2.api.bus.device.vm.event.VMLifecycleEventBus;

import java.util.ArrayList;

@SuppressWarnings("UnstableApiUsage")
public final class ManagedEventBus implements VMLifecycleEventBus {
    private final EventBus eventBus;
    private final ArrayList<Object> subscribers = new ArrayList<>();

    public ManagedEventBus(final EventBus eventBus) {
        this.eventBus = eventBus;
    }

    public void invalidate() {
        for (final Object subscriber : subscribers) {
            eventBus.unregister(subscriber);
        }
        subscribers.clear();
    }

    @Override
    public void register(final Object object) {
        eventBus.register(object);
        subscribers.add(object);
    }

    @Override
    public void unregister(final Object object) {
        eventBus.unregister(object);
        subscribers.remove(object);
    }
}
