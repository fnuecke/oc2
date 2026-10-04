/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.serial;

import li.cil.sedna.api.Sizes;
import li.cil.sedna.device.serial.UART16550A;

final class Uart {
    static final int LSR_DR = 1 << 0;
    static final int LSR_FE = 1 << 3;

    private static final int RBR_OFFSET = 0;
    private static final int THR_OFFSET = 0;
    private static final int DLL_OFFSET = 0;
    private static final int DLM_OFFSET = 1;
    private static final int FCR_OFFSET = 2;
    private static final int LCR_OFFSET = 3;
    private static final int LSR_OFFSET = 5;

    private static final int FCR_FE = 1 << 0;
    private static final int LCR_DLAB = 1 << 7;

    // --------------------------------------------------------------------- //

    static void enableFifo(final BufferedSerialDevice port) {
        port.store(FCR_OFFSET, FCR_FE, Sizes.SIZE_8_LOG2);
    }

    static void setBaudRate(final BufferedSerialDevice port, final int baudRate) {
        final int divisor = UART16550A.CLOCK_FREQUENCY / (16 * baudRate);
        port.store(LCR_OFFSET, LCR_DLAB, Sizes.SIZE_8_LOG2);
        port.store(DLL_OFFSET, divisor & 0xFF, Sizes.SIZE_8_LOG2);
        port.store(DLM_OFFSET, divisor >>> 8, Sizes.SIZE_8_LOG2);
        port.store(LCR_OFFSET, 0, Sizes.SIZE_8_LOG2);
    }

    static void write(final BufferedSerialDevice port, final int value) {
        port.store(THR_OFFSET, value, Sizes.SIZE_8_LOG2);
    }

    static char read(final BufferedSerialDevice port) {
        return (char) (port.load(RBR_OFFSET, Sizes.SIZE_8_LOG2) & 0xFF);
    }

    static int lineStatus(final BufferedSerialDevice port) {
        return (int) port.load(LSR_OFFSET, Sizes.SIZE_8_LOG2) & 0xFF;
    }

    static String drain(final BufferedSerialDevice port) {
        final StringBuilder result = new StringBuilder();
        port.step(0);
        while ((lineStatus(port) & LSR_DR) != 0) {
            result.append(read(port));
            port.step(0);
        }
        return result.toString();
    }

    // --------------------------------------------------------------------- //

    private Uart() {
    }
}
