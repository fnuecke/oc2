package li.cil.oc2.common.vm;

import it.unimi.dsi.fastutil.bytes.ByteArrayFIFOQueue;
import li.cil.ceres.api.Serialized;
import li.cil.oc2.api.bus.device.vm.event.VMInitializationException;
import li.cil.oc2.api.bus.device.vm.event.VMInitializingEvent;
import li.cil.oc2.api.bus.device.vm.event.VMResumedRunningEvent;
import li.cil.oc2.api.bus.device.vm.event.VMResumingRunningEvent;
import li.cil.oc2.common.Constants;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;

public abstract class AbstractTerminalVirtualMachineRunner extends VirtualMachineRunner {
    private static final ByteBuffer TERMINAL_RESET_SEQUENCE = ByteBuffer.wrap(new byte[]{
            // Make sure we're in normal mode.
            'J',
            // Reset color and style.
            '\033', '[', '0', 'm',
            // Clear screen.
            '\033', '[', '2', 'J'
    });

    private final CommonVirtualMachine virtualMachine;
    private final Terminal terminal;

    ///////////////////////////////////////////////////////////////////

    // Thread-local buffers for lock-free read/writes in inner loop.
    private final ByteArrayFIFOQueue outputBuffer = new ByteArrayFIFOQueue(1024);
    private final ByteArrayFIFOQueue inputBuffer = new ByteArrayFIFOQueue(32);

    private boolean firedResumeEvent;
    @Serialized private boolean firedInitializationEvent;
    @Serialized private ITextComponent runtimeError;

    ///////////////////////////////////////////////////////////////////

    public AbstractTerminalVirtualMachineRunner(final CommonVirtualMachine virtualMachine, final Terminal terminal) {
        super(virtualMachine.board);
        this.virtualMachine = virtualMachine;
        this.terminal = terminal;
    }

    ///////////////////////////////////////////////////////////////////

    protected abstract void sendTerminalUpdateToClient(final ByteBuffer output);

    ///////////////////////////////////////////////////////////////////

    public void resetTerminal() {
        TERMINAL_RESET_SEQUENCE.clear();
        putTerminalOutput(TERMINAL_RESET_SEQUENCE);
    }

    public void putTerminalOutput(final ByteBuffer output) {
        if (output.hasRemaining()) {
            terminal.putOutput(output);

            output.flip();
            sendTerminalUpdateToClient(output);
        }
    }

    public void scheduleResumeEvent() {
        firedResumeEvent = false;
    }

    @Nullable
    @Override
    public ITextComponent getRuntimeError() {
        return runtimeError;
    }

    @Override
    public void tick() {
        virtualMachine.rpcAdapter.tick();

        super.tick();
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void handleBeforeRun() {
        if (!firedInitializationEvent) {
            firedInitializationEvent = true;
            try {
                virtualMachine.vmAdapter.postLifecycleEvent(new VMInitializingEvent(virtualMachine.board.getDefaultProgramStart()));
            } catch (final VMInitializationException e) {
                virtualMachine.board.setRunning(false);
                runtimeError = e.getErrorMessage().orElse(new TranslationTextComponent(Constants.COMPUTER_ERROR_UNKNOWN));
                return;
            }
        }

        if (!firedResumeEvent) {
            firedResumeEvent = true;
            virtualMachine.vmAdapter.postLifecycleEvent(new VMResumingRunningEvent());
            virtualMachine.vmAdapter.postLifecycleEvent(new VMResumedRunningEvent());
        }

        int value;
        while ((value = terminal.readInput()) != -1) {
            inputBuffer.enqueue((byte) value);
        }
    }

    @Override
    protected void step(final int cyclesPerStep) {
        while (!inputBuffer.isEmpty() && virtualMachine.uart.canPutByte()) {
            virtualMachine.uart.putByte(inputBuffer.dequeueByte());
        }
        virtualMachine.uart.flush();

        int value;
        while ((value = virtualMachine.uart.read()) != -1) {
            outputBuffer.enqueue((byte) value);
        }

        virtualMachine.rpcAdapter.step(cyclesPerStep);
    }

    @Override
    protected void handleAfterRun() {
        final ByteBuffer output = ByteBuffer.allocate(outputBuffer.size());
        while (!outputBuffer.isEmpty()) {
            output.put(outputBuffer.dequeueByte());
        }

        output.flip();
        putTerminalOutput(output);
    }
}
