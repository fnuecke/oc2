/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.vm.item;

import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.io.IOCallback;
import li.cil.oc2.api.bus.device.io.IODevice;
import li.cil.oc2.api.bus.device.io.IODeviceDescription;
import li.cil.oc2.api.bus.device.io.IOOutputStream;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.RPCDeviceDescription;
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
import li.cil.oc2.common.serial.SerialFrame;
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
import java.util.function.LongSupplier;
import java.util.function.Supplier;

@RPCDeviceDescription(typeNames = {"serial"}, description = """
    Provided by the [serial interface card](../item/serial_interface_card.md).

    The serial port itself is driven through the operating system, see the card's page. This device reports the card's configuration and error counters, for example to tell serial ports apart or to detect collisions.

    Counters reset when the computer starts. Device order here may differ from the order the cards were installed in, so match them up by `getBaseAddress()`.""")
@IODeviceDescription(name = "SERIAL", description = """
    Counters are clamped to two bytes. Values wider than a byte are low byte first.

    Device order here may differ from the order the cards were installed in, so match them up by `getBaseAddress`.""")
public final class SerialInterfaceCardDevice extends AbstractItemRPCDevice implements VMDevice, ItemDevice, CapabilityProvider, RPCDevice, IODevice {
    private static final String DEVICE_TAG_NAME = "device";
    private static final String LINE_TAG_NAME = "line";
    private static final String ADDRESS_TAG_NAME = "address";
    private static final String INTERRUPT_TAG_NAME = "interrupt";

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
        super(identity);
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
            SerialInterfaceCardItem.setAddress(identity, SerialFrame.getRandomAddress());
        }

        cardAddress = SerialInterfaceCardItem.getAddress(identity);
        final SerialLine newLine = new SerialLine(device, gameTime.get(), SerialFrame.macOf(cardAddress));
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

    @Callback(synchronize = false,
        description = "Gets the address this card is set to. The hardware ignores it; software may use it to address endpoints on a shared segment.",
        returnValueDescription = "the address, in [0, 255].")
    public int getAddress() {
        return cardAddress;
    }

    @Callback(synchronize = false,
        description = "Gets the baud rate the serial port is configured to. Every endpoint on a segment must be set to the same rate.",
        returnValueDescription = "the baud rate in bits per second, or `0` while the computer is off.")
    public int getBaudRate() {
        return device != null ? device.getBaudRate() : 0;
    }

    @Callback(synchronize = false,
        description = "Gets how many received bytes were lost because this computer did not read them in time.",
        returnValueDescription = "the number of bytes lost since the computer started.")
    public int getOverrunCount() {
        return line != null ? line.getOverrunCount() : 0;
    }

    @Callback(synchronize = false,
        description = "Gets how often received data was corrupted because several endpoints sent at the same time. These are the collisions the receiver sees.",
        returnValueDescription = "the number of receiver collisions since the computer started.")
    public int getRxErrorCount() {
        return line != null ? line.getRxErrorCount() : 0;
    }

    @Callback(synchronize = false,
        description = "Gets how often sent data was corrupted because several endpoints sent at the same time. These are the collisions the sender sees. " +
            "Read it before sending and again once the port finished sending: a change means the send failed.",
        returnValueDescription = "the number of sender collisions since the computer started.")
    public int getTxErrorCount() {
        return line != null ? line.getTxErrorCount() : 0;
    }

    @Callback(synchronize = false,
        description = "Gets how often traffic arrived at a baud rate this port is not set to, including this computer's own port being set to a different rate.",
        returnValueDescription = "the number of data errors since the computer started.")
    public int getNoiseCount() {
        return line != null ? line.getNoiseCount() : 0;
    }

    @Callback(synchronize = false,
        description = "Gets where this card's serial port registers are mapped, to tell which port belongs to which card when there is more than one.",
        returnValueDescription = "the memory address on RISC-V, the I/O port on the Z80.")
    public long getBaseAddress() {
        return address.isPresent() ? address.getAsLong() : 0;
    }

    // --------------------------------------------------------------------- //

    @IOCallback(value = GET_ADDRESS_CODE, synchronize = false, name = "getAddress",
        description = "Reads the address this card is set to.",
        resultsDescription = "one byte, the address.")
    public void getAddressIO(final IOOutputStream results) throws IOException {
        results.writeU8(getAddress());
    }

    @IOCallback(value = GET_BAUD_RATE_CODE, synchronize = false, name = "getBaudRate",
        description = "Reads the baud rate the serial port is configured to.",
        resultsDescription = "four bytes, the baud rate in bits per second.")
    public void getBaudRateIO(final IOOutputStream results) throws IOException {
        results.writeU32(getBaudRate());
    }

    @IOCallback(value = GET_OVERRUN_COUNT_CODE, synchronize = false, name = "getOverrunCount",
        description = "Reads how many received bytes were lost because this computer did not read them in time.",
        resultsDescription = "two bytes, the count.")
    public void getOverrunCountIO(final IOOutputStream results) throws IOException {
        results.writeU16(clampToU16(getOverrunCount()));
    }

    @IOCallback(value = GET_RX_ERROR_COUNT_CODE, synchronize = false, name = "getRxErrorCount",
        description = "Reads how often received data was corrupted by other endpoints sending at the same time.",
        resultsDescription = "two bytes, the count.")
    public void getRxErrorCountIO(final IOOutputStream results) throws IOException {
        results.writeU16(clampToU16(getRxErrorCount()));
    }

    @IOCallback(value = GET_NOISE_COUNT_CODE, synchronize = false, name = "getNoiseCount",
        description = "Reads how often traffic arrived at a baud rate this port is not set to.",
        resultsDescription = "two bytes, the count.")
    public void getNoiseCountIO(final IOOutputStream results) throws IOException {
        results.writeU16(clampToU16(getNoiseCount()));
    }

    @IOCallback(value = GET_TX_ERROR_COUNT_CODE, synchronize = false, name = "getTxErrorCount",
        description = "Reads how often sent data was corrupted by other endpoints sending at the same time. A change after a send means it failed.",
        resultsDescription = "two bytes, the count.")
    public void getTxErrorCountIO(final IOOutputStream results) throws IOException {
        results.writeU16(clampToU16(getTxErrorCount()));
    }

    @IOCallback(value = GET_BASE_ADDRESS_CODE, synchronize = false, name = "getBaseAddress",
        description = "Reads the I/O port this card's serial port registers start at, to tell cards apart.",
        resultsDescription = "one byte, the port.")
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
