/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.storage;

import com.google.common.annotations.VisibleForTesting;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.blockentity.ModBlockEntity;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.container.TypedItemStackHandler;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.AbstractMessage;
import li.cil.oc2.common.transaction.Transaction;
import li.cil.oc2.common.transaction.TransactionContext;
import li.cil.oc2.common.util.ItemDeviceUtils;
import li.cil.oc2.common.util.ItemStackUtils;
import li.cil.oc2.common.util.LocationSupplierUtils;
import li.cil.oc2.common.util.SerializationUtils;
import li.cil.oc2.common.util.ThrottledSoundEmitter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import javax.annotation.Nullable;
import java.time.Duration;

public abstract class AbstractRemovableMediaBlockEntity<TDevice extends AbstractRemovableMediaDevice> extends ModBlockEntity implements RemovableMediaContainer {
    private final RemovableMediaItemStackHandler itemHandler;
    private final ThrottledSoundEmitter insertSoundEmitter;
    private final ThrottledSoundEmitter ejectSoundEmitter;

    @Nullable
    private TDevice device;

    // --------------------------------------------------------------------- //

    protected AbstractRemovableMediaBlockEntity(
        final BlockEntityType<?> type,
        final BlockPos pos,
        final BlockState state,
        final TagKey<Item> mediaTag,
        final SoundEvent insertSound,
        final SoundEvent ejectSound
    ) {
        super(type, pos, state);

        this.itemHandler = new RemovableMediaItemStackHandler(mediaTag);
        this.insertSoundEmitter = new ThrottledSoundEmitter(LocationSupplierUtils.of(this),
            insertSound).withMinInterval(Duration.ofMillis(100));
        this.ejectSoundEmitter = new ThrottledSoundEmitter(LocationSupplierUtils.of(this),
            ejectSound).withMinInterval(Duration.ofMillis(100));
    }

    // --------------------------------------------------------------------- //

    protected abstract Direction getEjectDirection();

    protected abstract AbstractMessage createMediaMessage();

    // --------------------------------------------------------------------- //

    public boolean canInsert(final ItemStack stack) {
        return itemHandler.isItemValid(0, stack);
    }

    @VisibleForTesting
    public ItemStack insert(final ItemStack stack, @Nullable final Player player) {
        if (level == null || !canInsert(stack)) {
            return stack;
        }

        final ItemStack ejected;
        final ItemStack remainder;
        try (Transaction transaction = Transaction.openRoot()) {
            ejected = itemHandler.extractItem(0, 1, transaction);
            remainder = itemHandler.insertItem(0, stack, transaction);
            transaction.commit();
        }

        dropEjected(ejected, player);
        insertSoundEmitter.play();
        return remainder;
    }

    public boolean canEject() {
        return !itemHandler.getStackInSlotRaw(0).isEmpty();
    }

    public void eject(@Nullable final Player player) {
        if (level == null) {
            return;
        }

        dropEjected(Transaction.run(transaction -> itemHandler.extractItem(0, 1, transaction)), player);
    }

    public void dropMedia() {
        if (level == null || level.isClientSide()) {
            return;
        }

        final ItemStack stack = itemHandler.getStackInSlot(0);
        if (!stack.isEmpty()) {
            itemHandler.setStackInSlot(0, ItemStack.EMPTY);
            ItemStackUtils.spawnAsEntity(level, getBlockPos(), stack);
        }
    }

    @Override
    public void preRemoveSideEffects(final BlockPos pos, final BlockState state) {
        super.preRemoveSideEffects(pos, state);
        dropMedia();
    }

    public InteractionResult useWith(final Level level, final ItemStack heldStack, final Player player, final InteractionHand hand) {
        if (player.isShiftKeyDown()) {
            if (canEject()) {
                if (!level.isClientSide()) {
                    eject(player);
                }
                return InteractionResult.SUCCESS;
            }
        } else {
            if (canInsert(heldStack)) {
                if (!level.isClientSide()) {
                    player.setItemInHand(hand, insert(heldStack, player));
                }
                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    public InteractionResult useWithoutItem(final Level level, final Player player) {
        if (player.isShiftKeyDown() && canEject()) {
            if (!level.isClientSide()) {
                eject(player);
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    public ItemStack getMedia() {
        return itemHandler.getStackInSlot(0);
    }

    @Environment(EnvType.CLIENT)
    public void setMediaClient(final ItemStack stack) {
        itemHandler.setStackInSlot(0, stack);
    }

    @Nullable
    public TDevice getDevice() {
        return device;
    }

    public void setDevice(final TDevice value) {
        device = value;
    }

    @Override
    public ItemStack getMediaItemStack() {
        return itemHandler.getStackInSlotRaw(0);
    }

    @Override
    public void handleMediaChanged() {
        setChanged();
    }

    @Override
    public CompoundTag getUpdateTag(final HolderLookup.Provider registries) {
        final CompoundTag tag = super.getUpdateTag(registries);
        tag.put(Constants.ITEMS_TAG_NAME, SerializationUtils.saveToTag(problemPath(), registries, itemHandler::save));
        return tag;
    }

    // --------------------------------------------------------------------- //

    @Override
    protected void collectCapabilities(final CapabilityCollector collector, @Nullable final Direction direction) {
        collector.offer(Capabilities.ITEM_HANDLER, itemHandler);
    }

    @Override
    protected void saveAdditional(final ValueOutput output) {
        super.saveAdditional(output);

        itemHandler.save(output.child(Constants.ITEMS_TAG_NAME));
    }

    @Override
    protected void loadAdditional(final ValueInput input) {
        super.loadAdditional(input);

        itemHandler.load(input.childOrEmpty(Constants.ITEMS_TAG_NAME));
    }

    // --------------------------------------------------------------------- //

    private void dropEjected(final ItemStack stack, @Nullable final Player player) {
        if (stack.isEmpty()) {
            return;
        }

        final Direction facing = getEjectDirection();
        ejectSoundEmitter.play();
        ItemStackUtils.spawnAsEntity(level, getBlockPos().relative(facing), stack, facing).ifPresent(entity -> {
            if (player != null) {
                entity.setNoPickUpDelay();
                entity.setTarget(player.getUUID());
            }
        });
    }

    // --------------------------------------------------------------------- //

    private final class RemovableMediaItemStackHandler extends TypedItemStackHandler {
        public RemovableMediaItemStackHandler(final TagKey<Item> mediaTag) {
            super(1, mediaTag);
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
        public ItemStack extractItem(final int slot, final int amount, final TransactionContext transaction) {
            if (slot == 0 && amount > 0) {
                exportDeviceDataToItemStack(getStackInSlotRaw(0));
            }

            return super.extractItem(slot, amount, transaction);
        }

        @Override
        public int getSlotLimit(final int slot) {
            return 1;
        }

        @Override
        public void save(final ValueOutput output) {
            exportDeviceDataToItemStack(getStackInSlotRaw(0));
            super.save(output);
        }

        @Override
        protected void onContentsChanged(final int slot) {
            super.onContentsChanged(slot);

            if (level == null || level.isClientSide()) {
                return;
            }

            final ItemStack stack = getStackInSlotRaw(slot);
            if (device != null) {
                if (stack.isEmpty()) {
                    device.removeBlockDevice();
                } else {
                    device.updateBlockDevice(ItemDeviceUtils.getDeviceData(stack, device.getDeviceDataKey()));
                }
            }

            Network.sendToClientsTrackingBlockEntity(createMediaMessage(), AbstractRemovableMediaBlockEntity.this);

            setChanged();
        }

        private void exportDeviceDataToItemStack(final ItemStack stack) {
            if (stack.isEmpty()) {
                return;
            }

            if (level == null || level.isClientSide()) {
                return;
            }

            if (device == null) {
                return;
            }

            final CompoundTag tag = new CompoundTag();

            device.exportToItemStack(tag);

            if (tag.isEmpty()) {
                return;
            }

            ItemDeviceUtils.setDeviceData(stack, device.getDeviceDataKey(), tag);
        }
    }
}
