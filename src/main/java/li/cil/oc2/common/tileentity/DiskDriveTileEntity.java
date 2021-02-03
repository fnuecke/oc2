package li.cil.oc2.common.tileentity;

import li.cil.oc2.api.bus.device.data.BlockDeviceData;
import li.cil.oc2.api.bus.device.vm.VMContext;
import li.cil.oc2.api.bus.device.vm.VMDevice;
import li.cil.oc2.api.bus.device.vm.VMDeviceLoadResult;
import li.cil.oc2.common.Config;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.block.DiskDriveBlock;
import li.cil.oc2.common.bus.device.item.AbstractBlockDeviceVMDevice;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.container.TypedItemStackHandler;
import li.cil.oc2.common.item.AbstractBlockDeviceItem;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.DiskDriveFloppyMessage;
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
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.util.Optional;

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
            final Direction facing = getBlockState().get(DiskDriveBlock.HORIZONTAL_FACING);
            ItemStackUtils.spawnAsEntity(getWorld(), getPos().offset(facing), stack, facing);
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
        tag.put(Constants.INVENTORY_TAG_NAME, itemHandler.serializeNBT());
        return tag;
    }

    @Override
    public void handleUpdateTag(final BlockState state, final CompoundNBT tag) {
        super.handleUpdateTag(state, tag);
        itemHandler.deserializeNBT(tag.getCompound(Constants.INVENTORY_TAG_NAME));
    }

    @Override
    public CompoundNBT write(CompoundNBT tag) {
        tag = super.write(tag);

        tag.put(Constants.INVENTORY_TAG_NAME, itemHandler.serializeNBT());

        return tag;
    }

    @Override
    public void read(final BlockState state, final CompoundNBT tag) {
        super.read(state, tag);

        itemHandler.deserializeNBT(tag.getCompound(Constants.INVENTORY_TAG_NAME));
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
        public ItemStack getStackInSlot(final int slot) {
            final ItemStack stack = getStackInSlotRaw(slot);
            exportDeviceDataToItemStack(stack);
            return stack;
        }

        @Override
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

            final World world = getWorld();
            if (world == null || world.isRemote()) {
                return;
            }

            final ItemStack stack = getStackInSlotRaw(slot);
            if (stack.isEmpty()) {
                device.removeBlockDevice();
            } else {
                final CompoundNBT tag = ItemStackUtils.getOrCreateModDataTag(stack).getCompound(DATA_TAG_NAME);
                device.updateBlockDevice(tag);
            }

            Network.sendToClientsTrackingChunk(new DiskDriveFloppyMessage(DiskDriveTileEntity.this), world.getChunkAt(getPos()));
        }

        private void exportDeviceDataToItemStack(final ItemStack stack) {
            if (stack.isEmpty()) {
                return;
            }

            final World world = getWorld();
            if (world == null || world.isRemote()) {
                return;
            }

            device.serializeData();

            final CompoundNBT tag = new CompoundNBT();
            device.exportToItemStack(tag);
            ItemStackUtils.getOrCreateModDataTag(stack).put(DATA_TAG_NAME, tag);
        }
    }

    private final class DiskDriveVMDevice extends AbstractBlockDeviceVMDevice<BlockDevice, TileEntity> {
        private VMContext context;

        public DiskDriveVMDevice() {
            super(DiskDriveTileEntity.this);
        }

        public void updateBlockDevice(final CompoundNBT tag) {
            blobHandle = null;
            importFromItemStack(tag);

            if (data != null) {
                data = createBlockDevice();
                deserializeData();

                assert device != null;
                try {
                    device.setBlock(data);
                } catch (final IOException e) {
                    LOGGER.error(e);
                }
            }
        }

        public void removeBlockDevice() {
            updateBlockDevice(new CompoundNBT());
        }

        @Override
        public VMDeviceLoadResult load(final VMContext context) {
            this.context = context;
            return super.load(context);
        }

        @Override
        protected int getSize() {
            return Config.maxFloppySize;
        }

        @Override
        protected BlockDevice createBlockDevice() {
            final ItemStack stack = itemHandler.getStackInSlotRaw(0);
            if (stack.isEmpty()) {
                return EMPTY_BLOCK_DEVICE;
            }

            // NB: I don't see any reason why people would want to add custom floppy items,
            //     since the existing ones can be customized by registering block device data
            //     with the BlockDeviceData registry and then just create our floppy item with
            //     the associated tag. If there should turn out to be a use case, we'll need
            //     to have the item return its data via a capability or so, so others don't
            //     have to extend the non-api BlockDeviceItem class.

            final BlockDeviceData data = AbstractBlockDeviceItem.getData(stack);
            if (data != null) {
                return new FakeReadonlyBlockDevice(data.getBlockDevice());
            }

            final int capacity = AbstractBlockDeviceItem.getCapacity(stack);
            if (capacity <= 0) {
                return EMPTY_BLOCK_DEVICE;
            }

            return ByteBufferBlockDevice.create(Math.min(capacity, Config.maxFloppySize), false);
        }

        @Override
        protected Optional<InputStream> getSerializationStream(final BlockDevice device) {
            if (context != null) {
                context.joinWorkerThread();
            }

            if (device.isReadonly()) {
                return Optional.empty();
            } else {
                return Optional.of(device.getInputStream());
            }
        }

        @Override
        protected OutputStream getDeserializationStream(final BlockDevice device) {
            if (context != null) {
                context.joinWorkerThread();
            }

            return device.getOutputStream();
        }

        @Override
        protected void handleDataAccess() {
            accessSoundEmitter.play();
        }
    }

    // We cannot change the read-only flag of the underlying VirtIO device after
    // initial setup (config change does not trigger feature renegotiation). So
    // we have to always pretend to be writable and just error when something
    // tries to write to a read-only file system.
    private static final class FakeReadonlyBlockDevice implements BlockDevice {
        private final BlockDevice device;

        private FakeReadonlyBlockDevice(final BlockDevice device) {
            this.device = device;
        }

        @Override
        public boolean isReadonly() {
            return false;
        }

        @Override
        public long getCapacity() {
            return device.getCapacity();
        }

        @Override
        public InputStream getInputStream(final long offset) {
            return device.getInputStream(offset);
        }

        @Override
        public OutputStream getOutputStream(final long offset) {
            throw new UnsupportedOperationException();
        }
    }
}
