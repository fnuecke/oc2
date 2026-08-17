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

    // ------------------------------------------------------------- //

    private final SerialDevice device;

    private final transient ByteBuffer discard = ByteBuffer.allocate(DISCARD_BUFFER_SIZE);

    @Serialized
    private final ByteArrayFIFOQueue queued = new ByteArrayFIFOQueue();

    // ------------------------------------------------------------- //

    RPCEventChannel(final SerialDevice device) {
        this.device = device;
    }

    // ------------------------------------------------------------- //

    boolean addEvent(final ByteBuffer frame) {
        synchronized (queued) {
            if (queued.size() + frame.remaining() > MAX_QUEUED_SIZE) {
                return false;
            }

            while (frame.hasRemaining()) {
                queued.enqueue(frame.get());
            }
        }

        return true;
    }

    void flush() {
        while (device.read(discard.clear()) > 0) {
            // Guest shouldn't send anything here, but let's just drain it to not block.
        }

        synchronized (queued) {
            while (!queued.isEmpty() && device.canPutByte()) {
                device.putByte(queued.dequeueByte());
            }
        }

        device.flush();
    }

    void reset() {
        synchronized (queued) {
            queued.clear();
        }
    }
}
