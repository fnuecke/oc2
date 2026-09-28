/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.bus.device;

import li.cil.oc2.api.bus.device.io.IOBusContext;
import li.cil.oc2.api.bus.device.rpc.RPCBusContext;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Tracks the contexts a device is mounted with, to send events to every computer it is mounted in.
 * <p>
 * Add contexts on mount and remove them on unmount, e.g. in the callbacks of
 * {@link li.cil.oc2.api.bus.device.object.LifecycleAwareDevice}. Safe to use from any thread.
 */
@SuppressWarnings("overloads")
public final class DeviceContext {
    private final Set<RPCBusContext> rpcContexts = new CopyOnWriteArraySet<>();
    private final Set<IOBusContext> ioContexts = new CopyOnWriteArraySet<>();

    // --------------------------------------------------------------------- //

    public void add(final RPCBusContext context) {
        rpcContexts.add(context);
    }

    public void remove(final RPCBusContext context) {
        rpcContexts.remove(context);
    }

    public void add(final IOBusContext context) {
        ioContexts.add(context);
    }

    public void remove(final IOBusContext context) {
        ioContexts.remove(context);
    }

    /**
     * Sends an event to all computers this device is mounted in via the high-level API.
     *
     * @see RPCBusContext#sendEvent(String, Object)
     */
    public void sendEvent(final String type, @Nullable final Object data) {
        for (final RPCBusContext context : rpcContexts) {
            context.sendEvent(type, data);
        }
    }

    /**
     * Sends an event to all computers this device is mounted in via the mid-level API.
     *
     * @see IOBusContext#sendEvent(int, int)
     */
    public void sendEvent(final int code, final int value) {
        for (final IOBusContext context : ioContexts) {
            context.sendEvent(code, value);
        }
    }
}
