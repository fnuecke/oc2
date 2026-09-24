/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.serial;

import li.cil.oc2.api.capabilities.NetworkInterface;
import li.cil.oc2.common.serialization.NBTSerialization;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;
import java.util.function.LongSupplier;

public final class SerialEndpoint {
    public static final int DEFAULT_BAUD_RATE = 9600;
    public static final int MIN_BAUD_RATE = 300;
    public static final int MAX_BAUD_RATE = 115200;

    private static final String PORT_TAG_NAME = "port";
    private static final String LINE_TAG_NAME = "line";
    private static final String ADDRESS_TAG_NAME = "address";
    private static final String BAUD_RATE_TAG_NAME = "baudRate";

    // --------------------------------------------------------------------- //

    private final LongSupplier gameTime;
    private final BufferedSerialDevice port = new BufferedSerialDevice();
    private final EndpointNetworkInterface networkInterface = new EndpointNetworkInterface();

    private int address = SerialFrame.getRandomAddress();
    private int baudRate = DEFAULT_BAUD_RATE;

    @Nullable
    private SerialLine line;
    @Nullable
    private CompoundTag lineTag;

    // --------------------------------------------------------------------- //

    public SerialEndpoint(final LongSupplier gameTime) {
        this.gameTime = gameTime;
        port.setBaudRate(baudRate);
    }

    // --------------------------------------------------------------------- //

    public NetworkInterface getNetworkInterface() {
        return networkInterface;
    }

    public int getAddress() {
        return address;
    }

    public int getBaudRate() {
        return baudRate;
    }

    public boolean setConfiguration(final int address, final int baudRate) {
        final int newAddress = Math.clamp(address, 0, SerialFrame.MAX_ADDRESS);
        final int newBaudRate = Math.clamp(baudRate, MIN_BAUD_RATE, MAX_BAUD_RATE);
        if (newAddress == this.address && newBaudRate == this.baudRate) {
            return false;
        }

        this.address = newAddress;
        this.baudRate = newBaudRate;

        port.setBaudRate(newBaudRate);
        line = null;
        lineTag = null;

        return true;
    }

    public long getLastReadTick() {
        return networkInterface.lastReadTick;
    }

    public boolean canSend() {
        return port.canSend();
    }

    public void send(final byte value) {
        port.send(value);
    }

    public int receive() {
        return port.receive();
    }

    public int getOverrunCount() {
        return line != null ? line.getOverrunCount() : 0;
    }

    public int getRxErrorCount() {
        return line != null ? line.getRxErrorCount() : 0;
    }

    public int getTxErrorCount() {
        return line != null ? line.getTxErrorCount() : 0;
    }

    public int getNoiseCount() {
        return line != null ? line.getNoiseCount() : 0;
    }

    // --------------------------------------------------------------------- //

    public void saveConfiguration(final CompoundTag tag) {
        tag.putInt(ADDRESS_TAG_NAME, address);
        tag.putInt(BAUD_RATE_TAG_NAME, baudRate);
    }

    public void loadConfiguration(final CompoundTag tag) {
        if (tag.contains(ADDRESS_TAG_NAME) && tag.contains(BAUD_RATE_TAG_NAME)) {
            setConfiguration(tag.getInt(ADDRESS_TAG_NAME), tag.getInt(BAUD_RATE_TAG_NAME));
        }
    }

    public void save(final CompoundTag tag) {
        tag.put(PORT_TAG_NAME, NBTSerialization.serialize(port));
        if (line != null) {
            tag.put(LINE_TAG_NAME, NBTSerialization.serialize(line));
        }
        saveConfiguration(tag);
    }

    public void load(final CompoundTag tag) {
        NBTSerialization.deserialize(tag.getCompound(PORT_TAG_NAME), port);
        address = tag.getInt(ADDRESS_TAG_NAME);
        baudRate = tag.getInt(BAUD_RATE_TAG_NAME);
        lineTag = tag.contains(LINE_TAG_NAME) ? tag.getCompound(LINE_TAG_NAME) : null;
    }

    // --------------------------------------------------------------------- //

    private SerialLine getLine() {
        SerialLine result = line;
        if (result == null) {
            result = new SerialLine(port, gameTime, SerialFrame.macOf(address));
            if (lineTag != null) {
                NBTSerialization.deserialize(lineTag, result);
                lineTag = null;
            }
            line = result;
        }

        return result;
    }

    // --------------------------------------------------------------------- //

    private final class EndpointNetworkInterface implements NetworkInterface {
        private long lastReadTick = Long.MIN_VALUE;

        @Nullable
        @Override
        public byte[] readEthernetFrame() {
            final SerialLine line = getLine();

            final long tick = line.currentTick();
            if (tick == lastReadTick) {
                return null;
            }

            lastReadTick = tick;
            return line.frameForTick();
        }

        @Override
        public void writeEthernetFrame(final NetworkInterface source, final byte[] frame, final int timeToLive) {
            getLine().writeEthernetFrame(frame);
        }
    }
}
