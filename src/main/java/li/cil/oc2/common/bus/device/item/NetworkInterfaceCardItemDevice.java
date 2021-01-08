package li.cil.oc2.common.bus.device.item;

import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.capabilities.NetworkInterface;
import li.cil.oc2.api.bus.device.vm.*;
import li.cil.oc2.common.bus.device.util.IdentityProxy;
import li.cil.oc2.common.bus.device.util.OptionalAddress;
import li.cil.oc2.common.bus.device.util.OptionalInterrupt;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.serialization.NBTSerialization;
import li.cil.oc2.common.util.NBTTagIds;
import li.cil.sedna.device.virtio.VirtIONetworkDevice;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class NetworkInterfaceCardItemDevice extends IdentityProxy<ItemStack> implements VMDevice, VMDeviceLifecycleListener, ItemDevice, ICapabilityProvider {
    private static final String DEVICE_TAG_NAME = "device";
    private static final String ADDRESS_NBT_TAG_NAME = "address";
    private static final String INTERRUPT_NBT_TAG_NAME = "interrupt";

    ///////////////////////////////////////////////////////////////

    private VirtIONetworkDevice device;
    private final NetworkInterface networkInterface = new NetworkInterfaceImpl();
    private boolean isRunning;

    private final OptionalAddress address = new OptionalAddress();
    private final OptionalInterrupt interrupt = new OptionalInterrupt();
    private CompoundNBT deviceNbt;

    ///////////////////////////////////////////////////////////////

    public NetworkInterfaceCardItemDevice(final ItemStack identity) {
        super(identity);
    }

    ///////////////////////////////////////////////////////////////

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull final Capability<T> cap, @Nullable final Direction side) {
        if (cap == Capabilities.NETWORK_INTERFACE && side != null) {
            return LazyOptional.of(() -> networkInterface).cast();
        }

        return LazyOptional.empty();
    }

    @Override
    public VMDeviceLoadResult load(final VMContext context) {
        device = new VirtIONetworkDevice(context.getMemoryMap());

        if (!address.claim(context, device)) {
            return VMDeviceLoadResult.fail();
        }

        if (interrupt.claim(context)) {
            device.getInterrupt().set(interrupt.getAsInt(), context.getInterruptController());
        } else {
            return VMDeviceLoadResult.fail();
        }

        if (deviceNbt != null) {
            NBTSerialization.deserialize(deviceNbt, device);
        }

        return VMDeviceLoadResult.success();
    }

    @Override
    public void handleLifecycleEvent(final VMDeviceLifecycleEventType event) {
        switch (event) {
            case RESUMED_RUNNING:
                isRunning = true;
                break;
            case UNLOAD:
                unload();
                break;
        }
    }

    @Override
    public CompoundNBT serializeNBT() {
        final CompoundNBT tag = new CompoundNBT();

        if (device != null) {
            deviceNbt = NBTSerialization.serialize(device);
        }
        if (deviceNbt != null) {
            tag.put(DEVICE_TAG_NAME, deviceNbt);
        }
        if (address.isPresent()) {
            tag.putLong(ADDRESS_NBT_TAG_NAME, address.getAsLong());
        }
        if (interrupt.isPresent()) {
            tag.putInt(INTERRUPT_NBT_TAG_NAME, interrupt.getAsInt());
        }

        return tag;
    }

    @Override
    public void deserializeNBT(final CompoundNBT tag) {
        if (tag.contains(DEVICE_TAG_NAME, NBTTagIds.TAG_COMPOUND)) {
            deviceNbt = tag.getCompound(DEVICE_TAG_NAME);
        }
        if (tag.contains(ADDRESS_NBT_TAG_NAME, NBTTagIds.TAG_LONG)) {
            address.set(tag.getLong(ADDRESS_NBT_TAG_NAME));
        }
        if (tag.contains(INTERRUPT_NBT_TAG_NAME, NBTTagIds.TAG_INT)) {
            interrupt.set(tag.getInt(INTERRUPT_NBT_TAG_NAME));
        }
    }

    ///////////////////////////////////////////////////////////////

    private void unload() {
        device = null;
        isRunning = false;
        address.clear();
        interrupt.clear();
    }

    ///////////////////////////////////////////////////////////////

    private final class NetworkInterfaceImpl implements NetworkInterface {
        @Override
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
