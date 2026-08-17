/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus;

import it.unimi.dsi.fastutil.bytes.ByteArrayFIFOQueue;
import li.cil.sedna.api.device.serial.SerialDevice;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

final class TestSerialDevice implements SerialDevice {
    private final ByteArrayFIFOQueue transmit = new ByteArrayFIFOQueue();
    private final ByteArrayFIFOQueue receive = new ByteArrayFIFOQueue();
    private final int capacity;
    private final int transmitCapacity;

    TestSerialDevice() {
        this(Integer.MAX_VALUE);
    }

    TestSerialDevice(final int capacity) {
        this(capacity, Integer.MAX_VALUE);
    }

    TestSerialDevice(final int capacity, final int transmitCapacity) {
        this.capacity = capacity;
        this.transmitCapacity = transmitCapacity;
    }

    public void putAsVM(final String data) {
        final byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < bytes.length; i++) {
            transmit.enqueue(bytes[i]);
        }
        transmit.enqueue((byte) 0);
    }

    public int offerRawAsVM(final byte[] data, final int offset) {
        int i = offset;
        while (i < data.length && transmit.size() < transmitCapacity) {
            transmit.enqueue(data[i++]);
        }
        return i;
    }

    public void putRawAsVM(final byte[] data) {
        for (final byte value : data) {
            transmit.enqueue(value);
        }
    }

    @Nullable
    public String readMessageAsVM() {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int consumed = 0;
        boolean terminated = false;

        while (!receive.isEmpty()) {
            final byte value = receive.dequeueByte();
            consumed++;

            if (value == 0) {
                if (bytes.size() == 0) {
                    continue;
                }
                terminated = true;
                break;
            }

            bytes.write(value);
        }

        if (terminated) {
            return bytes.toString(StandardCharsets.UTF_8);
        }

        final byte[] partial = bytes.toByteArray();
        for (int i = partial.length - 1; i >= 0; i--) {
            receive.enqueueFirst(partial[i]);
        }
        for (int i = consumed - partial.length; i > 0; i--) {
            receive.enqueueFirst((byte) 0);
        }

        return null;
    }

    public byte[] drainAsVM() {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        while (!receive.isEmpty()) {
            bytes.write(receive.dequeueByte());
        }
        return bytes.toByteArray();
    }

    @Override
    public int read() {
        return transmit.isEmpty() ? -1 : (transmit.dequeueByte() & 0xFF);
    }

    @Override
    public int read(final ByteBuffer dst) {
        int count = 0;
        while (dst.hasRemaining() && !transmit.isEmpty()) {
            dst.put(transmit.dequeueByte());
            count++;
        }
        return count;
    }

    @Override
    public boolean canPutByte() {
        return receive.size() < capacity;
    }

    @Override
    public void putByte(final byte value) {
        receive.enqueue(value);
    }
}
