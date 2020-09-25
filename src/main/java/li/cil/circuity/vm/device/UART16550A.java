package li.cil.circuity.vm.device;

import it.unimi.dsi.fastutil.bytes.ByteArrayFIFOQueue;
import it.unimi.dsi.fastutil.bytes.BytePriorityQueue;
import li.cil.circuity.api.vm.Interrupt;
import li.cil.circuity.api.vm.device.InterruptSource;
import li.cil.circuity.api.vm.device.Resettable;
import li.cil.circuity.api.vm.device.Steppable;
import li.cil.circuity.api.vm.device.memory.MemoryMappedDevice;
import li.cil.circuity.api.vm.device.memory.Sizes;

import java.util.Collections;

/**
 * Implements a 16550A UART.
 * <p>
 * This is not a cycle-correct implementation. It does not care about baudrates and
 * timeout delays. But it's good enough to pump data into and out of a virtual machine.
 * <p>
 * See: https://web.archive.org/web/20200207194832/https://www.lammertbies.nl/comm/info/serial-uart
 */
@SuppressWarnings("PointlessBitwiseExpression")
public final class UART16550A implements Resettable, Steppable, MemoryMappedDevice, InterruptSource {
    private static final int UART_RBR_OFFSET = 0; // Receive buffer register (Read-only)
    private static final int UART_THR_OFFSET = 0; // Transmitter holding register (Write-only)
    private static final int UART_IER_OFFSET = 1; // Interrupt enable register (Read-write)
    private static final int UART_FCR_OFFSET = 2; // FIFO control register (Write-only)
    private static final int UART_IIR_OFFSET = 2; // Interrupt identification register (Read-write)
    private static final int UART_LCR_OFFSET = 3; // Line control register (Read-write)
    private static final int UART_MCR_OFFSET = 4; // Modem control register (Read-write)
    private static final int UART_LSR_OFFSET = 5; // Line status register (Read-only)
    private static final int UART_MSR_OFFSET = 6; // Modem status register (Read-only)
    private static final int UART_SCR_OFFSET = 7; // Scratch register (Read-only)

    private static final int UART_DLL_OFFSET = 0; // Divisor latch register (least significant byte) (Read-write)
    private static final int UART_DLM_OFFSET = 1; // Divisor latch register (most significant byte) (Read-write)

    private static final int UART_IER_RDI = 0b0001; // Received data available
    private static final int UART_IER_THRI = 0b0010; // Transmitter holding register empty
    private static final int UART_IER_RLSI = 0b0100; // Receiver line status register change
    private static final int UART_IER_MSI = 0b1000; // Modem status register change

    private static final int UART_IIR_NO_INTERRUPT = 0b00000001; // Any interrupt pending bit
    private static final int UART_IIR_ID_MASK = 0b00001110; // Last interrupt type mask
    private static final int UART_IIR_MSI = 0x0000;  // Modem status change
    private static final int UART_IIR_THRI = 0b0010; // Transmitter holding register empty
    private static final int UART_IIR_RDI = 0b0100;  // Received data available
    private static final int UART_IIR_RLSI = 0b0110; // Line status change
    private static final int UART_IIR_CTI = 0b1100;  // Character timeout
    private static final int UART_IIR_NO_FIFO = 0b00000000;       // No FIFO
    private static final int UART_IIR_UNUSABLE_FIFO = 0b10000000; // Unusable FIFO
    private static final int UART_IIR_FIFO_ENABLED = 0b11000000;  // FIFO enabled

    private static final int UART_LCR_WORD_LENGTH_8 = 0b00000011; // Data word length = 8
    private static final int UART_LCR_STOP_BITS = 0b00000100;     // 0 = 1 stop bit, 1 = 1.5 stop bits (5 bits word) / 2 stop bits (7, 7 or 8 bits word)
    private static final int UART_LCR_ODD_PARITY = 0b00001000;    // Odd parity
    private static final int UART_LCR_EVEN_PARITY = 0b00011000;   // Even parity
    private static final int UART_LCR_HIGH_PARITY = 0b00101000;   // High parity (stick)
    private static final int UART_LCR_LOW_PARITY = 0b00111000;    // Low parity (stick)
    private static final int UART_LCR_BREAK_SIGNAL = 0b01000000;  // Break signal enabled
    private static final int UART_LCR_DLAB = 0b10000000;          // Divisor latch access bit
    private static final int UART_LCR_MUTABLE_BITS_MASK = UART_LCR_DLAB; // We only support a few settings...

    private static final int UART_MCR_DTR = 1 << 0; // Data terminal ready
    private static final int UART_MCR_RTS = 1 << 1; // Request to send
    private static final int UART_MCR_AO1 = 1 << 2; // Auxiliary output 1
    private static final int UART_MCR_AO2 = 1 << 3; // Auxiliary output 2
    private static final int UART_MCR_LBM = 1 << 4; // Loopback mode

    private static final int UART_LSR_DR = 1 << 0;    // Receiver data ready
    private static final int UART_LSR_OE = 1 << 1;    // Overrun error indicator
    private static final int UART_LSR_PE = 1 << 2;    // Parity error indicator
    private static final int UART_LSR_FE = 1 << 3;    // Frame error indicator
    private static final int UART_LSR_BI = 1 << 4;    // Break interrupt indicator
    private static final int UART_LSR_THRE = 1 << 5;  // Transmit-hold-register empty
    private static final int UART_LSR_TEMT = 1 << 6;  // Transmitter empty
    private static final int UART_LSR_FIFOE = 1 << 7; // Fifo error
    private static final int UART_LSR_IRQ_MASK = UART_LSR_BI | UART_LSR_FE | UART_LSR_PE | UART_LSR_OE;

    private static final int UART_MSR_DCTS = 1 << 0; // Change in Clear to send
    private static final int UART_MSR_DDSR = 1 << 1; // Change in Data set ready
    private static final int UART_MSR_TERI = 1 << 2; // Trailing edge Ring indicator
    private static final int UART_MSR_DDCD = 1 << 3; // Change in Carrier detect
    private static final int UART_MSR_CTS = 1 << 4;  // Clear to send
    private static final int UART_MSR_DSR = 1 << 5;  // Data set ready
    private static final int UART_MSR_RI = 1 << 6;   // Ring indicator
    private static final int UART_MSR_DCD = 1 << 7;  // Carrier detect
    private static final int UART_MSR_DIRTY = UART_MSR_DCTS | UART_MSR_DDSR | UART_MSR_TERI | UART_MSR_DDCD;

    private static final int UART_FCR_FE = 1 << 0;  // Enable/disable FIFOs
    private static final int UART_FCR_RFR = 1 << 1; // Clear receive FIFO
    private static final int UART_FCR_XFR = 1 << 2; // Clear transmit FIFO
    private static final int UART_FCR_DMS = 1 << 3; // Select DMA mode
    private static final int UART_FCR_ITL_MASK = 0b11000000; // Receive FIFO interrupt trigger level mask
    private static final int UART_FCR_ITL1 = 0b00000000; // Receive FIFO interrupt trigger at 1 byte
    private static final int UART_FCR_ITL2 = 0b01000000; // Receive FIFO interrupt trigger at 4 bytes
    private static final int UART_FCR_ITL3 = 0b10000000; // Receive FIFO interrupt trigger at 8 bytes
    private static final int UART_FCR_ITL4 = 0b11000000; // Receive FIFO interrupt trigger at 14 bytes

    private static final int UART_DL_2304 = 0x0900; // 50 Baud
    private static final int UART_DL_384 = 0x0180; // 300 Baud
    private static final int UART_DL_96 = 0x0060; // 1200 Baud
    private static final int UART_DL_48 = 0x0030; // 2400 Baud
    private static final int UART_DL_24 = 0x0018; // 4800 Baud
    private static final int UART_DL_12 = 0x000C; // 9600 Baud
    private static final int UART_DL_6 = 0x0006; // 19200 Baud
    private static final int UART_DL_3 = 0x0003; // 38400 Baud
    private static final int UART_DL_2 = 0x0002; // 57600 Baud
    private static final int UART_DL_1 = 0x0001; // 115200 Baud

    private static final int FIFO_QUEUE_CAPACITY = 16;

    private byte rbr;
    private byte thr;
    private byte ier;
    private byte iir;
    private byte fcr;
    private byte lcr;
    private byte mcr;
    private byte lsr;
    private byte msr;
    private byte scr;
    private short dl;

    private int triggerLevel;
    private final BytePriorityQueue receiveFifo = new ByteArrayFIFOQueue(FIFO_QUEUE_CAPACITY);
    private final BytePriorityQueue transmitFifo = new ByteArrayFIFOQueue(FIFO_QUEUE_CAPACITY);

    private boolean interruptUpdatePending;
    private boolean transmitInterruptPending;
    private boolean timeoutInterruptPending;
    private final Interrupt interrupt = new Interrupt();

    private final Object lock = new Object();

    public UART16550A() {
        reset();
    }

    public Interrupt getInterrupt() {
        return interrupt;
    }

    public int read() {
        synchronized (lock) {
            if ((lsr & UART_LSR_TEMT) != 0) {
                return -1;
            }

            final byte value;
            if ((fcr & UART_FCR_FE) != 0) {
                value = transmitFifo.dequeueByte();
                if (transmitFifo.isEmpty()) {
                    lsr |= UART_LSR_THRE | UART_LSR_TEMT;
                }
            } else {
                value = thr;
                lsr |= UART_LSR_THRE | UART_LSR_TEMT;
            }

            if ((lsr & UART_LSR_THRE) != 0 && !transmitInterruptPending) {
                transmitInterruptPending = true;
                interruptUpdatePending = true;
            }

            return value;
        }
    }

    public boolean canPutByte() {
        return receiveFifo.size() < FIFO_QUEUE_CAPACITY;
    }

    public void putByte(final byte value) {
        synchronized (lock) {
            if ((fcr & UART_FCR_FE) != 0) {
                if (receiveFifo.size() < FIFO_QUEUE_CAPACITY) {
                    receiveFifo.enqueue(value);
                } else {
                    lsr |= UART_LSR_OE;
                }
                lsr |= UART_LSR_DR;
            } else {
                if ((lsr & UART_LSR_DR) != 0) {
                    lsr |= UART_LSR_OE;
                }
                rbr = value;
                lsr |= UART_LSR_DR;
            }

            timeoutInterruptPending = true; // Not correct, but good enough.
            interruptUpdatePending = true;
        }
    }

    public void putBreak() {
        synchronized (lock) {
            rbr = 0;
            // QEMU says: when the LSR_DR is set a null byte is pushed into the fifo.
            putByte((byte) 0);
            lsr |= UART_LSR_BI | UART_LSR_DR;
        }
    }

    @Override
    public void reset() {
        rbr = 0;
        thr = 0;
        ier = 0;
        iir = (byte) UART_IIR_NO_INTERRUPT;
        fcr = 0;
        lcr = 0;
        mcr = (byte) UART_MCR_AO2;
        lsr = (byte) (UART_LSR_THRE | UART_LSR_TEMT);
        msr = (byte) (UART_MSR_CTS | UART_MSR_DCD | UART_MSR_DSR);
        scr = 0;
        dl = UART_DL_12;

        triggerLevel = 1;

        receiveFifo.clear();
        transmitFifo.clear();

        interruptUpdatePending = false;
        transmitInterruptPending = false;
        timeoutInterruptPending = false;
        interrupt.lowerInterrupt();
    }

    @Override
    public void step(final int cycles) {
        if (interruptUpdatePending) {
            updateInterrupts();
        }
    }

    @Override
    public int getLength() {
        return 0x100;
    }

    @Override
    public int getSupportedSizes() {
        return 1 << Sizes.SIZE_8_LOG2;
    }

    @Override
    public int load(final int offset, final int sizeLog2) {
        assert sizeLog2 == Sizes.SIZE_8_LOG2;
        switch (offset) {
            // case UART_DLL_OFFSET:
            case UART_RBR_OFFSET: {
                if ((lcr & UART_LCR_DLAB) != 0) { // UART_DLL
                    return (byte) dl;
                } else { // UART_RBR
                    final byte result;
                    synchronized (lock) {
                        if ((fcr & UART_FCR_FE) != 0) { // FIFO enabled
                            result = receiveFifo.isEmpty() ? 0 : receiveFifo.dequeueByte();
                            if (receiveFifo.isEmpty()) {
                                lsr &= ~(UART_LSR_DR | UART_LSR_BI);
                                timeoutInterruptPending = false;
                            } else {
                                timeoutInterruptPending = true; // Not correct, but good enough.
                            }
                        } else { // No FIFO
                            result = rbr;
                            lsr &= ~(UART_LSR_DR | UART_LSR_BI);
                        }
                        updateInterrupts();
                    }

                    if ((mcr & UART_MCR_LBM) == 0) {
                        // TODO Fire event that input was accepted?
                    }

                    return result;
                }
            }

            // case UART_DLM_OFFSET:
            case UART_IER_OFFSET: {
                if ((lcr & UART_LCR_DLAB) != 0) { // UART_DLM
                    return (byte) (dl >>> 8);
                } else { // UART_IER
                    return ier;
                }
            }

            case UART_IIR_OFFSET: {
                synchronized (lock) {
                    final byte result = iir;
                    if ((iir & UART_IIR_ID_MASK) == UART_IIR_THRI) {
                        transmitInterruptPending = false;
                        updateInterrupts();
                    }
                    return result;
                }
            }

            case UART_LCR_OFFSET: {
                return lcr;
            }

            case UART_MCR_OFFSET: {
                return mcr;
            }

            case UART_LSR_OFFSET: {
                synchronized (lock) {
                    final byte result = lsr;
                    if ((lsr & (UART_LSR_BI | UART_LSR_OE)) != 0) {
                        lsr &= ~(UART_LSR_BI | UART_LSR_OE);
                        updateInterrupts();
                    }
                    return result;
                }
            }

            case UART_MSR_OFFSET: {
                if ((mcr & UART_MCR_LBM) != 0) {
                    // Loopback mode -> output = input
                    return (byte) ((mcr & 0b1100) << 4 | // OUT [3:2] -> [7:6]
                                   (mcr & 0b0010) << 3 | // RTS [1] -> [4]
                                   (mcr & 0b0001) << 5); // DTR [0] -> [5]
                } else {
                    synchronized (lock) {
                        final byte result = msr;
                        if ((msr & UART_MSR_DIRTY) != 0) {
                            msr &= ~UART_MSR_DIRTY;
                            updateInterrupts();
                        }
                        return result;
                    }
                }
            }

            case UART_SCR_OFFSET: {
                return scr;
            }
        }

        return 0;
    }

    @Override
    public void store(final int offset, final int value, final int sizeLog2) {
        assert sizeLog2 == Sizes.SIZE_8_LOG2;
        switch (offset) {
            // case UART_DLL_OFFSET:
            case UART_THR_OFFSET: {
                if ((lcr & UART_LCR_DLAB) != 0) { // UART_DLL
                    dl = (short) ((dl & 0xFF00) | (value & 0x00FF));
                } else { // UART_RBR
                    synchronized (lock) {
                        thr = (byte) value;
                        if ((fcr & UART_FCR_FE) != 0) {
                            if (transmitFifo.size() >= FIFO_QUEUE_CAPACITY) {
                                transmitFifo.dequeueByte();
                            }
                            transmitFifo.enqueue(thr);
                        }

                        transmitInterruptPending = false;
                        lsr &= ~(UART_LSR_THRE | UART_LSR_TEMT);

                        updateInterrupts();
                    }
                }
                break;
            }

            // case UART_DLM_OFFSET:
            case UART_IER_OFFSET: {
                if ((lcr & UART_LCR_DLAB) != 0) { // UART_DLM
                    dl = (short) ((value << 8) | (dl & 0x00FF));
                } else { // UART_IER
                    synchronized (lock) {
                        final int changes = ier ^ (byte) value;
                        ier = (byte) (value & 0b1111);

                        if ((changes & UART_IER_THRI) != 0) {
                            transmitInterruptPending = (ier & UART_IER_THRI) != 0 && (lsr & UART_LSR_THRE) != 0;
                        }

                        if (changes != 0) {
                            updateInterrupts();
                        }
                    }
                }
                break;
            }

            case UART_FCR_OFFSET: {
                synchronized (lock) {
                    final int changes = fcr ^ (byte) value;
                    final boolean forceClear = (changes & UART_FCR_FE) != 0;

                    if (forceClear || (value & UART_FCR_RFR) != 0) {
                        synchronized (receiveFifo) {
                            lsr &= ~(UART_LSR_DR | UART_LSR_BI);
                            timeoutInterruptPending = false;
                            receiveFifo.clear();
                        }
                    }
                    if (forceClear || (value & UART_FCR_XFR) != 0) {
                        synchronized (transmitFifo) {
                            lsr |= UART_LSR_THRE | UART_LSR_TEMT;
                            transmitInterruptPending = true;
                            transmitFifo.clear();
                        }
                    }

                    fcr = (byte) (value & (UART_FCR_FE | UART_FCR_DMS | UART_FCR_ITL_MASK));

                    if ((fcr & UART_FCR_FE) != 0) {
                        iir |= UART_IIR_FIFO_ENABLED;
                        switch (fcr & UART_FCR_ITL_MASK) {
                            case UART_FCR_ITL1: {
                                triggerLevel = 1;
                                break;
                            }
                            case UART_FCR_ITL2: {
                                triggerLevel = 4;
                                break;
                            }
                            case UART_FCR_ITL3: {
                                triggerLevel = 8;
                                break;
                            }
                            case UART_FCR_ITL4: {
                                triggerLevel = 14;
                                break;
                            }
                        }
                    } else {
                        iir &= ~UART_IIR_FIFO_ENABLED;
                    }

                    updateInterrupts();
                }
                break;
            }

            case UART_LCR_OFFSET: {
                lcr = (byte) value;
                break;
            }

            case UART_MCR_OFFSET: {
                mcr = (byte) (value & 0b11111);
                break;
            }

            case UART_SCR_OFFSET: {
                scr = (byte) value;
                break;
            }
        }
    }

    @Override
    public Iterable<Interrupt> getInterrupts() {
        return Collections.singleton(interrupt);
    }

    private void updateInterrupts() {
        final int niir;
        if ((ier & UART_IER_RLSI) != 0 && (lsr & UART_LSR_IRQ_MASK) != 0) {
            niir = UART_IIR_RLSI;
        } else if ((ier & UART_IER_RDI) != 0 && timeoutInterruptPending) {
            niir = UART_IIR_CTI;
        } else if ((ier & UART_IER_RDI) != 0 && (lsr & UART_LSR_DR) != 0 &&
                   ((fcr & UART_FCR_FE) == 0 || receiveFifo.size() > triggerLevel)) {
            niir = UART_IIR_RDI;
        } else if ((ier & UART_IER_THRI) != 0 && transmitInterruptPending) {
            niir = UART_IIR_THRI;
        } else if ((ier & UART_IER_MSI) != 0 && (msr & UART_MSR_DIRTY) != 0) {
            niir = UART_IIR_MSI;
        } else {
            niir = UART_IIR_NO_INTERRUPT;
        }

        iir = (byte) (niir | (iir & ~(UART_IIR_ID_MASK | UART_IIR_NO_INTERRUPT)));

        if ((iir & UART_IIR_NO_INTERRUPT) != 0) {
            interrupt.lowerInterrupt();
        } else {
            interrupt.raiseInterrupt();
        }
    }
}
