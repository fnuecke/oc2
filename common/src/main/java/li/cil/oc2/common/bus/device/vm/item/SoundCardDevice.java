/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.vm.item;

import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.vm.VMDevice;
import li.cil.oc2.api.bus.device.vm.VMDeviceLoadResult;
import li.cil.oc2.api.bus.device.vm.context.VMContext;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.bus.device.util.IdentityProxy;
import li.cil.oc2.common.bus.device.util.OptionalAddress;
import li.cil.oc2.common.bus.device.util.OptionalInterrupt;
import li.cil.oc2.common.network.SoundCardLoadBalancer;
import li.cil.oc2.common.serialization.NBTSerialization;
import li.cil.oc2.common.util.BlockLocation;
import li.cil.oc2.common.util.MuLaw;
import li.cil.oc2.common.util.NBTTagIds;
import li.cil.sedna.api.device.audio.AudioSink;
import li.cil.sedna.device.virtio.VirtIOSoundDevice;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public final class SoundCardDevice extends IdentityProxy<ItemStack> implements VMDevice, ItemDevice {
    public static final class Stream implements AudioSink {
        private static final AtomicInteger NEXT_ID = new AtomicInteger();

        private final int id = NEXT_ID.incrementAndGet();
        private final Supplier<Optional<BlockLocation>> location;
        private final byte[] buffer = new byte[BUFFER_SIZE];
        private int start;
        private int size;
        private int sampleRate;

        private Stream(final Supplier<Optional<BlockLocation>> location) {
            this.location = location;
        }

        public int getId() {
            return id;
        }

        public Optional<BlockLocation> getLocation() {
            return location.get();
        }

        public synchronized boolean hasAudio() {
            return size > 0;
        }

        @Nullable
        public synchronized Chunk poll() {
            if (size == 0) {
                return null;
            }

            final int count = Math.clamp(sampleRate * MAX_CHUNK_MILLIS / 1000, 1, size);
            start = (start + size - count) % buffer.length;

            final byte[] samples = new byte[count];
            final int head = Math.min(count, buffer.length - start);
            System.arraycopy(buffer, start, samples, 0, head);
            System.arraycopy(buffer, 0, samples, head, count - head);
            start = 0;
            size = 0;

            return new Chunk(sampleRate, samples);
        }

        @Override
        public synchronized void write(final ByteBuffer samples, final int sampleRate) {
            if (sampleRate != this.sampleRate) {
                // What is buffered was played at another rate and cannot share a packet with this.
                this.sampleRate = sampleRate;
                start = 0;
                size = 0;
            }

            for (int i = samples.position(); i + 1 < samples.limit(); i += 2) {
                if (size == buffer.length) {
                    start = (start + 1) % buffer.length;
                    size--;
                }
                buffer[(start + size) % buffer.length] = MuLaw.encode(samples.getShort(i));
                size++;
            }
        }

        private synchronized void clear() {
            start = 0;
            size = 0;
        }
    }

    public record Chunk(int sampleRate, byte[] samples) {
    }

    // --------------------------------------------------------------------- //

    public static final int AUDIBLE_DISTANCE = 32;
    private static final String DEVICE_TAG_NAME = "device";
    private static final String ADDRESS_TAG_NAME = "address";
    private static final String INTERRUPT_TAG_NAME = "interrupt";
    private static final int BUFFER_SIZE = 22050; // 1s of max fidelity
    private static final int MAX_CHUNK_MILLIS = 250; // max supported lag

    // --------------------------------------------------------------------- //

    private final Stream stream;
    @Nullable
    private VirtIOSoundDevice device;
    private final OptionalAddress address = new OptionalAddress();
    private final OptionalInterrupt interrupt = new OptionalInterrupt();
    @Nullable
    private CompoundTag deviceTag;

    // --------------------------------------------------------------------- //

    public SoundCardDevice(final ItemStack identity, final Supplier<Optional<BlockLocation>> location) {
        super(identity);
        this.stream = new Stream(location);
    }

    // --------------------------------------------------------------------- //

    public Stream getStream() {
        return stream;
    }

    @Override
    public VMDeviceLoadResult mount(final VMContext context) {
        device = new VirtIOSoundDevice(context.getMemoryMap(), stream, context.getClock());

        if (!address.claim(context.getDeviceRangeAllocator(), device)) {
            device = null;
            return VMDeviceLoadResult.fail()
                .withErrorMessage(Component.translatable(Constants.COMPUTER_ERROR_DEVICE_DOES_NOT_FIT));
        }

        if (interrupt.claim(context)) {
            device.getInterrupt().set(interrupt.getAsInt(), context.getInterruptController());
        } else {
            device = null;
            return VMDeviceLoadResult.fail();
        }

        if (deviceTag != null) {
            NBTSerialization.deserialize(deviceTag, device);
        }

        SoundCardLoadBalancer.add(stream);

        return VMDeviceLoadResult.success();
    }

    @Override
    public void unmount() {
        if (device != null) {
            deviceTag = NBTSerialization.serialize(device);
        }

        device = null;
        SoundCardLoadBalancer.remove(stream);
        stream.clear();
    }

    @Override
    public void dispose() {
        address.clear();
        interrupt.clear();
    }

    @Override
    public CompoundTag serializeNBT() {
        final CompoundTag tag = new CompoundTag();

        if (device != null) {
            deviceTag = NBTSerialization.serialize(device);
        }
        if (deviceTag != null) {
            tag.put(DEVICE_TAG_NAME, deviceTag);
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
        if (tag.contains(ADDRESS_TAG_NAME, NBTTagIds.TAG_LONG)) {
            address.set(tag.getLong(ADDRESS_TAG_NAME));
        }
        if (tag.contains(INTERRUPT_TAG_NAME, NBTTagIds.TAG_INT)) {
            interrupt.set(tag.getInt(INTERRUPT_TAG_NAME));
        }
    }
}
