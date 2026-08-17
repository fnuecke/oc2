/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus;

import li.cil.ceres.api.Serialized;
import li.cil.oc2.common.Constants;
import li.cil.sedna.api.device.serial.SerialDevice;

import java.nio.ByteBuffer;

final class RPCPayloadChannel {
    private final SerialDevice device;

    @Serialized
    private final ByteBuffer guestToHost = ByteBuffer.allocate(Constants.RPC_MAX_PAYLOAD_SIZE);
    @Serialized
    private volatile ByteBuffer hostToGuest;

    RPCPayloadChannel(final SerialDevice device) {
        this.device = device;
    }

    // ------------------------------------------------------------- //

    void receive() {
        while (true) {
            if (guestToHost.hasRemaining()) {
                if (device.read(guestToHost) == 0) {
                    return;
                }
                continue;
            }

            // The buffer is full, or this payload was already given up on. Anything still
            // coming has to be drained regardless, so the guest does not get stuck on a port
            // nobody reads. But it does mean the payload does not fit. So we just drain it.
            final int position = guestToHost.position();
            final int limit = guestToHost.limit();

            guestToHost.clear();
            if (device.read(guestToHost) == 0) { // not oversized, just fits exactly
                guestToHost.position(0).limit(limit).position(position);
                return;
            }

            guestToHost.clear();
            guestToHost.limit(0); // marks payload too large
        }
    }

    byte[] take(final int length, final int expectedChecksum) {
        if (guestToHost.limit() == 0 || length != guestToHost.position()) {
            discard();
            throw new PayloadException(RPCDeviceBusAdapter.ERROR_PAYLOAD_MISMATCH);
        }

        guestToHost.flip();
        final byte[] payload = new byte[length];
        guestToHost.get(payload);
        guestToHost.clear();

        if (checksum(payload) != expectedChecksum) {
            throw new PayloadException(RPCDeviceBusAdapter.ERROR_PAYLOAD_CORRUPT);
        }

        return payload;
    }

    void discard() {
        guestToHost.clear();
    }

    boolean isSending() {
        return hostToGuest != null;
    }

    void send(final byte[] payload) {
        hostToGuest = ByteBuffer.wrap(payload);
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
        discard();
        hostToGuest = null;
    }

    // ------------------------------------------------------------- //

    static int checksum(final byte[] data) {
        int sum = 0;
        for (int i = 0; i < data.length; i += 4) {
            int word = 0;
            for (int j = 0; j < 4; j++) {
                final int index = i + j;
                word |= (index < data.length ? (data[index] & 0xFF) : 0) << (j * 8);
            }
            sum = Integer.rotateLeft(sum, 1) + word;
        }
        return sum;
    }

    static final class PayloadException extends RuntimeException {
        final String error;

        PayloadException(final String error) {
            super(error);
            this.error = error;
        }
    }
}
