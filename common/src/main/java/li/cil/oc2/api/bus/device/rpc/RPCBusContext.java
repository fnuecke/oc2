/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.bus.device.rpc;

import javax.annotation.Nullable;

/**
 * A device's handle on the computer it was mounted in.
 * <p>
 * Passed to {@link RPCDevice#mount(RPCBusContext)} and, as the same instance, to
 * {@link RPCDevice#unmount(RPCBusContext)}. Valid in between. A device mounted
 * more than once at a time, e.g. in several computers, receives one context per mount.
 */
public interface RPCBusContext {
    /**
     * Sends an event to the guest of this computer, attributed to this device.
     * <p>
     * May be called from any thread. Events are queued and delivered when the
     * machine next runs. The queue is bounded; an event that does not fit is
     * dropped, and the guest is told so separately.
     * <p>
     * {@code data} is serialized as JSON on the calling thread, using the same
     * type adapters as method results, except that binary payloads ({@code byte[]})
     * are not supported. A payload that fails to serialize is logged and dropped.
     * Only pass live game objects, such as an {@code ItemStack}, from the server thread.
     *
     * @param type the event type, e.g. {@code redstoneChanged}.
     * @param data the event payload, may be {@code null}.
     * @return {@code false} if the event was dropped or this context is no longer valid.
     */
    boolean sendEvent(String type, @Nullable Object data);
}
