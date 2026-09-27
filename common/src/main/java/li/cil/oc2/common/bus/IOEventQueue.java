/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus;

import li.cil.ceres.api.Serialized;
import li.cil.sedna.api.Interrupt;

import java.util.Arrays;
import java.util.UUID;

final class IOEventQueue {
    private static final int CONTROL_QUEUE = 0b0000_0001;
    private static final int CONTROL_INTERRUPT = 0b0000_0010;
    private static final int STATUS_OVERFLOW = 0b0100_0000;
    private static final int STATUS_PENDING = 0b1000_0000;

    private static final int CAPACITY = 16;
    private static final int RECORD_SIZE = 4;
    private static final int NO_EVENT = 0xFF;
    private static final UUID NO_KEY = new UUID(0, 0);

    // --------------------------------------------------------------------- //

    @Serialized
    private int control;
    @Serialized
    private final UUID[] keys = new UUID[CAPACITY];
    @Serialized
    private final int[] payloads = new int[CAPACITY];
    @Serialized
    private int head;
    @Serialized
    private int count;
    @Serialized
    private int cursor;

    // --------------------------------------------------------------------- //

    private final transient IODeviceRegistry registry;
    private final transient Interrupt interrupt = new Interrupt();
    private transient volatile boolean hasEnqueued;

    // --------------------------------------------------------------------- //

    IOEventQueue(final IODeviceRegistry registry) {
        this.registry = registry;
        Arrays.fill(keys, NO_KEY);
    }

    // --------------------------------------------------------------------- //

    Interrupt getInterrupt() {
        return interrupt;
    }

    synchronized boolean enqueue(final UUID key, final int code, final int value) {
        if ((control & CONTROL_QUEUE) == 0 || !registry.contains(key)) {
            return false;
        }
        if (count == CAPACITY) {
            control |= STATUS_OVERFLOW;
            return false;
        }

        final int slot = (head + count) % CAPACITY;
        keys[slot] = key;
        payloads[slot] = code | (value << 8);
        count++;
        hasEnqueued = true;
        return true;
    }

    void step() {
        if (hasEnqueued) {
            synchronized (this) {
                hasEnqueued = false;
                updateInterrupt();
            }
        }
    }

    synchronized int readControl() {
        return count > 0 ? control | STATUS_PENDING : control;
    }

    synchronized void writeControl(final int value) {
        control = value & (CONTROL_QUEUE | CONTROL_INTERRUPT);
        if ((control & CONTROL_QUEUE) == 0) {
            clear();
        }
        updateInterrupt();
    }

    synchronized int readData() {
        if (count == 0) {
            return NO_EVENT;
        }

        final int value = cursor == 0
            ? registry.indexOf(keys[head])
            : (payloads[head] >>> ((cursor - 1) * 8)) & 0xFF;

        if (++cursor == RECORD_SIZE) {
            head = (head + 1) % CAPACITY;
            count--;
            cursor = 0;
            updateInterrupt();
        }

        return value;
    }

    synchronized void dropEventsOfDroppedDevices() {
        int kept = 0;
        for (int i = 0; i < count; i++) {
            final int slot = (head + i) % CAPACITY;
            final boolean isPartiallyRead = i == 0 && cursor > 0;
            if (!isPartiallyRead && !registry.contains(keys[slot])) {
                continue;
            }

            final int target = (head + kept) % CAPACITY;
            keys[target] = keys[slot];
            payloads[target] = payloads[slot];
            kept++;
        }
        count = kept;
        updateInterrupt();
    }

    synchronized void reset() {
        control = 0;
        clear();
        updateInterrupt();
    }

    // --------------------------------------------------------------------- //

    private void clear() {
        head = 0;
        count = 0;
        cursor = 0;
    }

    private void updateInterrupt() {
        if ((control & CONTROL_INTERRUPT) != 0 && count > 0) {
            interrupt.raiseInterrupt();
        } else {
            interrupt.lowerInterrupt();
        }
    }
}
