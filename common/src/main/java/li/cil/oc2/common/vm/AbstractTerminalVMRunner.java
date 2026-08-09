/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.vm;

import it.unimi.dsi.fastutil.bytes.ByteArrayFIFOQueue;
import li.cil.sedna.device.serial.UART16550A;

import java.nio.ByteBuffer;

public abstract class AbstractTerminalVMRunner extends VMRunner {
    private final UART16550A uart;
    private final Terminal terminal;

    ///////////////////////////////////////////////////////////////////

    // Thread-local buffers for lock-free read/writes in inner loop.
    private final ByteArrayFIFOQueue outputBuffer = new ByteArrayFIFOQueue(1024);
    private final ByteArrayFIFOQueue inputBuffer = new ByteArrayFIFOQueue(32);

    ///////////////////////////////////////////////////////////////////

    public AbstractTerminalVMRunner(final AbstractVirtualMachine virtualMachine, final Terminal terminal) {
        super(virtualMachine);
        this.terminal = terminal;
        uart = virtualMachine.state.builtinDevices.uart;
    }

    ///////////////////////////////////////////////////////////////////

    protected abstract void sendTerminalUpdateToClient(final ByteBuffer output);

    ///////////////////////////////////////////////////////////////////

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

        final ByteBuffer output = ByteBuffer.allocate(outputBuffer.size());
        while (!outputBuffer.isEmpty()) {
            output.put(outputBuffer.dequeueByte());
        }

        output.flip();
        putTerminalOutput(output);
    }

    ///////////////////////////////////////////////////////////////////

    private void putTerminalOutput(final ByteBuffer output) {
        if (output.hasRemaining()) {
            terminal.putOutput(output);

            output.flip();
            sendTerminalUpdateToClient(output);
        }
    }
}
