/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.vm.item;

import com.google.common.eventbus.Subscribe;
import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.vm.FirmwareLoader;
import li.cil.oc2.api.bus.device.vm.VMDevice;
import li.cil.oc2.api.bus.device.vm.VMDeviceLoadResult;
import li.cil.oc2.api.bus.device.vm.context.VMContext;
import li.cil.oc2.api.bus.device.vm.event.VMInitializationException;
import li.cil.oc2.api.bus.device.vm.event.VMInitializingEvent;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.bus.device.util.IdentityProxy;
import li.cil.oc2.common.serialization.BlobReference;
import li.cil.oc2.common.serialization.BlobStorage;
import li.cil.oc2.common.util.StorageItemUtils;
import li.cil.oc2.common.util.StorageItemUtils.State;
import li.cil.sedna.api.memory.MemoryAccessException;
import li.cil.sedna.memory.MemoryMaps;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class FlashStorageDevice extends IdentityProxy<ItemStack> implements VMDevice, ItemDevice, FirmwareLoader {
    private static final Logger LOGGER = LogManager.getLogger(FlashStorageDevice.class);

    // --------------------------------------------------------------------- //

    protected final int size;

    @Nullable
    private ByteBuffer data;

    // Offline persisted data.
    protected final BlobReference blob = new BlobReference();

    // --------------------------------------------------------------------- //

    public FlashStorageDevice(final ItemStack identity, final int size) {
        super(identity);
        this.size = size;
    }

    // --------------------------------------------------------------------- //

    @Override
    public VMDeviceLoadResult mount(final VMContext context) {
        if (!context.getMemoryAllocator().claimMemory(size)) {
            return VMDeviceLoadResult.fail();
        }

        if (blob.isStale() && !StorageItemUtils.acceptStaleData(identity)) {
            return VMDeviceLoadResult.fail()
                .withErrorMessage(Component.translatable(Constants.COMPUTER_ERROR_STORAGE_INCONSISTENT))
                .asPermanent();
        }

        try {
            data = readData();
        } catch (final BlobStorage.BlobStorageFullException e) {
            LOGGER.error(e);
            return VMDeviceLoadResult.fail()
                .withErrorMessage(Component.translatable(Constants.COMPUTER_ERROR_STORAGE_FULL));
        } catch (final BlobStorage.BlobMissingException | BlobStorage.BlobInUseException e) {
            flag(State.CORRUPTED);
            return VMDeviceLoadResult.fail()
                .withErrorMessage(Component.translatable(Constants.COMPUTER_ERROR_STORAGE_CORRUPTED))
                .asPermanent();
        } catch (final IOException e) {
            LOGGER.error(e);
            return VMDeviceLoadResult.fail();
        }

        context.getEventBus().register(this);

        return VMDeviceLoadResult.success();
    }

    @Override
    public void unmount() {
        data = null;
        blob.close();
    }

    @Override
    public void exportToItemStack(final CompoundTag nbt) {
        blob.writeTo(nbt);
    }

    @Override
    public void importFromItemStack(final CompoundTag nbt) {
        blob.readFrom(nbt);
    }

    @Override
    public CompoundTag serializeNBT() {
        final CompoundTag tag = new CompoundTag();
        blob.writeTo(tag);
        return tag;
    }

    @Override
    public void deserializeNBT(final CompoundTag tag) {
        blob.readFrom(tag);
    }

    @Subscribe
    public void handleInitializingEvent(final VMInitializingEvent event) {
        if (data == null) {
            return; // Never programmed; the machine starts on zeroes and goes nowhere, as before.
        }

        data.clear();

        try {
            MemoryMaps.store(event.memory(), event.programStartAddress(), data);
        } catch (final MemoryAccessException e) {
            throw new VMInitializationException(Component.translatable(Constants.COMPUTER_ERROR_INSUFFICIENT_MEMORY));
        }
    }

    // --------------------------------------------------------------------- //

    @Nullable
    protected ByteBuffer readData() throws IOException {
        if (!blob.isValid()) {
            return null;
        }

        final FileChannel channel = blob.open();
        try {
            final ByteBuffer buffer = ByteBuffer.allocate(size);
            while (buffer.hasRemaining()) {
                if (channel.read(buffer) < 0) {
                    break;
                }
            }
            return buffer;
        } catch (final IOException e) {
            blob.close();
            throw e;
        }
    }

    private void flag(final State state) {
        StorageItemUtils.setState(identity, state);
    }
}
