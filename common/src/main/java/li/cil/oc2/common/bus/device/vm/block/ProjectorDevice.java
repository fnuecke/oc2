/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.vm.block;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import li.cil.oc2.api.bus.device.vm.VMDevice;
import li.cil.oc2.api.bus.device.vm.VMDeviceLoadResult;
import li.cil.oc2.api.bus.device.vm.context.VMContext;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.bus.device.util.IdentityProxy;
import li.cil.oc2.common.bus.device.util.OptionalAddress;
import li.cil.oc2.common.serialization.BlobReference;
import li.cil.oc2.common.serialization.BlobStorage;
import li.cil.oc2.common.util.NBTTagIds;
import li.cil.oc2.common.vm.device.SimpleFramebufferDevice;
import li.cil.oc2.jcodec.common.model.Picture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public final class ProjectorDevice extends IdentityProxy<BlockEntity> implements VMDevice {
    private static final Logger LOGGER = LogManager.getLogger(ProjectorDevice.class);

    private static final String ADDRESS_TAG_NAME = "address";

    public static final int WIDTH = 640;
    public static final int HEIGHT = 480;

    private static final int FRAMEBUFFER_SIZE = WIDTH * HEIGHT * SimpleFramebufferDevice.STRIDE;

    // --------------------------------------------------------------------- //

    private final BooleanConsumer onMountedChanged;

    @Nullable
    private SimpleFramebufferDevice device;

    // --------------------------------------------------------------------- //

    private final OptionalAddress address = new OptionalAddress();
    private final BlobReference blob = new BlobReference();

    // --------------------------------------------------------------------- //

    public ProjectorDevice(final BlockEntity identity, final BooleanConsumer onMountedChanged) {
        super(identity);
        this.onMountedChanged = onMountedChanged;
    }

    // --------------------------------------------------------------------- //

    public boolean hasChanges() {
        final SimpleFramebufferDevice framebufferDevice = device;
        return framebufferDevice != null && framebufferDevice.hasChanges();
    }

    public boolean applyChanges(final Picture picture) {
        final SimpleFramebufferDevice framebufferDevice = device;
        return framebufferDevice != null && framebufferDevice.applyChanges(picture);
    }

    @Override
    public VMDeviceLoadResult mount(final VMContext context) {
        if (!allocateDevice(context)) {
            return VMDeviceLoadResult.fail();
        }

        assert device != null;
        if (!address.claim(context.getDeviceRangeAllocator(), device)) {
            releaseDevice();
            return VMDeviceLoadResult.fail()
                .withErrorMessage(Component.translatable(Constants.COMPUTER_ERROR_DEVICE_DOES_NOT_FIT));
        }

        onMountedChanged.accept(true);

        return VMDeviceLoadResult.success();
    }

    @Override
    public void unmount() {
        releaseDevice();
        onMountedChanged.accept(false);
    }

    @Override
    public void dispose() {
        blob.delete();

        address.clear();
    }

    @Override
    public CompoundTag serializeNBT() {
        final CompoundTag tag = new CompoundTag();

        blob.writeTo(tag);
        if (address.isPresent()) {
            tag.putLong(ADDRESS_TAG_NAME, address.getAsLong());
        }

        return tag;
    }

    @Override
    public void deserializeNBT(final CompoundTag tag) {
        blob.readFrom(tag);
        if (tag.contains(ADDRESS_TAG_NAME, NBTTagIds.TAG_LONG)) {
            address.set(tag.getLong(ADDRESS_TAG_NAME));
        }
    }

    // --------------------------------------------------------------------- //

    private boolean allocateDevice(final VMContext context) {
        if (!context.getMemoryAllocator().claimMemory(Constants.PAGE_SIZE + FRAMEBUFFER_SIZE)) {
            return false;
        }

        try {
            device = createFrameBufferDevice();
        } catch (final IOException e) {
            blob.close();
            return false;
        }

        return true;
    }

    private void releaseDevice() {
        final SimpleFramebufferDevice framebufferDevice = device;
        device = null;
        if (framebufferDevice != null) {
            framebufferDevice.close();
        }

        blob.close();
    }

    private SimpleFramebufferDevice createFrameBufferDevice() throws IOException {
        if (blob.isStale()) {
            LOGGER.error("Discarding stale projector framebuffer data [{}].", blob.getHandle());
            blob.delete();
        }

        FileChannel channel;
        try {
            channel = blob.open();
        } catch (final BlobStorage.BlobMissingException | BlobStorage.BlobInUseException e) {
            blob.release();
            channel = blob.open();
        }

        final MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, FRAMEBUFFER_SIZE);
        return new SimpleFramebufferDevice(WIDTH, HEIGHT, buffer);
    }
}
