/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.blockentity;

import li.cil.oc2.common.Constants;
import li.cil.oc2.common.block.DiskDriveBlock;
import li.cil.oc2.common.bus.device.vm.block.DiskDriveContainer;
import li.cil.oc2.common.bus.device.vm.block.DiskDriveDevice;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.container.TypedItemStackHandler;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.DiskDriveFloppyMessage;
import li.cil.oc2.common.tags.ItemTags;
import li.cil.oc2.common.util.ItemStackUtils;
import li.cil.oc2.common.util.LocationSupplierUtils;
import li.cil.oc2.common.util.SoundEvents;
import li.cil.oc2.common.util.ThrottledSoundEmitter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;

public final class DiskDriveBlockEntity extends ModBlockEntity implements DiskDriveContainer {
    private static final String DATA_TAG_NAME = "data";

    ///////////////////////////////////////////////////////////////////

    private final DiskDriveItemStackHandler itemHandler = new DiskDriveItemStackHandler();
    private final DiskDriveDevice<DiskDriveBlockEntity> device = new DiskDriveDevice<>(this);
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
        collector.offer(Capabilities.itemHandler(), itemHandler);

        if (direction == getBlockState().getValue(DiskDriveBlock.FACING).getOpposite()) {
            collector.offer(Capabilities.device(), device);
        }
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

    @Override
    public ItemStack getDiskItemStack() {
        return itemHandler.getStackInSlotRaw(0);
    }

    @Override
    public void handleDataAccess() {
        accessSoundEmitter.play();
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
}
