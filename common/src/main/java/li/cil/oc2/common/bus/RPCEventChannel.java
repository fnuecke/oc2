/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus;

import it.unimi.dsi.fastutil.bytes.ByteArrayFIFOQueue;
import li.cil.ceres.api.Serialized;
import li.cil.oc2.common.Constants;
import li.cil.sedna.api.device.serial.SerialDevice;

import java.nio.ByteBuffer;

final class RPCEventChannel {
    private static final int MAX_QUEUED_SIZE = 8 * Constants.KILOBYTE;
    private static final int DISCARD_BUFFER_SIZE = 256;

    // --------------------------------------------------------------------- //

    private final SerialDevice device;

    private final transient ByteBuffer discard = ByteBuffer.allocate(DISCARD_BUFFER_SIZE);

    @Serialized
    private final ByteArrayFIFOQueue queued = new ByteArrayFIFOQueue();
    @Serialized
    private int dropped;

    // --------------------------------------------------------------------- //

    RPCEventChannel(final SerialDevice device) {
        this.device = device;
    }

    // --------------------------------------------------------------------- //

    boolean sendEvent(final ByteBuffer frame) {
        synchronized (queued) {
            if (queued.size() + frame.remaining() > MAX_QUEUED_SIZE) {
                dropped += dropped < 0 ? -1 : 1;
                return false;
            }

            while (frame.hasRemaining()) {
                queued.enqueue(frame.get());
            }
        }

        return true;
    }

    void sendNotice(final ByteBuffer frame) {
        synchronized (queued) {
            dropped = -1; // nothing refused since this one; anything new counts further down
            while (frame.hasRemaining()) {
                queued.enqueue(frame.get());
            }
        }
    }

    int takeDropped() {
        synchronized (queued) {
            if (dropped <= 0) {
                return 0;
            }

            final int count = dropped;
            dropped = 0;
            return count;
        }
    }

    void flush() {
        while (device.read(discard.clear()) > 0) {
            // Guest shouldn't send anything here, but let's just drain it to not block.
        }

        synchronized (queued) {
            while (!queued.isEmpty() && device.canPutByte()) {
                device.putByte(queued.dequeueByte());
            }

            if (queued.isEmpty() && dropped < 0) {
                dropped = -dropped - 1; // the notice is out; swap to pending new drops, if any
            }
        }

        device.flush();
    }

    void reset() {
        synchronized (queued) {
            queued.clear();
            dropped = 0;
        }
    }
}
