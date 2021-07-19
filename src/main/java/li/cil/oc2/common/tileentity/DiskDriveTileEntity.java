package li.cil.oc2.common.tileentity;

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
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.time.Duration;

public final class DiskDriveTileEntity extends AbstractTileEntity {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final String DATA_TAG_NAME = "data";

    private static final ByteBufferBlockDevice EMPTY_BLOCK_DEVICE = ByteBufferBlockDevice.create(0, false);

    ///////////////////////////////////////////////////////////////////

    private final DiskDriveItemStackHandler itemHandler = new DiskDriveItemStackHandler();
    private final DiskDriveVMDevice device = new DiskDriveVMDevice();
    private final ThrottledSoundEmitter accessSoundEmitter;
    private final ThrottledSoundEmitter insertSoundEmitter;
    private final ThrottledSoundEmitter ejectSoundEmitter;

    ///////////////////////////////////////////////////////////////////

    public DiskDriveTileEntity() {
        super(TileEntities.DISK_DRIVE_TILE_ENTITY.get());

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

    public ItemStack insert(final ItemStack stack) {
        if (stack.isEmpty() || !ItemTags.DEVICES_FLOPPY.contains(stack.getItem())) {
            return stack;
        }

        eject();

        if (!stack.isEmpty()) {
            insertSoundEmitter.play();
        }

        return itemHandler.insertItem(0, stack, false);
    }

    public void eject() {
        final ItemStack stack = itemHandler.extractItem(0, 1, false);
        if (!stack.isEmpty()) {
            final Direction facing = getBlockState().getValue(DiskDriveBlock.FACING);
            ItemStackUtils.spawnAsEntity(level, getBlockPos().relative(facing), stack, facing);
            ejectSoundEmitter.play();
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
    public CompoundNBT getUpdateTag() {
        final CompoundNBT tag = super.getUpdateTag();
        tag.put(Constants.ITEMS_TAG_NAME, itemHandler.serializeNBT());
        return tag;
    }

    @Override
    public void handleUpdateTag(final BlockState state, final CompoundNBT tag) {
        super.handleUpdateTag(state, tag);
        itemHandler.deserializeNBT(tag.getCompound(Constants.ITEMS_TAG_NAME));
    }

    @Override
    public CompoundNBT save(CompoundNBT tag) {
        tag = super.save(tag);

        tag.put(Constants.ITEMS_TAG_NAME, itemHandler.serializeNBT());

        return tag;
    }

    @Override
    public void load(final BlockState state, final CompoundNBT tag) {
        super.load(state, tag);

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
        protected void onContentsChanged(final int slot) {
            super.onContentsChanged(slot);

            if (level == null || level.isClientSide) {
                return;
            }

            final ItemStack stack = getStackInSlotRaw(slot);
            if (stack.isEmpty()) {
                device.removeBlockDevice();
            } else {
                final CompoundNBT tag = ItemStackUtils.getOrCreateModDataTag(stack).getCompound(DATA_TAG_NAME);
                device.updateBlockDevice(tag);
            }

            Network.sendToClientsTrackingTileEntity(new DiskDriveFloppyMessage(DiskDriveTileEntity.this), DiskDriveTileEntity.this);
        }

        private void exportDeviceDataToItemStack(final ItemStack stack) {
            if (stack.isEmpty()) {
                return;
            }

            if (level == null || level.isClientSide) {
                return;
            }

            final CompoundNBT tag = new CompoundNBT();
            device.exportToItemStack(tag);
            ItemStackUtils.getOrCreateModDataTag(stack).put(DATA_TAG_NAME, tag);
        }
    }

    private final class DiskDriveVMDevice extends AbstractBlockDeviceVMDevice<BlockDevice, TileEntity> {
        public DiskDriveVMDevice() {
            super(DiskDriveTileEntity.this);
        }

        public void updateBlockDevice(final CompoundNBT tag) {
            if (device == null) {
                return;
            }

            if (blobHandle != null) {
                BlobStorage.close(blobHandle);
                blobHandle = null;
            }

            importFromItemStack(tag);

            try {
                device.setBlock(createBlockDevice());
            } catch (final IOException e) {
                LOGGER.error(e);
            }
        }

        public void removeBlockDevice() {
            if (device == null) {
                return;
            }

            if (blobHandle != null) {
                BlobStorage.close(blobHandle);
                blobHandle = null;
            }

            try {
                device.setBlock(EMPTY_BLOCK_DEVICE);
            } catch (final IOException e) {
                LOGGER.error(e);
            }
        }

        @Override
        protected BlockDevice createBlockDevice() throws IOException {
            final ItemStack stack = itemHandler.getStackInSlotRaw(0);
            if (stack.isEmpty() || !(stack.getItem() instanceof FloppyItem)) {
                return EMPTY_BLOCK_DEVICE;
            }

            final FloppyItem item = (FloppyItem) stack.getItem();
            final int capacity = MathHelper.clamp(item.getCapacity(stack), 0, Config.maxFloppySize);
            if (capacity <= 0) {
                return EMPTY_BLOCK_DEVICE;
            }

            blobHandle = BlobStorage.validateHandle(blobHandle);
            final FileChannel channel = BlobStorage.getOrOpen(blobHandle);
            final MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, capacity);
            return ByteBufferBlockDevice.wrap(buffer, false);
        }

        @Override
        protected void handleDataAccess() {
            accessSoundEmitter.play();
        }
    }
}
