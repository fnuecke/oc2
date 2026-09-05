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
import java.util.UUID;

public class FlashStorageDevice extends IdentityProxy<ItemStack> implements VMDevice, ItemDevice, FirmwareLoader {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final String BLOB_HANDLE_TAG_NAME = "blob";

    // --------------------------------------------------------------------- //

    protected final int size;

    @Nullable
    private ByteBuffer data;

    // Offline persisted data.
    @Nullable
    protected UUID blobHandle;

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

        if (BlobStorage.isStaleHandle(blobHandle) && !StorageItemUtils.acceptStaleData(identity)) {
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

        if (blobHandle != null) {
            BlobStorage.close(blobHandle);
        }
    }

    @Override
    public void exportToItemStack(final CompoundTag nbt) {
        if (blobHandle != null) {
            nbt.putUUID(BLOB_HANDLE_TAG_NAME, blobHandle);
        }
    }

    @Override
    public void importFromItemStack(final CompoundTag nbt) {
        if (nbt.hasUUID(BLOB_HANDLE_TAG_NAME)) {
            blobHandle = nbt.getUUID(BLOB_HANDLE_TAG_NAME);
        }
    }

    @Override
    public CompoundTag serializeNBT() {
        final CompoundTag tag = new CompoundTag();

        if (blobHandle != null) {
            tag.putUUID(BLOB_HANDLE_TAG_NAME, blobHandle);
        }

        return tag;
    }

    @Override
    public void deserializeNBT(final CompoundTag tag) {
        if (tag.hasUUID(BLOB_HANDLE_TAG_NAME)) {
            blobHandle = tag.getUUID(BLOB_HANDLE_TAG_NAME);
        }
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
        if (!BlobStorage.isValidHandle(blobHandle)) {
            return null;
        }

        return read(blobHandle, false);
    }

    protected final ByteBuffer read(final UUID handle, final boolean createIfMissing) throws IOException {
        final FileChannel channel = BlobStorage.open(handle, createIfMissing);
        try {
            final ByteBuffer buffer = ByteBuffer.allocate(size);
            while (buffer.hasRemaining()) {
                if (channel.read(buffer) < 0) {
                    break;
                }
            }
            return buffer;
        } catch (final IOException e) {
            BlobStorage.close(handle);
            throw e;
        }
    }

    public static void unmount(final CompoundTag tag) {
        if (tag.hasUUID(BLOB_HANDLE_TAG_NAME)) {
            BlobStorage.close(tag.getUUID(BLOB_HANDLE_TAG_NAME));
        }
    }

    private void flag(final State state) {
        StorageItemUtils.setState(identity, state);
    }
}
