package li.cil.oc2.common.vm.context.global;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.SubscriberExceptionContext;
import li.cil.oc2.api.bus.device.vm.VMLifecycleEventBus;
import li.cil.oc2.api.bus.device.vm.event.VMInitializationException;
import li.cil.oc2.common.vm.context.EventManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@SuppressWarnings("UnstableApiUsage")
final class GlobalEventBus implements VMLifecycleEventBus, EventManager {
    private static final Logger LOGGER = LogManager.getLogger();

    ///////////////////////////////////////////////////////////////////

    private final EventBus eventBus = new EventBus(this::handleEventBusException);
    private VMInitializationException initializationException;

    ///////////////////////////////////////////////////////////////////

    public void post(final Object event) {
        initializationException = null;

        eventBus.post(event);

        final VMInitializationException exception = initializationException;
        initializationException = null;
        if (exception != null) {
            throw exception;
        }
    }

    @Override
    public void register(final Object subscriber) {
        eventBus.register(subscriber);
    }

    @Override
    public void unregister(final Object subscriber) {
        eventBus.unregister(subscriber);
    }

    ///////////////////////////////////////////////////////////////////

    private void handleEventBusException(final Throwable throwable, final SubscriberExceptionContext context) {
        if (throwable instanceof VMInitializationException) {
            initializationException = (VMInitializationException) throwable;
        } else {
            LOGGER.error(throwable);
        }
    }
}
