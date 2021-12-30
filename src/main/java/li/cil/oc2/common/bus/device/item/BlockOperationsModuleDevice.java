package li.cil.oc2.common.bus.device.item;

import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.Parameter;
import li.cil.oc2.api.capabilities.Robot;
import li.cil.oc2.api.util.RobotOperationSide;
import li.cil.oc2.common.Config;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.tags.ItemTags;
import li.cil.oc2.common.util.FakePlayerUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.TierSortingRegistry;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.List;

import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CommandBlock;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.StructureBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class BlockOperationsModuleDevice extends AbstractItemRPCDevice {
    private static final String LAST_OPERATION_TAG_NAME = "cooldown";

    private static final int COOLDOWN = Constants.SECONDS_TO_TICKS;

    ///////////////////////////////////////////////////////////////////

    private final Entity entity;
    private final Robot robot;
    private long lastOperation;

    ///////////////////////////////////////////////////////////////////

    public BlockOperationsModuleDevice(final ItemStack identity, final Entity entity, final Robot robot) {
        super(identity, "block_operations");
        this.entity = entity;
        this.robot = robot;
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public CompoundTag serializeNBT() {
        final CompoundTag tag = new CompoundTag();
        tag.putLong(LAST_OPERATION_TAG_NAME, lastOperation);
        return tag;
    }

    @Override
    public void deserializeNBT(final CompoundTag tag) {
        lastOperation = Mth.clamp(tag.getLong(LAST_OPERATION_TAG_NAME), 0, entity.level.getGameTime());
    }

    @Callback
    public boolean excavate(@Parameter("side") @Nullable final RobotOperationSide side) {
        if (isOnCooldown()) {
            return false;
        }

        beginCooldown();

        final Level level = entity.level;
        if (!(level instanceof ServerLevel)) {
            return false;
        }

        final int selectedSlot = robot.getSelectedSlot(); // Get once to avoid change due to threading.
        final ItemStackHandler inventory = robot.getInventory();

        final List<ItemEntity> oldItems = getItemsInRange();

        final Direction direction = RobotOperationSide.getAdjustedDirection(side, entity);
        if (!tryHarvestBlock(level, entity.blockPosition().relative(direction))) {
            return false;
        }

        final List<ItemEntity> droppedItems = getItemsInRange();
        droppedItems.removeAll(oldItems);

        for (final ItemEntity itemEntity : droppedItems) {
            ItemStack stack = itemEntity.getItem();
            stack = insertStartingAt(inventory, stack, selectedSlot, false);
            itemEntity.setItem(stack);
        }

        return true;
    }

    @Callback
    public boolean place(@Parameter("side") @Nullable final RobotOperationSide side) {
        if (isOnCooldown()) {
            return false;
        }

        beginCooldown();

        final Level level = entity.level;
        if (!(level instanceof ServerLevel)) {
            return false;
        }

        final int selectedSlot = robot.getSelectedSlot(); // Get once to avoid change due to threading.
        final ItemStackHandler inventory = robot.getInventory();

        final ItemStack extracted = inventory.extractItem(selectedSlot, 1, true);
        if (extracted.isEmpty() || !(extracted.getItem() instanceof BlockItem)) {
            return false;
        }

        final Direction direction = RobotOperationSide.getAdjustedDirection(side, entity);
        final BlockPos blockPos = entity.blockPosition().relative(direction);
        final Direction oppositeDirection = direction.getOpposite();
        final BlockHitResult hit = new BlockHitResult(
                Vec3.atCenterOf(blockPos).add(Vec3.atCenterOf(oppositeDirection.getNormal()).scale(0.5)),
                oppositeDirection,
                blockPos,
                false);

        final ItemStack itemStack = extracted.copy();
        final BlockItem blockItem = (BlockItem) itemStack.getItem();
        final ServerPlayer player = FakePlayerUtils.getFakePlayer((ServerLevel) level, entity);
        final BlockPlaceContext context = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, itemStack, hit);

        final InteractionResult result = blockItem.place(context);
        if (!result.consumesAction()) {
            return false;
        }

        if (itemStack.isEmpty()) {
            inventory.extractItem(selectedSlot, 1, false);
        }

        return true;
    }

    @Callback(synchronize = false)
    public int durability() {
        return identity.getMaxDamage() - identity.getDamageValue();
    }

    @Callback
    public boolean repair() {
        if (isOnCooldown()) {
            return false;
        }

        beginCooldown();

        if (identity.getDamageValue() == 0) {
            return false;
        }

        final int selectedSlot = robot.getSelectedSlot(); // Get once to avoid change due to threading.
        final ItemStackHandler inventory = robot.getInventory();

        final ItemStack extracted = inventory.extractItem(selectedSlot, 1, true);

        final Tiers tier = getRepairItemTier(extracted);
        if (tier == null) {
            return false;
        }

        final int repairValue = tier.getUses();
        if (repairValue == 0) {
            return false;
        }

        // Extra check just to ease my paranoia.
        if (inventory.extractItem(selectedSlot, 1, false).isEmpty()) {
            return false;
        }

        identity.setDamageValue(identity.getDamageValue() - repairValue);

        return true;
    }

    ///////////////////////////////////////////////////////////////////

    private void beginCooldown() {
        lastOperation = entity.level.getGameTime();
    }

    private boolean isOnCooldown() {
        return entity.level.getGameTime() - lastOperation < COOLDOWN;
    }

    private List<ItemEntity> getItemsInRange() {
        return entity.level.getEntitiesOfClass(ItemEntity.class, entity.getBoundingBox().inflate(2));
    }

    private boolean tryHarvestBlock(final Level world, final BlockPos blockPos) {
        // This method is based on PlayerInteractionManager::tryHarvestBlock. Simplified for our needs.
        final BlockState blockState = world.getBlockState(blockPos);
        if (blockState.isAir()) {
            return false;
        }

        final ServerPlayer player = FakePlayerUtils.getFakePlayer((ServerLevel) world, entity);
        final int experience = net.minecraftforge.common.ForgeHooks.onBlockBreakEvent(world, GameType.DEFAULT_MODE, player, blockPos);
        if (experience == -1) {
            return false;
        }

        final BlockEntity tileEntity = world.getBlockEntity(blockPos);
        final Block block = blockState.getBlock();
        final boolean isCommandBlock = block instanceof CommandBlock || block instanceof StructureBlock || block instanceof JigsawBlock;
        if (isCommandBlock && !player.canUseGameMasterBlocks()) {
            return false;
        }

        if (player.blockActionRestricted(world, blockPos, GameType.DEFAULT_MODE)) {
            return false;
        }

        final Tier toolTier = TierSortingRegistry.byName(Config.blockOperationsModuleToolTier);
        if (toolTier == null || !TierSortingRegistry.isCorrectTierForDrops(toolTier, blockState)) {
            return false;
        }

        if (!ForgeEventFactory.doPlayerHarvestCheck(player, blockState, true)) {
            return false;
        }

        if (identity.hurt(1, world.random, null)) {
            return false;
        }

        if (!blockState.onDestroyedByPlayer(world, blockPos, player, true, world.getFluidState(blockPos))) {
            return false;
        }

        block.destroy(world, blockPos, blockState);
        block.playerDestroy(world, player, blockPos, blockState, tileEntity, ItemStack.EMPTY);

        return true;
    }

    @Nullable
    private Tiers getRepairItemTier(final ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }

        final Item item = stack.getItem();
        if (ItemTags.TOOL_MATERIAL_NETHERITE.contains(item)) {
            return Tiers.NETHERITE;
        } else if (ItemTags.TOOL_MATERIAL_GOLD.contains(item)) {
            return Tiers.DIAMOND;
        } else if (ItemTags.TOOL_MATERIAL_DIAMOND.contains(item)) {
            return Tiers.GOLD;
        } else if (ItemTags.TOOL_MATERIAL_IRON.contains(item)) {
            return Tiers.IRON;
        } else if (ItemTags.TOOL_MATERIAL_STONE.contains(item)) {
            return Tiers.STONE;
        } else if (ItemTags.TOOL_MATERIAL_WOOD.contains(item)) {
            return Tiers.WOOD;
        }

        return null;
    }

    private ItemStack insertStartingAt(final IItemHandler handler, ItemStack stack, final int startSlot, final boolean simulate) {
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
