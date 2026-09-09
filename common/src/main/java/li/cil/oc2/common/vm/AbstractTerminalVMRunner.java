/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.vm;

import it.unimi.dsi.fastutil.bytes.ByteArrayFIFOQueue;
import li.cil.sedna.api.device.serial.SerialDevice;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class AbstractTerminalVMRunner extends VMRunner {
    private final SerialDevice uart;
    private final Terminal terminal;

    // --------------------------------------------------------------------- //

    // Thread-local buffers for lock-free read/writes in inner loop.
    private final ByteArrayFIFOQueue outputBuffer = new ByteArrayFIFOQueue(1024);
    private final ByteArrayFIFOQueue inputBuffer = new ByteArrayFIFOQueue(32);
    private final Queue<byte[]> pendingOutput = new ConcurrentLinkedQueue<>();

    // --------------------------------------------------------------------- //

    public AbstractTerminalVMRunner(final AbstractArchitecture architecture, final Terminal terminal) {
        super(architecture);
        this.terminal = terminal;
        uart = architecture.getTerminalDevice();
    }

    // --------------------------------------------------------------------- //

    protected abstract void sendTerminalUpdateToClient(final ByteBuffer output);

    // --------------------------------------------------------------------- //

    @Override
    public void tick() {
        flushPendingOutput();

        super.tick();
    }

    @Override
    public void join() {
        super.join();

        flushPendingOutput();
    }

    @Override
    protected void handleBeforeRun() {
        super.handleBeforeRun();

        int value;
        while ((value = terminal.readInput()) != -1) {
            inputBuffer.enqueue((byte) value);
        }
    }

    @Override
    protected void step(final int cyclesPerStep) {
        super.step(cyclesPerStep);

        while (!inputBuffer.isEmpty() && uart.canPutByte()) {
            uart.putByte(inputBuffer.dequeueByte());
        }
        uart.flush();

        int value;
        while ((value = uart.read()) != -1) {
            outputBuffer.enqueue((byte) value);
        }
    }

    @Override
    protected void handleAfterRun() {
        super.handleAfterRun();

        if (outputBuffer.isEmpty()) {
            return;
        }

        final byte[] output = new byte[outputBuffer.size()];
        for (int i = 0; i < output.length; i++) {
            output[i] = outputBuffer.dequeueByte();
        }

        terminal.putOutput(ByteBuffer.wrap(output));
        pendingOutput.add(output);
    }

    // --------------------------------------------------------------------- //

    private void flushPendingOutput() {
        final byte[] first = pendingOutput.poll();
        if (first == null) {
            return;
        }

        if (pendingOutput.isEmpty()) {
            sendTerminalUpdateToClient(ByteBuffer.wrap(first));
            return;
        }

        final ArrayList<byte[]> chunks = new ArrayList<>();
        chunks.add(first);

        int length = first.length;
        byte[] chunk;
        while ((chunk = pendingOutput.poll()) != null) {
            chunks.add(chunk);
            length += chunk.length;
        }

        final byte[] merged = new byte[length];
        int offset = 0;
        for (final byte[] value : chunks) {
            System.arraycopy(value, 0, merged, offset, value.length);
            offset += value.length;
        }

        sendTerminalUpdateToClient(ByteBuffer.wrap(merged));
    }
}
