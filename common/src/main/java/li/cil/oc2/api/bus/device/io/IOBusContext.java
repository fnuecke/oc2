/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.bus.device.io;

/**
 * A device's handle on the computer it was mounted in.
 * <p>
 * Passed to {@link IODevice#mountIO(IOBusContext)} and, as the same instance, to
 * {@link IODevice#unmountIO(IOBusContext)}. Valid in between. A device mounted
 * more than once at a time, e.g. in several computers, receives one context per mount.
 */
public interface IOBusContext {
    /**
     * The largest value {@code code} may take in {@link #sendEvent(int, int)}.
     */
    int MAX_EVENT_CODE = 0xFF;

    /**
     * The largest value {@code value} may take in {@link #sendEvent(int, int)}.
     */
    int MAX_EVENT_VALUE = 0xFFFF;

    /**
     * Queues an event for the guest of this computer, attributed to this device.
     * <p>
     * May be called from any thread. Guests opt in to receiving events; until they do,
     * events are dropped. The queue is bounded; an event that does not fit is dropped;
     * such overflows are reported to the guest.
     * <p>
     * Event codes are per device, like method codes. Document them in the device's
     * {@link li.cil.oc2.api.bus.device.object.IODeviceDescription}.
     *
     * @param code  the event code, at most {@link #MAX_EVENT_CODE}.
     * @param value the event value, at most {@link #MAX_EVENT_VALUE}.
     * @return {@code false} if the event was dropped or this context is no longer valid.
     * @throws IllegalArgumentException if {@code code} or {@code value} is out of range.
     */
    boolean sendEvent(int code, int value);
}
