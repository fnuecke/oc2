/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.device;

import li.cil.sedna.api.device.serial.SerialDevice;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class GuestTestChannel {
    private static final int MAX_LINE_LENGTH = 4 * 1024;

    // ------------------------------------------------------------- //

    private final SerialDevice device;
    private final byte[] line = new byte[MAX_LINE_LENGTH];
    private int lineLength;
    private final List<String> inbound = new ArrayList<>();
    private final Deque<ByteBuffer> outbound = new ArrayDeque<>();

    // ------------------------------------------------------------- //

    public GuestTestChannel(final SerialDevice device) {
        this.device = device;
    }

    // ------------------------------------------------------------- //

    public synchronized void send(final String command) {
        outbound.add(ByteBuffer.wrap((command + "\n").getBytes(StandardCharsets.UTF_8)));
    }

    public synchronized List<String> receive() {
        if (inbound.isEmpty()) {
            return List.of();
        }

        final List<String> result = List.copyOf(inbound);
        inbound.clear();
        return result;
    }

    synchronized void step() {
        read();
        write();
    }

    // ------------------------------------------------------------- //

    private void read() {
        int value;
        while ((value = device.read()) >= 0) {
            if (value == '\n') {
                inbound.add(new String(line, 0, lineLength, StandardCharsets.UTF_8));
                lineLength = 0;
            } else if (value != '\r' && lineLength < line.length) {
                line[lineLength++] = (byte) value;
            }
        }
    }

    private void write() {
        boolean wrote = false;
        while (!outbound.isEmpty() && device.canPutByte()) {
            final ByteBuffer buffer = outbound.peek();
            device.putByte(buffer.get());
            wrote = true;
            if (!buffer.hasRemaining()) {
                outbound.remove();
            }
        }

        if (wrote) {
            device.flush();
        }
    }
}
