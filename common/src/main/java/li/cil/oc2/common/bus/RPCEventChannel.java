/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus;

import it.unimi.dsi.fastutil.bytes.ByteArrayFIFOQueue;
import li.cil.ceres.api.Serialized;
import li.cil.oc2.common.Constants;
import li.cil.sedna.api.device.serial.SerialDevice;

import java.nio.ByteBuffer;

final class RPCEventChannel {
    private static final int MAX_QUEUED_SIZE = 8 * Constants.KILOBYTE;

    // ------------------------------------------------------------- //

    private final SerialDevice device;

    @Serialized
    private final ByteArrayFIFOQueue queued = new ByteArrayFIFOQueue();

    // ------------------------------------------------------------- //

    RPCEventChannel(final SerialDevice device) {
        this.device = device;
    }

    // ------------------------------------------------------------- //

    boolean addEvent(final ByteBuffer frame) {
        if (queued.size() + frame.remaining() > MAX_QUEUED_SIZE) {
            return false;
        }

        while (frame.hasRemaining()) {
            queued.enqueue(frame.get());
        }

        return true;
    }

    void flush() {
        while (device.read() >= 0) {
            // Guest shouldn't send anything here, but let's just drain it to not block.
        }

        while (!queued.isEmpty() && device.canPutByte()) {
            device.putByte(queued.dequeueByte());
        }

        device.flush();
    }

    void reset() {
        queued.clear();
    }
}
