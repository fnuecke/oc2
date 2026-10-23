/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.serial;

import li.cil.oc2.api.capabilities.NetworkInterface;
import li.cil.oc2.common.serialization.NBTSerialization;
import li.cil.oc2.common.util.SerializationUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import javax.annotation.Nullable;
import java.util.function.LongSupplier;

public final class SerialEndpoint {
    public static final int DEFAULT_BAUD_RATE = 9600;
    public static final int MIN_BAUD_RATE = 300;
    public static final int MAX_BAUD_RATE = 115200;

    static final String ADDRESS_TAG_NAME = "address";
    static final String BAUD_RATE_TAG_NAME = "baudRate";

    private static final String PORT_TAG_NAME = "port";
    private static final String LINE_TAG_NAME = "line";

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

    public void saveConfiguration(final ValueOutput output) {
        output.putInt(ADDRESS_TAG_NAME, address);
        output.putInt(BAUD_RATE_TAG_NAME, baudRate);
    }

    public void save(final ValueOutput output) {
        output.store(PORT_TAG_NAME, CompoundTag.CODEC, NBTSerialization.serialize(port));
        if (line != null) {
            output.store(LINE_TAG_NAME, CompoundTag.CODEC, NBTSerialization.serialize(line));
        }
        saveConfiguration(output);
    }

    public void load(final ValueInput input) {
        NBTSerialization.deserialize(SerializationUtils.getCompoundOrEmpty(input, PORT_TAG_NAME), port);
        address = input.getIntOr(ADDRESS_TAG_NAME, 0);
        baudRate = input.getIntOr(BAUD_RATE_TAG_NAME, 0);
        lineTag = input.read(LINE_TAG_NAME, CompoundTag.CODEC).orElse(null);
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
