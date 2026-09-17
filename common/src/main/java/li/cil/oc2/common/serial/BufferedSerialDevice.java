/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.serial;

import it.unimi.dsi.fastutil.bytes.ByteArrayFIFOQueue;
import it.unimi.dsi.fastutil.shorts.ShortArrayFIFOQueue;
import li.cil.ceres.api.Serialized;
import li.cil.oc2.common.Constants;
import li.cil.sedna.api.Interrupt;
import li.cil.sedna.api.device.InterruptSource;
import li.cil.sedna.api.device.MemoryMappedDevice;
import li.cil.sedna.api.device.Resettable;
import li.cil.sedna.api.device.Steppable;
import li.cil.sedna.device.serial.UART16550A;

import javax.annotation.Nullable;

// Wrapper for UART with a larger buffer because we only pump on the server thread tick.
@Serialized
public final class BufferedSerialDevice implements MemoryMappedDevice, Steppable, Resettable, InterruptSource {
    public static final int PORT_COUNT = 8;

    private static final int RX_BACKLOG_TICKS = 2;
    private static final int TX_BACKLOG_TICKS = 2;
    private static final int GARBLED = 0x100;

    // --------------------------------------------------------------------- //

    private final UART16550A uart = new UART16550A();

    private final transient Object lock = new Object();
    private final ShortArrayFIFOQueue rxQueue = new ShortArrayFIFOQueue(256);
    private final ByteArrayFIFOQueue txQueue = new ByteArrayFIFOQueue(256);

    // --------------------------------------------------------------------- //

    public Interrupt getInterrupt() {
        return uart.getInterrupt();
    }

    public int getBaudRate() {
        return uart.getBaudRate();
    }

    public int getBaudDivisor() {
        return uart.getBaudDivisor();
    }

    public int offer(final byte[] values, final boolean garbled) {
        synchronized (lock) {
            final int count = Math.clamp(RX_BACKLOG_TICKS * SerialLine.bytesPerTick(uart.getBaudRate()) - rxQueue.size(), 0, values.length);
            final int flag = garbled ? GARBLED : 0;
            for (int i = 0; i < count; i++) {
                rxQueue.enqueue((short) ((values[i] & 0xFF) | flag));
            }
            return count;
        }
    }

    @Nullable
    public byte[] poll(final int count) {
        synchronized (lock) {
            final int available = Math.min(count, txQueue.size());
            if (available <= 0) {
                return null;
            }

            final byte[] result = new byte[available];
            for (int i = 0; i < available; i++) {
                result[i] = txQueue.dequeueByte();
            }
            return result;
        }
    }

    // --------------------------------------------------------------------- //

    @Override
    public void step(final int cycles) {
        synchronized (lock) {
            while (!rxQueue.isEmpty() && uart.canPutByte()) {
                final int value = rxQueue.dequeueShort() & 0xFFFF;
                if ((value & GARBLED) != 0) {
                    uart.putFrameError((byte) value);
                } else {
                    uart.putByte((byte) value);
                }
            }

            final int capacity = Math.max(1, TX_BACKLOG_TICKS * uart.getBaudRate() / (Constants.SECONDS_TO_TICKS * SerialLine.BITS_PER_BYTE));
            int value;
            while (txQueue.size() < capacity && (value = uart.read()) >= 0) {
                txQueue.enqueue((byte) value);
            }
        }

        uart.step(cycles);
    }

    @Override
    public void reset() {
        synchronized (lock) {
            rxQueue.clear();
            txQueue.clear();
        }

        uart.reset();
    }

    @Override
    public Iterable<Interrupt> getInterrupts() {
        return uart.getInterrupts();
    }

    @Override
    public int getLength() {
        return uart.getLength();
    }

    @Override
    public int getSupportedSizes() {
        return uart.getSupportedSizes();
    }

    @Override
    public long load(final int offset, final int sizeLog2) {
        return uart.load(offset, sizeLog2);
    }

    @Override
    public void store(final int offset, final long value, final int sizeLog2) {
        uart.store(offset, value, sizeLog2);
    }
}
