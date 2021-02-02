package li.cil.oc2.common.util;

import li.cil.oc2.common.vm.Terminal;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

public final class TerminalUtils {
    private static final ByteBuffer TERMINAL_RESET_SEQUENCE = ByteBuffer.wrap(new byte[]{
            // Make sure we're in normal mode.
            'J',
            // Reset color and style.
            '\033', '[', '0', 'm',
            // Clear screen.
            '\033', '[', '2', 'J'
    });

    ///////////////////////////////////////////////////////////////////

    public static void resetTerminal(final Terminal terminal, final Consumer<ByteBuffer> packetSender) {
        TERMINAL_RESET_SEQUENCE.clear();
        terminal.putOutput(TERMINAL_RESET_SEQUENCE);

        TERMINAL_RESET_SEQUENCE.flip();
        packetSender.accept(TERMINAL_RESET_SEQUENCE);
    }
}
