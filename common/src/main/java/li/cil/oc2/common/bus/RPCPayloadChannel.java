/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus;

import li.cil.ceres.api.Serialized;
import li.cil.oc2.common.Constants;
import li.cil.sedna.api.device.serial.SerialDevice;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.zip.CRC32;

final class RPCPayloadChannel {
    private final SerialDevice device;

    @Serialized
    private final ByteBuffer guestToHost = ByteBuffer.allocate(Constants.RPC_MAX_PAYLOAD_SIZE);
    @Serialized
    private volatile ByteBuffer hostToGuest;

    RPCPayloadChannel(final SerialDevice device) {
        this.device = device;
    }

    // --------------------------------------------------------------------- //

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

    byte[][] take(final List<RPCDeviceBusAdapter.BlobReference> references) {
        long length = 0;
        for (final RPCDeviceBusAdapter.BlobReference reference : references) {
            if (reference.length() < 0) {
                length = -1;
                break;
            }
            length += reference.length();
        }
        if (guestToHost.limit() == 0 || length != guestToHost.position()) {
            discard();
            throw new PayloadException(RPCDeviceBusAdapter.ERROR_PAYLOAD_MISMATCH);
        }

        guestToHost.flip();
        final byte[][] payloads = new byte[references.size()][];
        for (int i = 0; i < payloads.length; i++) {
            payloads[i] = new byte[references.get(i).length()];
            guestToHost.get(payloads[i]);
        }
        guestToHost.clear();

        for (int i = 0; i < payloads.length; i++) {
            if (checksum(payloads[i]) != references.get(i).checksum()) {
                throw new PayloadException(RPCDeviceBusAdapter.ERROR_PAYLOAD_CORRUPT);
            }
        }

        return payloads;
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

    // --------------------------------------------------------------------- //

    static long checksum(final byte[] data) {
        final CRC32 crc = new CRC32();
        crc.update(data);
        return crc.getValue();
    }

    static final class PayloadException extends RuntimeException {
        final String error;

        PayloadException(final String error) {
            super(error);
            this.error = error;
        }
    }
}
