/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.rpc.item;

import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.Parameter;
import li.cil.oc2.api.capabilities.Robot;
import li.cil.oc2.api.inventory.ItemHandler;
import li.cil.oc2.api.util.RobotOperationSide;
import li.cil.oc2.common.util.FakePlayerUtils;
import li.cil.oc2.common.util.LevelUtils;
import li.cil.oc2.common.util.TickUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CommandBlock;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.StructureBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.List;

public final class BlockOperationsModuleDevice extends AbstractItemRPCDevice {
    private static final String LAST_OPERATION_TAG_NAME = "cooldown";
    private static final String COOLDOWN_TAG_NAME = "cooldown_ticks";

    private static final int MIN_COOLDOWN = TickUtils.toTicks(Duration.ofSeconds(1));
    private static final int MAX_COOLDOWN = TickUtils.toTicks(Duration.ofSeconds(15));

    // --------------------------------------------------------------------- //

    private final Entity entity;
    private final Robot robot;
    private long lastOperation;
    private int cooldown = MIN_COOLDOWN;

    // --------------------------------------------------------------------- //

    public BlockOperationsModuleDevice(final ItemStack identity, final Entity entity, final Robot robot) {
        super(identity, "block_operations");
        this.entity = entity;
        this.robot = robot;
    }

    // --------------------------------------------------------------------- //

    @Override
    public CompoundTag serializeNBT() {
        final CompoundTag tag = new CompoundTag();
        tag.putLong(LAST_OPERATION_TAG_NAME, lastOperation);
        tag.putInt(COOLDOWN_TAG_NAME, cooldown);
        return tag;
    }

    @Override
    public void deserializeNBT(final CompoundTag tag) {
        lastOperation = Mth.clamp(tag.getLong(LAST_OPERATION_TAG_NAME), 0, entity.level().getGameTime());
        cooldown = Mth.clamp(tag.getInt(COOLDOWN_TAG_NAME), MIN_COOLDOWN, MAX_COOLDOWN);
    }

    @Callback
    public boolean excavate() {
        return excavate(null);
    }

    @Callback
    public boolean excavate(@Parameter("side") @Nullable final RobotOperationSide side) {
        if (isOnCooldown()) {
            return false;
        }

        beginCooldown(MIN_COOLDOWN);

        final Level level = entity.level();
        if (!(level instanceof final ServerLevel serverLevel)) {
            return false;
        }

        final int selectedSlot = robot.getSelectedSlot(); // Get once to avoid change due to threading.
        final ItemHandler inventory = robot.getInventory();

        final Direction direction = RobotOperationSide.toGlobal(entity, side);
        final BlockPos blockPos = entity.blockPosition().relative(direction);

        final List<ItemEntity> oldItems = getItemsInRange();

        final ItemStack tool = inventory.getStackInSlot(selectedSlot);
        final ServerPlayer player = FakePlayerUtils.getFakePlayer(serverLevel, entity);

        final int breakTicks;
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        player.setOnGround(true); // Avoid the robot hovering slowing it down.
        try {
            breakTicks = tryHarvestBlock(serverLevel, player, blockPos, tool);
        } finally {
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }

        if (breakTicks < 0) {
            return false;
        }

        beginCooldown(Math.max(MIN_COOLDOWN, breakTicks));

        final List<ItemEntity> droppedItems = getItemsInRange();
        droppedItems.removeAll(oldItems);

        for (final ItemEntity itemEntity : droppedItems) {
            ItemStack stack = itemEntity.getItem();
            stack = insertStartingAt(inventory, stack, selectedSlot + 1, false);
            itemEntity.setItem(stack);
        }

        return true;
    }

    @Callback
    public boolean place() {
        return place(null);
    }

    @Callback
    public boolean place(@Parameter("side") @Nullable final RobotOperationSide side) {
        if (isOnCooldown()) {
            return false;
        }

        beginCooldown(MIN_COOLDOWN);

        final Level level = entity.level();
        if (!(level instanceof final ServerLevel serverLevel)) {
            return false;
        }

        final int selectedSlot = robot.getSelectedSlot(); // Get once to avoid change due to threading.
        final ItemHandler inventory = robot.getInventory();

        final ItemStack extracted = inventory.extractItem(selectedSlot, 1, true);
        if (extracted.isEmpty() || !(extracted.getItem() instanceof BlockItem)) {
            return false;
        }

        final Direction direction = RobotOperationSide.toGlobal(entity, side);
        final BlockPos blockPos = entity.blockPosition().relative(direction);
        final Direction oppositeDirection = direction.getOpposite();
        final BlockHitResult hit = new BlockHitResult(
            Vec3.atCenterOf(blockPos).add(Vec3.atCenterOf(oppositeDirection.getNormal()).scale(0.5)),
            oppositeDirection,
            blockPos,
            false);

        final ItemStack itemStack = extracted.copy();
        final ServerPlayer player = FakePlayerUtils.getFakePlayer(serverLevel, entity);

        // Full useOn dance for permission checks.
        player.setItemInHand(InteractionHand.MAIN_HAND, itemStack);
        try {
            final InteractionResult result = itemStack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
            if (!result.consumesAction()) {
                return false;
            }
        } finally {
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }

        if (itemStack.isEmpty()) {
            inventory.extractItem(selectedSlot, 1, false);
        }

        return true;
    }

    @Callback
    public int durability() {
        final ItemStack tool = robot.getInventory().getStackInSlot(robot.getSelectedSlot());
        if (!tool.isDamageableItem()) {
            return 0;
        }

        return tool.getMaxDamage() - tool.getDamageValue();
    }

    // --------------------------------------------------------------------- //

    private void beginCooldown(final int ticks) {
        lastOperation = entity.level().getGameTime();
        cooldown = ticks;
    }

    private boolean isOnCooldown() {
        return entity.level().getGameTime() - lastOperation < cooldown;
    }

    private List<ItemEntity> getItemsInRange() {
        return entity.level().getEntitiesOfClass(ItemEntity.class, entity.getBoundingBox().inflate(2));
    }

    private int tryHarvestBlock(final ServerLevel level, final ServerPlayer player, final BlockPos blockPos, final ItemStack tool) {
        // This method is based on ServerPlayerGameMode::destroyBlock. Simplified for our needs.
        final BlockState blockState = level.getBlockState(blockPos);
        if (blockState.isAir()) {
            return -1;
        }

        final Block block = blockState.getBlock();
        final boolean isCommandBlock = block instanceof CommandBlock || block instanceof StructureBlock || block instanceof JigsawBlock;
        if (isCommandBlock && !player.canUseGameMasterBlocks()) {
            return -1;
        }

        if (!LevelUtils.hasCorrectToolForDrops(level, player, blockPos, blockState)) {
            return -1;
        }

        final int breakTicks = breakDurationInTicks(blockState, player, level, blockPos);
        if (breakTicks < 0 || breakTicks > MAX_COOLDOWN) {
            return -1;
        }

        if (!LevelUtils.fireBlockBreak(level, player, blockPos, blockState)) {
            return -1;
        }

        final BlockEntity blockEntity = level.getBlockEntity(blockPos);
        final ItemStack toolBeforeMining = tool.copy();

        block.playerWillDestroy(level, blockPos, blockState, player);
        if (!level.removeBlock(blockPos, false)) {
            return -1;
        }

        block.destroy(level, blockPos, blockState);
        tool.mineBlock(level, blockState, blockPos, player);
        block.playerDestroy(level, player, blockPos, blockState, blockEntity, toolBeforeMining);

        return breakTicks;
    }

    private static int breakDurationInTicks(final BlockState blockState, final ServerPlayer player, final ServerLevel level, final BlockPos blockPos) {
        final float progressPerTick = blockState.getDestroyProgress(player, level, blockPos);
        if (progressPerTick <= 0) {
            return -1;
        }

        return Mth.ceil(1 / progressPerTick);
    }

    private ItemStack insertStartingAt(final ItemHandler handler, ItemStack stack, final int startSlot, final boolean simulate) {
        for (int i = 0; i < handler.getSlots(); i++) {
            final int slot = (startSlot + i) % handler.getSlots();
            stack = handler.insertItem(slot, stack, simulate);
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
        }

        return stack;
    }
}
