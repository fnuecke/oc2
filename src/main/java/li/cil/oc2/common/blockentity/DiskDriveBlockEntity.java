package li.cil.oc2.common.blockentity;

import li.cil.oc2.api.bus.device.vm.VMDevice;
import li.cil.oc2.common.Config;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.block.DiskDriveBlock;
import li.cil.oc2.common.bus.device.item.AbstractBlockDeviceVMDevice;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.container.TypedItemStackHandler;
import li.cil.oc2.common.item.FloppyItem;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.DiskDriveFloppyMessage;
import li.cil.oc2.common.serialization.BlobStorage;
import li.cil.oc2.common.tags.ItemTags;
import li.cil.oc2.common.util.ItemStackUtils;
import li.cil.oc2.common.util.LocationSupplierUtils;
import li.cil.oc2.common.util.SoundEvents;
import li.cil.oc2.common.util.ThrottledSoundEmitter;
import li.cil.sedna.api.device.BlockDevice;
import li.cil.sedna.device.block.ByteBufferBlockDevice;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public final class DiskDriveBlockEntity extends ModBlockEntity {
    private static final String DATA_TAG_NAME = "data";

    private static final ByteBufferBlockDevice EMPTY_BLOCK_DEVICE = ByteBufferBlockDevice.create(0, false);

    ///////////////////////////////////////////////////////////////////

    private final DiskDriveItemStackHandler itemHandler = new DiskDriveItemStackHandler();
    private final DiskDriveVMDevice device = new DiskDriveVMDevice();
    private final ThrottledSoundEmitter accessSoundEmitter;
    private final ThrottledSoundEmitter insertSoundEmitter;
    private final ThrottledSoundEmitter ejectSoundEmitter;

    ///////////////////////////////////////////////////////////////////

    public DiskDriveBlockEntity(final BlockPos pos, final BlockState state) {
        super(BlockEntities.DISK_DRIVE.get(), pos, state);

        this.accessSoundEmitter = new ThrottledSoundEmitter(LocationSupplierUtils.of(this),
            SoundEvents.FLOPPY_ACCESS.get()).withMinInterval(Duration.ofSeconds(1));
        this.insertSoundEmitter = new ThrottledSoundEmitter(LocationSupplierUtils.of(this),
            SoundEvents.FLOPPY_INSERT.get()).withMinInterval(Duration.ofMillis(100));
        this.ejectSoundEmitter = new ThrottledSoundEmitter(LocationSupplierUtils.of(this),
            SoundEvents.FLOPPY_EJECT.get()).withMinInterval(Duration.ofMillis(100));
    }

    ///////////////////////////////////////////////////////////////////

    public VMDevice getDevice() {
        return device;
    }

    public boolean canInsert(final ItemStack stack) {
        return !stack.isEmpty() && ItemTags.DEVICES_FLOPPY.contains(stack.getItem());
    }

    public ItemStack insert(final ItemStack stack, @Nullable final Player player) {
        if (stack.isEmpty() || !ItemTags.DEVICES_FLOPPY.contains(stack.getItem())) {
            return stack;
        }

        eject(player);

        insertSoundEmitter.play();
        return itemHandler.insertItem(0, stack, false);
    }

    public boolean canEject() {
        return !itemHandler.extractItem(0, 1, true).isEmpty();
    }

    public void eject(@Nullable final Player player) {
        if (level == null) {
            return;
        }

        final ItemStack stack = itemHandler.extractItem(0, 1, false);
        if (!stack.isEmpty()) {
            final Direction facing = getBlockState().getValue(DiskDriveBlock.FACING);
            ejectSoundEmitter.play();
            ItemStackUtils.spawnAsEntity(level, getBlockPos().relative(facing), stack, facing).ifPresent(entity -> {
                if (player != null) {
                    entity.setNoPickUpDelay();
                    entity.setOwner(player.getUUID());
                }
            });
        }
    }

    public ItemStack getFloppy() {
        return itemHandler.getStackInSlot(0);
    }

    @OnlyIn(Dist.CLIENT)
    public void setFloppyClient(final ItemStack stack) {
        itemHandler.setStackInSlot(0, stack);
    }

    @Override
    protected void collectCapabilities(final CapabilityCollector collector, @Nullable final Direction direction) {
        collector.offer(Capabilities.ITEM_HANDLER, itemHandler);
    }

    @Override
    public CompoundTag getUpdateTag() {
        final CompoundTag tag = super.getUpdateTag();
        tag.put(Constants.ITEMS_TAG_NAME, itemHandler.serializeNBT());
        return tag;
    }

    @Override
    public void handleUpdateTag(final CompoundTag tag) {
        super.handleUpdateTag(tag);
        itemHandler.deserializeNBT(tag.getCompound(Constants.ITEMS_TAG_NAME));
    }

    @Override
    protected void saveAdditional(final CompoundTag tag) {
        super.saveAdditional(tag);

        tag.put(Constants.ITEMS_TAG_NAME, itemHandler.serializeNBT());
    }

    @Override
    public void load(final CompoundTag tag) {
        super.load(tag);

        itemHandler.deserializeNBT(tag.getCompound(Constants.ITEMS_TAG_NAME));
    }

    ///////////////////////////////////////////////////////////////////

    private final class DiskDriveItemStackHandler extends TypedItemStackHandler {
        public DiskDriveItemStackHandler() {
            super(1, ItemTags.DEVICES_FLOPPY);
        }

        public ItemStack getStackInSlotRaw(final int slot) {
            return super.getStackInSlot(slot);
        }

        @Override
        @Nonnull
        public ItemStack getStackInSlot(final int slot) {
            final ItemStack stack = getStackInSlotRaw(slot);
            exportDeviceDataToItemStack(stack);
            return stack;
        }

        @Override
        @Nonnull
        public ItemStack extractItem(final int slot, final int amount, final boolean simulate) {
            if (slot == 0 && !simulate && amount > 0) {
                exportDeviceDataToItemStack(getStackInSlotRaw(0));
            }

            return super.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(final int slot) {
            return 1;
        }

        @Override
        public CompoundTag serializeNBT() {
            exportDeviceDataToItemStack(getStackInSlotRaw(0));
            return super.serializeNBT();
        }

        @Override
        protected void onContentsChanged(final int slot) {
            super.onContentsChanged(slot);

            if (level == null || level.isClientSide()) {
                return;
            }

            final ItemStack stack = getStackInSlotRaw(slot);
            if (stack.isEmpty()) {
                device.removeBlockDevice();
            } else {
                final CompoundTag tag = ItemStackUtils.getOrCreateModDataTag(stack).getCompound(DATA_TAG_NAME);
                device.updateBlockDevice(tag);
            }

            Network.sendToClientsTrackingBlockEntity(new DiskDriveFloppyMessage(DiskDriveBlockEntity.this), DiskDriveBlockEntity.this);

            setChanged();
        }

        private void exportDeviceDataToItemStack(final ItemStack stack) {
            if (stack.isEmpty()) {
                return;
            }

            if (level == null || level.isClientSide()) {
                return;
            }

            final CompoundTag tag = new CompoundTag();
            device.exportToItemStack(tag);
            ItemStackUtils.getOrCreateModDataTag(stack).put(DATA_TAG_NAME, tag);
        }
    }

    private final class DiskDriveVMDevice extends AbstractBlockDeviceVMDevice<BlockDevice, BlockEntity> {
        public DiskDriveVMDevice() {
            super(DiskDriveBlockEntity.this);
        }

        public void updateBlockDevice(final CompoundTag tag) {
            joinOpenJob();

            if (device == null) {
                return;
            }

            try {
                device.setBlock(EMPTY_BLOCK_DEVICE);
            } catch (final IOException e) {
                LOGGER.error(e);
            }

            if (blobHandle != null) {
                BlobStorage.close(blobHandle);
                blobHandle = null;
            }

            importFromItemStack(tag);

            setOpenJob(createBlockDevice().thenAcceptAsync(blockDevice -> {
                try {
                    device.setBlock(blockDevice);
                } catch (final IOException e) {
                    throw new RuntimeException(e);
                }
            }, WORKERS));
        }

        public void removeBlockDevice() {
            joinOpenJob();

            if (device == null) {
                return;
            }

            try {
                device.setBlock(EMPTY_BLOCK_DEVICE);
            } catch (final IOException e) {
                LOGGER.error(e);
            }

            if (blobHandle != null) {
                BlobStorage.close(blobHandle);
                blobHandle = null;
            }
        }

        @Override
        protected CompletableFuture<BlockDevice> createBlockDevice() {
            final ItemStack stack = itemHandler.getStackInSlotRaw(0);
            if (stack.isEmpty() || !(stack.getItem() instanceof final FloppyItem floppy)) {
                return CompletableFuture.completedFuture(EMPTY_BLOCK_DEVICE);
            }

            final int capacity = Mth.clamp(floppy.getCapacity(stack), 0, Config.maxFloppySize);
            if (capacity <= 0) {
                return CompletableFuture.completedFuture(EMPTY_BLOCK_DEVICE);
            }

            blobHandle = BlobStorage.validateHandle(blobHandle);
            return CompletableFuture.supplyAsync(() -> {
                try {
                    final FileChannel channel = BlobStorage.getOrOpen(blobHandle);
                    final MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, capacity);
                    return ByteBufferBlockDevice.wrap(buffer, false);
                } catch (final IOException e) {
                    throw new RuntimeException(e);
                }
            }, WORKERS);
        }

        @Override
        protected void handleDataAccess() {
            accessSoundEmitter.play();
        }
    }
}
