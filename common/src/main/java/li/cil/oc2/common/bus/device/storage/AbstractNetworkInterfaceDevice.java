/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.storage;

import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.vm.VMDevice;
import li.cil.oc2.api.bus.device.vm.VMDeviceLoadResult;
import li.cil.oc2.api.bus.device.vm.context.VMContext;
import li.cil.oc2.api.capabilities.NetworkInterface;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.bus.device.util.IdentityProxy;
import li.cil.oc2.common.bus.device.util.OptionalAddress;
import li.cil.oc2.common.bus.device.util.OptionalInterrupt;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.capabilities.CapabilityProvider;
import li.cil.oc2.common.capabilities.CapabilityType;
import li.cil.oc2.common.serialization.NBTSerialization;
import li.cil.sedna.device.virtio.VirtIONetworkDevice;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import javax.annotation.Nullable;

public abstract class AbstractNetworkInterfaceDevice extends IdentityProxy<ItemStack> implements VMDevice, ItemDevice, CapabilityProvider {
    private static final String DEVICE_TAG_NAME = "device";
    private static final String ADDRESS_TAG_NAME = "address";
    private static final String INTERRUPT_TAG_NAME = "interrupt";

    // --------------------------------------------------------------------- //

    private VirtIONetworkDevice device;
    private final NetworkInterface networkInterface = new NetworkInterfaceImpl();
    private boolean isRunning;

    private final OptionalAddress address = new OptionalAddress();
    private final OptionalInterrupt interrupt = new OptionalInterrupt();
    private CompoundTag deviceTag;

    // --------------------------------------------------------------------- //

    protected AbstractNetworkInterfaceDevice(final ItemStack identity) {
        super(identity);
    }

    // --------------------------------------------------------------------- //

    @Nullable
    @SuppressWarnings("unchecked")
    @Override
    public <T> T getCapability(final CapabilityType<T> capability, @Nullable final Direction side) {
        if (capability == Capabilities.NETWORK_INTERFACE) {
            return (T) networkInterface;
        }

        return null;
    }

    @Override
    public VMDeviceLoadResult mount(final VMContext context) {
        device = new VirtIONetworkDevice(context.getMemoryMap(), Constants.VIRTIO_NETWORK_QUEUE_SIZE);

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

        context.getEventBus().register(this);

        isRunning = true;

        return VMDeviceLoadResult.success();
    }

    @Override
    public void unmount() {
        if (device != null) {
            deviceTag = NBTSerialization.serialize(device);
        }

        device = null;
        isRunning = false;
    }

    @Override
    public void dispose() {
        address.clear();
        interrupt.clear();
    }

    @Override
    public void serialize(final ValueOutput output) {
        if (device != null) {
            deviceTag = NBTSerialization.serialize(device);
        }
        if (deviceTag != null) {
            output.store(DEVICE_TAG_NAME, CompoundTag.CODEC, deviceTag);
        }
        if (address.isPresent()) {
            output.putLong(ADDRESS_TAG_NAME, address.getAsLong());
        }
        if (interrupt.isPresent()) {
            output.putInt(INTERRUPT_TAG_NAME, interrupt.getAsInt());
        }
    }

    @Override
    public void deserialize(final ValueInput input) {
        input.read(DEVICE_TAG_NAME, CompoundTag.CODEC).ifPresent(tag -> deviceTag = tag);
        input.getLong(ADDRESS_TAG_NAME).ifPresent(address::set);
        input.getInt(INTERRUPT_TAG_NAME).ifPresent(interrupt::set);
    }

    // --------------------------------------------------------------------- //

    protected NetworkInterface getNetworkInterface() {
        return networkInterface;
    }

    // --------------------------------------------------------------------- //

    private final class NetworkInterfaceImpl implements NetworkInterface {
        @Override
        @Nullable
        public byte[] readEthernetFrame() {
            if (device != null && isRunning) {
                return device.readEthernetFrame();
            } else {
                return null;
            }
        }

        @Override
        public void writeEthernetFrame(final NetworkInterface source, final byte[] frame, final int timeToLive) {
            if (device != null && isRunning) {
                device.writeEthernetFrame(frame);
            }
        }
    }
}
