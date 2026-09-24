/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.serial;

import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import li.cil.oc2.api.capabilities.NetworkInterface;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;
import java.util.function.LongSupplier;

// For integrations that need a free floating endpoint. See ComputerCraft and TIS-3D integrations.
public final class SerialPort implements NetworkInterface {
    private static final int RECEIVE_BACKLOG_TICKS = 10;

    // --------------------------------------------------------------------- //

    private final SerialEndpoint endpoint;
    private final Object lock = new Object();
    private final ShortArrayList received = new ShortArrayList();
    private int overrunCount;
    @Nullable
    private volatile Runnable listener;

    // --------------------------------------------------------------------- //

    public SerialPort(final LongSupplier gameTime) {
        endpoint = new SerialEndpoint(gameTime);
    }

    // --------------------------------------------------------------------- //

    public int getAddress() {
        return endpoint.getAddress();
    }

    public int getBaudRate() {
        return endpoint.getBaudRate();
    }

    public void setConfiguration(final int address, final int baudRate) {
        endpoint.setConfiguration(address, baudRate);
    }

    public void saveConfiguration(final CompoundTag tag) {
        endpoint.saveConfiguration(tag);
    }

    public void loadConfiguration(final CompoundTag tag) {
        endpoint.loadConfiguration(tag);
    }

    public int getOverrunCount() {
        return endpoint.getOverrunCount() + overrunCount;
    }

    public int getRxErrorCount() {
        return endpoint.getRxErrorCount();
    }

    public int getTxErrorCount() {
        return endpoint.getTxErrorCount();
    }

    public int getNoiseCount() {
        return endpoint.getNoiseCount();
    }

    public void setListener(@Nullable final Runnable listener) {
        this.listener = listener;
    }

    // --------------------------------------------------------------------- //

    public boolean canWrite() {
        return endpoint.canSend();
    }

    public int write(final byte[] data) {
        int count = 0;
        while (count < data.length && endpoint.canSend()) {
            endpoint.send(data[count++]);
        }
        return count;
    }

    public int available() {
        synchronized (lock) {
            return received.size();
        }
    }

    public int peek() {
        synchronized (lock) {
            return received.isEmpty() ? -1 : received.getShort(0) & 0xFFFF;
        }
    }

    public void skip() {
        synchronized (lock) {
            if (!received.isEmpty()) {
                received.removeShort(0);
            }
        }
    }

    public short[] read(final int count) {
        synchronized (lock) {
            final short[] values = new short[Math.clamp(count, 0, received.size())];
            received.getElements(0, values, 0, values.length);
            received.removeElements(0, values.length);
            return values;
        }
    }

    public void clear() {
        synchronized (lock) {
            received.clear();
        }
    }

    // --------------------------------------------------------------------- //

    @Nullable
    @Override
    public byte[] readEthernetFrame() {
        final byte[] frame = endpoint.getNetworkInterface().readEthernetFrame();
        receive();
        return frame;
    }

    @Override
    public void writeEthernetFrame(final NetworkInterface source, final byte[] frame, final int timeToLive) {
        endpoint.getNetworkInterface().writeEthernetFrame(source, frame, timeToLive);
    }

    // --------------------------------------------------------------------- //

    private int receiveCapacity() {
        return Math.max(SerialFrame.MAX_DATA_SIZE, RECEIVE_BACKLOG_TICKS * SerialLine.bytesPerTick(endpoint.getBaudRate()));
    }

    private void receive() {
        boolean hasNewData = false;
        final int capacity = receiveCapacity();
        synchronized (lock) {
            int value;
            while ((value = endpoint.receive()) >= 0) {
                if (received.size() < capacity) {
                    received.add((short) value);
                    hasNewData = true;
                } else {
                    overrunCount++;
                }
            }
        }

        if (hasNewData) {
            final Runnable listener = this.listener;
            if (listener != null) {
                listener.run();
            }
        }
    }
}
