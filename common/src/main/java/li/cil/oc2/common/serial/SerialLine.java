/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.serial;

import li.cil.ceres.api.Serialized;
import li.cil.oc2.common.Constants;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.LongSupplier;

// Simulates serial behavior to some degree. In no way "realistic", but close enough for our
// purposes for now. Data accumulates for a full tick so we can use that window to determine
// whether the data written could theoretically have fit inside our baud rate. If this interface
// sent something in such an overloaded tick will then be flagged for a tx error as well.
@Serialized
@SuppressWarnings("NonAtomicOperationOnVolatileField") // We don't have competing writes to the volatile fields.
public final class SerialLine {
    static final int BITS_PER_BYTE = 10; // start bit, eight data bits, stop bit.
    private static final byte[] EMPTY = new byte[0];

    // --------------------------------------------------------------------- //

    private final transient BufferedSerialDevice port;
    private final transient LongSupplier gameTime;
    private final transient byte[] sourceMac;

    // The following block of fields is valid only for the current tick.
    private long currentTick = Long.MIN_VALUE;
    private int divisor;
    private int bytesPerTick;
    private int txBits;
    private int txBytes;
    private int rxBytes;
    private int rxSampledBytes;
    private boolean hasNoise;
    private byte[] rxData = EMPTY;

    private volatile int overrunCount;
    private volatile int rxErrorCount;
    private volatile int txErrorCount;
    private volatile int noiseCount;

    @Nullable
    private transient byte[] frameThisTick;

    // --------------------------------------------------------------------- //

    public SerialLine(final BufferedSerialDevice port, final LongSupplier gameTime, final byte[] sourceMac) {
        this.port = port;
        this.gameTime = gameTime;
        this.sourceMac = sourceMac;
    }

    // --------------------------------------------------------------------- //

    public int getOverrunCount() {
        return overrunCount;
    }

    public int getRxErrorCount() {
        return rxErrorCount;
    }

    public int getTxErrorCount() {
        return txErrorCount;
    }

    public int getNoiseCount() {
        return noiseCount;
    }

    // --------------------------------------------------------------------- //

    public long currentTick() {
        advance();
        return currentTick;
    }

    @Nullable
    public byte[] frameForTick() {
        advance();
        return frameThisTick;
    }

    public void writeEthernetFrame(final byte[] ethernetFrame) {
        final SerialFrame frame = SerialFrame.fromEthernetFrame(ethernetFrame);
        if (frame == null || Arrays.equals(frame.sourceMac(), sourceMac)) {
            return;
        }

        advance();

        final byte[] data = frame.data();
        rxBytes += data.length;

        final int sampled;
        if (frame.baudDivisor() != divisor) {
            hasNoise = true;
            sampled = Math.clamp((long) data.length * Math.max(1, frame.baudDivisor()) / Math.max(1, divisor), 1, SerialFrame.MAX_DATA_SIZE);
        } else {
            sampled = data.length;
            if (txBytes + rxBytes <= bytesPerTick) {
                final int offset = rxData.length;
                rxData = Arrays.copyOf(rxData, offset + data.length);
                System.arraycopy(data, 0, rxData, offset, data.length);
            }
        }
        rxSampledBytes = Math.min(rxSampledBytes + sampled, SerialFrame.MAX_DATA_SIZE);
    }

    // --------------------------------------------------------------------- //

    static int bytesPerTick(final int baudRate) {
        return (baudRate / Constants.SECONDS_TO_TICKS + BITS_PER_BYTE - 1) / BITS_PER_BYTE; // what one tick's bucket sends
    }

    // --------------------------------------------------------------------- //

    private void advance() {
        final long tick = gameTime.getAsLong();
        if (tick == currentTick) {
            return;
        }

        deliverReceived();

        final int baudRate = port.getBaudRate();
        divisor = port.getBaudDivisor();
        bytesPerTick = bytesPerTick(baudRate);

        final int bitsPerTick = baudRate / Constants.SECONDS_TO_TICKS;
        final int ceiling = bitsPerTick + BITS_PER_BYTE - 1; // one tick's worth, plus what is left of a byte
        if (currentTick == Long.MIN_VALUE || tick - currentTick > 1) {
            txBits = ceiling;
        } else {
            txBits = Math.min(txBits + bitsPerTick, ceiling);
        }

        currentTick = tick;

        frameThisTick = transmit();
    }

    @Nullable
    private byte[] transmit() {
        final int count = Math.min(txBits / BITS_PER_BYTE, SerialFrame.MAX_DATA_SIZE);
        if (count <= 0) {
            return null;
        }

        final byte[] data = port.poll(count);
        if (data == null) {
            return null;
        }
        txBits -= data.length * BITS_PER_BYTE;
        txBytes += data.length;

        return new SerialFrame(sourceMac, divisor, data).toEthernetFrame();
    }

    private void deliverReceived() {
        if (rxBytes > 0) {
            if (hasNoise || txBytes + rxBytes > bytesPerTick) {
                if (hasNoise) {
                    noiseCount++;
                } else {
                    rxErrorCount++;
                }
                if (txBytes > 0) {
                    txErrorCount++;
                }
                offerNoise(rxSampledBytes);
            } else {
                overrunCount += rxData.length - port.offer(rxData, false);
            }
        }

        txBytes = 0;
        rxBytes = 0;
        rxSampledBytes = 0;
        hasNoise = false;
        rxData = EMPTY;
    }

    private void offerNoise(final int length) {
        final byte[] noise = new byte[Math.min(length, bytesPerTick)];
        ThreadLocalRandom.current().nextBytes(noise);
        overrunCount += noise.length - port.offer(noise, true);
    }
}
