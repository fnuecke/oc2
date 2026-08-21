/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus;

import li.cil.ceres.api.Serialized;
import li.cil.sedna.api.device.serial.SerialDevice;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * NUL-delimited JSON data frames over one serial port, in both directions.
 */
final class RPCMessageChannel {
    private static final byte[] MESSAGE_DELIMITER = "\0".getBytes(StandardCharsets.US_ASCII);

    // --------------------------------------------------------------------- //

    private final SerialDevice device;

    @Serialized
    private final ByteBuffer guestToHost; // for data written to device by VM
    @Serialized
    private volatile ByteBuffer hostToGuest; // for data written by device to VM

    // --------------------------------------------------------------------- //

    RPCMessageChannel(final SerialDevice device, final int maxMessageSize) {
        this.device = device;
        this.guestToHost = ByteBuffer.allocate(maxMessageSize);
    }

    // --------------------------------------------------------------------- //

    boolean isSending() {
        return hostToGuest != null;
    }

    void send(final ByteBuffer frame) {
        if (hostToGuest != null) {
            throw new IllegalStateException("a frame is already going out on this channel");
        }
        hostToGuest = frame;
    }

    boolean readFrame(final Consumer<byte[]> onFrame, final Runnable onOversized) {
        int value;
        while ((value = device.read()) >= 0) {
            if (value != 0) {
                if (guestToHost.hasRemaining()) {
                    guestToHost.put((byte) value);
                } else {
                    guestToHost.clear();
                    guestToHost.limit(0); // marks message too large
                }
                continue;
            }

            if (guestToHost.limit() == 0) {
                guestToHost.clear();
                onOversized.run();
                return true;
            }

            guestToHost.flip();
            byte[] frame = null;
            if (guestToHost.hasRemaining()) {
                frame = new byte[guestToHost.remaining()];
                guestToHost.get(frame);
            }
            guestToHost.clear();

            if (frame != null) {
                onFrame.accept(frame);
                return true;
            }
            // Otherwise this was just the delimiter pair that separates two frames.
        }

        return false;
    }

    void flush() {
        if (hostToGuest == null) {
            return;
        }

        while (hostToGuest.hasRemaining() && device.canPutByte()) {
            device.putByte(hostToGuest.get());
        }

        device.flush();

        if (!hostToGuest.hasRemaining()) {
            hostToGuest = null;
        }
    }

    void reset() {
        guestToHost.clear();
        hostToGuest = null;
    }

    // --------------------------------------------------------------------- //

    static ByteBuffer frame(final byte[] payload) {
        final ByteBuffer buffer = ByteBuffer.allocate(payload.length + MESSAGE_DELIMITER.length * 2);

        // In case we went through a reset and the VM was in the middle of reading
        // a message we inject a delimiter up front to cause the truncated message
        // to be discarded.
        buffer.put(MESSAGE_DELIMITER);
        buffer.put(payload);

        // We follow up each message with a delimiter, too, so the VM knows when the
        // message has been completed. This will lead to two delimiters between most
        // messages. The VM is expected to ignore such "empty" messages.
        buffer.put(MESSAGE_DELIMITER);

        buffer.flip();
        return buffer;
    }
}
