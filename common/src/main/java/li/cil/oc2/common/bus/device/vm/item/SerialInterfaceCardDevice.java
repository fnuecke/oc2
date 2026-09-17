/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.vm.item;

import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.io.IOCallback;
import li.cil.oc2.api.bus.device.io.IODevice;
import li.cil.oc2.api.bus.device.io.IOName;
import li.cil.oc2.api.bus.device.io.IOOutputStream;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.DocumentedDevice;
import li.cil.oc2.api.bus.device.rpc.RPCDevice;
import li.cil.oc2.api.bus.device.vm.VMDevice;
import li.cil.oc2.api.bus.device.vm.VMDeviceLoadResult;
import li.cil.oc2.api.bus.device.vm.context.VMContext;
import li.cil.oc2.api.capabilities.NetworkInterface;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.bus.device.rpc.item.AbstractItemRPCDevice;
import li.cil.oc2.common.bus.device.util.OptionalAddress;
import li.cil.oc2.common.bus.device.util.OptionalInterrupt;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.capabilities.CapabilityProvider;
import li.cil.oc2.common.capabilities.CapabilityType;
import li.cil.oc2.common.item.SerialInterfaceCardItem;
import li.cil.oc2.common.serial.BufferedSerialDevice;
import li.cil.oc2.common.serial.SerialLine;
import li.cil.oc2.common.serialization.NBTSerialization;
import li.cil.oc2.common.util.BlockLocation;
import li.cil.oc2.common.util.LevelUtils;
import li.cil.oc2.common.util.NBTTagIds;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

@IOName("SERIAL")
public final class SerialInterfaceCardDevice extends AbstractItemRPCDevice implements VMDevice, ItemDevice, CapabilityProvider, RPCDevice, IODevice, DocumentedDevice {
    private static final String DEVICE_TAG_NAME = "device";
    private static final String LINE_TAG_NAME = "line";
    private static final String ADDRESS_TAG_NAME = "address";
    private static final String INTERRUPT_TAG_NAME = "interrupt";

    private static final String GET_ADDRESS = "getAddress";
    private static final String GET_BAUD_RATE = "getBaudRate";
    private static final String GET_OVERRUN_COUNT = "getOverrunCount";
    private static final String GET_RX_ERROR_COUNT = "getRxErrorCount";
    private static final String GET_TX_ERROR_COUNT = "getTxErrorCount";
    private static final String GET_NOISE_COUNT = "getNoiseCount";
    private static final String GET_BASE_ADDRESS = "getBaseAddress";

    private static final int GET_ADDRESS_CODE = 1;
    private static final int GET_BAUD_RATE_CODE = 2;
    private static final int GET_OVERRUN_COUNT_CODE = 3;
    private static final int GET_RX_ERROR_COUNT_CODE = 4;
    private static final int GET_TX_ERROR_COUNT_CODE = 5;
    private static final int GET_NOISE_COUNT_CODE = 6;
    private static final int GET_BASE_ADDRESS_CODE = 7;

    // --------------------------------------------------------------------- //

    private final Supplier<Optional<BlockLocation>> location;
    private final NetworkInterface[] networkInterfaces = new NetworkInterface[Constants.BLOCK_FACE_COUNT];

    private final OptionalAddress address = new OptionalAddress();
    private final OptionalInterrupt interrupt = new OptionalInterrupt();
    private CompoundTag deviceTag;
    private CompoundTag lineTag;

    @Nullable
    private BufferedSerialDevice device;
    @Nullable
    private SerialLine line;
    private volatile int cardAddress;

    // --------------------------------------------------------------------- //

    public SerialInterfaceCardDevice(final ItemStack identity, final Supplier<Optional<BlockLocation>> location) {
        super(identity, "serial");
        this.location = location;

        for (int i = 0; i < networkInterfaces.length; i++) {
            networkInterfaces[i] = new SideInterface();
        }
    }

    // --------------------------------------------------------------------- //

    @Nullable
    @SuppressWarnings("unchecked")
    @Override
    public <T> T getCapability(final CapabilityType<T> capability, @Nullable final Direction side) {
        if (capability == Capabilities.NETWORK_INTERFACE && SerialInterfaceCardItem.getSideConfiguration(identity, side)) {
            return (T) networkInterfaces[side.get3DDataValue()];
        }

        return null;
    }

    @Override
    public VMDeviceLoadResult mount(final VMContext context) {
        final Optional<LongSupplier> gameTime = location.get()
            .flatMap(BlockLocation::tryGetLevel)
            .map(level -> LevelUtils.gameTimeSupplier((Level) level));
        if (gameTime.isEmpty()) {
            return VMDeviceLoadResult.fail();
        }

        final BufferedSerialDevice device = new BufferedSerialDevice();
        if (!address.claim(context.getDeviceRangeAllocator(), device)) {
            return VMDeviceLoadResult.fail()
                .withErrorMessage(Component.translatable(Constants.COMPUTER_ERROR_DEVICE_DOES_NOT_FIT));
        }

        if (interrupt.claim(context)) {
            device.getInterrupt().set(interrupt.getAsInt(), context.getInterruptController());
        } else {
            return VMDeviceLoadResult.fail();
        }

        if (deviceTag != null) {
            NBTSerialization.deserialize(deviceTag, device);
        }

        if (!SerialInterfaceCardItem.hasAddress(identity)) {
            SerialInterfaceCardItem.setAddress(identity, ThreadLocalRandom.current().nextInt(SerialInterfaceCardItem.MAX_ADDRESS + 1));
        }

        cardAddress = SerialInterfaceCardItem.getAddress(identity);
        final SerialLine newLine = new SerialLine(device, gameTime.get(), macOf(cardAddress));
        if (lineTag != null) {
            NBTSerialization.deserialize(lineTag, newLine);
        }

        this.device = device;
        line = newLine;

        return VMDeviceLoadResult.success();
    }

    @Override
    public void unmount() {
        captureState();
        device = null;
        line = null;
    }

    @Override
    public void dispose() {
        deviceTag = null;
        lineTag = null;
        device = null;
        line = null;

        address.clear();
        interrupt.clear();
    }

    @Override
    public CompoundTag serializeNBT() {
        final CompoundTag tag = new CompoundTag();

        captureState();
        if (deviceTag != null) {
            tag.put(DEVICE_TAG_NAME, deviceTag);
        }
        if (lineTag != null) {
            tag.put(LINE_TAG_NAME, lineTag);
        }
        if (address.isPresent()) {
            tag.putLong(ADDRESS_TAG_NAME, address.getAsLong());
        }
        if (interrupt.isPresent()) {
            tag.putInt(INTERRUPT_TAG_NAME, interrupt.getAsInt());
        }

        return tag;
    }

    @Override
    public void deserializeNBT(final CompoundTag tag) {
        if (tag.contains(DEVICE_TAG_NAME, NBTTagIds.TAG_COMPOUND)) {
            deviceTag = tag.getCompound(DEVICE_TAG_NAME);
        }
        if (tag.contains(LINE_TAG_NAME, NBTTagIds.TAG_COMPOUND)) {
            lineTag = tag.getCompound(LINE_TAG_NAME);
        }
        if (tag.contains(ADDRESS_TAG_NAME, NBTTagIds.TAG_LONG)) {
            address.set(tag.getLong(ADDRESS_TAG_NAME));
        }
        if (tag.contains(INTERRUPT_TAG_NAME, NBTTagIds.TAG_INT)) {
            interrupt.set(tag.getInt(INTERRUPT_TAG_NAME));
        }
    }

    // --------------------------------------------------------------------- //

    @Callback(name = GET_ADDRESS, synchronize = false)
    public int getAddress() {
        return cardAddress;
    }

    @Callback(name = GET_BAUD_RATE, synchronize = false)
    public int getBaudRate() {
        return device != null ? device.getBaudRate() : 0;
    }

    @Callback(name = GET_OVERRUN_COUNT, synchronize = false)
    public int getOverrunCount() {
        return line != null ? line.getOverrunCount() : 0;
    }

    @Callback(name = GET_RX_ERROR_COUNT, synchronize = false)
    public int getRxErrorCount() {
        return line != null ? line.getRxErrorCount() : 0;
    }

    @Callback(name = GET_TX_ERROR_COUNT, synchronize = false)
    public int getTxErrorCount() {
        return line != null ? line.getTxErrorCount() : 0;
    }

    @Callback(name = GET_NOISE_COUNT, synchronize = false)
    public int getNoiseCount() {
        return line != null ? line.getNoiseCount() : 0;
    }

    @Callback(name = GET_BASE_ADDRESS, synchronize = false)
    public long getBaseAddress() {
        return address.isPresent() ? address.getAsLong() : 0;
    }

    @Override
    public void getDeviceDocumentation(final DeviceVisitor visitor) {
        visitor.visitCallback(GET_ADDRESS)
            .description("Get the address this interface is set to.\n" +
                "This allows software to filter for packets. It is purely for convenience. The " +
                "interface itself does not use this value. Use it to address other interfaces.")
            .returnValueDescription("the address, in [0, 255].");
        visitor.visitCallback(GET_BAUD_RATE)
            .description("Get the baud rate the interface is configured with.\n" +
                "Every interface on a segment must be configured to match, as must the software side.")
            .returnValueDescription("the baud rate in bits per second.");
        visitor.visitCallback(GET_OVERRUN_COUNT)
            .description("Get how many received bytes were lost because this machine did not " +
                "read them in time.")
            .returnValueDescription("the number of bytes lost since the last reset.");
        visitor.visitCallback(GET_RX_ERROR_COUNT)
            .description("Get how often values received by this machine were corrupted because " +
                "the segment was overloaded. These are receiver visible collisions.")
            .returnValueDescription("the number of receiver collisions since the last reset.");
        visitor.visitCallback(GET_TX_ERROR_COUNT)
            .description("Get how often values sent by this machine were corrupted because " +
                "the segment got overloaded while writing. These are sender visible collisions. Check before " +
                "sending and after waiting a bit, so the port finished sending: a change means the send failed.")
            .returnValueDescription("the number of sender collisions since the last reset.");
        visitor.visitCallback(GET_NOISE_COUNT)
            .description("Get how often traffic arrived at a line rate this card is not set to, " +
                "including this machine's own serial port being set to a different rate.")
            .returnValueDescription("the number of observed data errors since the last reset.");
        visitor.visitCallback(GET_BASE_ADDRESS)
            .description("Get where this interface's serial port registers are mapped, to tell which port " +
                "belongs to which interface when there is more than one.")
            .returnValueDescription("the memory address on RISC-V, the I/O port on the Z80.");
    }

    // --------------------------------------------------------------------- //

    @IOCallback(value = GET_ADDRESS_CODE, synchronize = false)
    public void getAddressIO(final IOOutputStream results) throws IOException {
        results.writeU8(getAddress());
    }

    @IOCallback(value = GET_BAUD_RATE_CODE, synchronize = false)
    public void getBaudRateIO(final IOOutputStream results) throws IOException {
        results.writeU32(getBaudRate());
    }

    @IOCallback(value = GET_OVERRUN_COUNT_CODE, synchronize = false)
    public void getOverrunCountIO(final IOOutputStream results) throws IOException {
        results.writeU16(clampToU16(getOverrunCount()));
    }

    @IOCallback(value = GET_RX_ERROR_COUNT_CODE, synchronize = false)
    public void getRxErrorCountIO(final IOOutputStream results) throws IOException {
        results.writeU16(clampToU16(getRxErrorCount()));
    }

    @IOCallback(value = GET_NOISE_COUNT_CODE, synchronize = false)
    public void getNoiseCountIO(final IOOutputStream results) throws IOException {
        results.writeU16(clampToU16(getNoiseCount()));
    }

    @IOCallback(value = GET_TX_ERROR_COUNT_CODE, synchronize = false)
    public void getTxErrorCountIO(final IOOutputStream results) throws IOException {
        results.writeU16(clampToU16(getTxErrorCount()));
    }

    @IOCallback(value = GET_BASE_ADDRESS_CODE, synchronize = false)
    public void getBaseAddressIO(final IOOutputStream results) throws IOException {
        results.writeU8((int) getBaseAddress());
    }

    // --------------------------------------------------------------------- //

    private void captureState() {
        if (device != null) {
            deviceTag = NBTSerialization.serialize(device);
        }
        if (line != null) {
            lineTag = NBTSerialization.serialize(line);
        }
    }

    private static int clampToU16(final int value) {
        return Math.min(value, 0xFFFF);
    }

    private static byte[] macOf(final int address) {
        return new byte[]{0x02, 0x6F, 0x63, 0, 0, (byte) address}; // locally administered, unicast
    }

    // --------------------------------------------------------------------- //

    private final class SideInterface implements NetworkInterface {
        private long servedTick = Long.MIN_VALUE;

        @Nullable
        @Override
        public byte[] readEthernetFrame() {
            if (line == null) {
                return null;
            }

            final long tick = line.currentTick();
            if (tick == servedTick) {
                return null;
            }

            servedTick = tick;
            return line.frameForTick();
        }

        @Override
        public void writeEthernetFrame(final NetworkInterface source, final byte[] frame, final int timeToLive) {
            if (line != null) {
                line.writeEthernetFrame(frame);
            }
        }
    }
}
