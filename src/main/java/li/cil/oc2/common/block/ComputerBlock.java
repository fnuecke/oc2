/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.block;

import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.api.capabilities.RedstoneEmitter;
import li.cil.oc2.common.Config;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.blockentity.ComputerBlockEntity;
import li.cil.oc2.common.blockentity.TickableBlockEntity;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.integration.Wrenches;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.util.NBTUtils;
import li.cil.oc2.common.util.TooltipUtils;
import li.cil.oc2.common.util.VoxelShapeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

import static li.cil.oc2.common.Constants.BLOCK_ENTITY_TAG_NAME_IN_ITEM;
import static li.cil.oc2.common.Constants.ITEMS_TAG_NAME;
import static li.cil.oc2.common.util.NBTUtils.makeInventoryTag;
import static li.cil.oc2.common.util.RegistryUtils.key;
import static li.cil.oc2.common.util.TranslationUtils.text;

public final class ComputerBlock extends ImmutableHorizontalBlock implements EntityBlock {
    // We bake the "screen" indent on the front into the collision shape, to prevent stuff being
    // placeable on that side, such as network connectors, torches, etc.
    private static final VoxelShape NEG_Z_SHAPE = Shapes.or(
        Block.box(0, 0, 1, 16, 16, 16), // main body
        Block.box(0, 15, 0, 16, 16, 1), // across top
        Block.box(0, 0, 0, 16, 6, 1), // across bottom
        Block.box(0, 0, 0, 1, 16, 1), // up left
        Block.box(15, 0, 0, 16, 16, 1) // up right
    );
    private static final VoxelShape NEG_X_SHAPE = VoxelShapeUtils.rotateHorizontalClockwise(NEG_Z_SHAPE);
    private static final VoxelShape POS_Z_SHAPE = VoxelShapeUtils.rotateHorizontalClockwise(NEG_X_SHAPE);
    private static final VoxelShape POS_X_SHAPE = VoxelShapeUtils.rotateHorizontalClockwise(POS_Z_SHAPE);

    ///////////////////////////////////////////////////////////////////

    public ComputerBlock() {
        super(Properties
            .of(Material.METAL)
            .sound(SoundType.METAL)
            .strength(1.5f, 6.0f));
        registerDefaultState(getStateDefinition().any().setValue(FACING, Direction.NORTH));
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void fillItemCategory(final CreativeModeTab group, final NonNullList<ItemStack> items) {
        items.add(getComputerWithFlash());
        items.add(getPreconfiguredComputer());
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(final ItemStack stack, @Nullable final BlockGetter level, final List<Component> tooltip, final TooltipFlag advanced) {
        super.appendHoverText(stack, level, tooltip, advanced);
        TooltipUtils.addEnergyConsumption(Config.computerEnergyPerTick, tooltip);
        TooltipUtils.addBlockEntityInventoryInformation(stack, tooltip);
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean isSignalSource(final BlockState state) {
        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getSignal(final BlockState state, final BlockGetter level, final BlockPos pos, final Direction side) {
        final BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity != null) {
            // Redstone requests info for faces with external perspective. Capabilities treat
            // the Direction from internal perspective, so flip it.
            return blockEntity.getCapability(Capabilities.REDSTONE_EMITTER, side.getOpposite())
                .map(RedstoneEmitter::getRedstoneOutput)
                .orElse(0);
        }

        return super.getSignal(state, level, pos, side);
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getDirectSignal(final BlockState state, final BlockGetter level, final BlockPos pos, final Direction side) {
        return getSignal(state, level, pos, side);
    }

    @Override
    public boolean shouldCheckWeakPower(final BlockState state, final LevelReader level, final BlockPos pos, final Direction side) {
        return false;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void neighborChanged(final BlockState state, final Level level, final BlockPos pos, final Block changedBlock, final BlockPos changedBlockPos, final boolean isMoving) {
        final BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof final ComputerBlockEntity computer) {
            computer.handleNeighborChanged();
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case NORTH -> NEG_Z_SHAPE;
            case SOUTH -> POS_Z_SHAPE;
            case WEST -> NEG_X_SHAPE;
            default -> POS_X_SHAPE;
        };
    }

    @SuppressWarnings("deprecation")
    @Override
    public InteractionResult use(final BlockState state, final Level level, final BlockPos pos, final Player player, final InteractionHand hand, final BlockHitResult hit) {
        final BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof final ComputerBlockEntity computer)) {
            return super.use(state, level, pos, player, hand, hit);
        }

        final ItemStack heldItem = player.getItemInHand(hand);
        if (Wrenches.isWrench(heldItem)) {
            if (!player.isShiftKeyDown()) {
                if (!level.isClientSide() && player instanceof final ServerPlayer serverPlayer) {
                    computer.openInventoryScreen(serverPlayer);
                }
                return InteractionResult.sidedSuccess(level.isClientSide());
            }
        } else {
            if (!level.isClientSide()) {
                if (player.isShiftKeyDown()) {
                    computer.start();
                } else if (player instanceof final ServerPlayer serverPlayer) {
                    computer.openTerminalScreen(serverPlayer);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        return super.use(state, level, pos, player, hand, hit);
    }

    @Override
    public void playerWillDestroy(final Level level, final BlockPos pos, final BlockState state, final Player player) {
        final BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!level.isClientSide() && blockEntity instanceof final ComputerBlockEntity computer) {
            if (!computer.getItemStackHandlers().isEmpty()) {
                computer.getItemStackHandlers().exportDeviceDataToItemStacks();

                if (player.isCreative()) {
                    final ItemStack stack = new ItemStack(Items.COMPUTER.get());
                    computer.exportToItemStack(stack);
                    popResource(level, pos, stack);
                }
            }
        }

        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return super.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    ///////////////////////////////////////////////////////////////////
    // EntityBlock

    @Nullable
    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return BlockEntities.COMPUTER.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state, final BlockEntityType<T> type) {
        return TickableBlockEntity.createTicker(level, type, BlockEntities.COMPUTER.get());
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    ///////////////////////////////////////////////////////////////////

    private ItemStack getComputerWithFlash() {
        final ItemStack computer = new ItemStack(this);

        final CompoundTag itemsTag = NBTUtils.getOrCreateChildTag(computer.getOrCreateTag(), BLOCK_ENTITY_TAG_NAME_IN_ITEM, ITEMS_TAG_NAME);
        itemsTag.put(key(DeviceTypes.FLASH_MEMORY), makeInventoryTag(
            new ItemStack(Items.FLASH_MEMORY_CUSTOM.get())
        ));

        return computer;
    }

    private ItemStack getPreconfiguredComputer() {
        final ItemStack computer = getComputerWithFlash();

        final CompoundTag itemsTag = NBTUtils.getOrCreateChildTag(computer.getOrCreateTag(), BLOCK_ENTITY_TAG_NAME_IN_ITEM, ITEMS_TAG_NAME);
        itemsTag.put(key(DeviceTypes.MEMORY), makeInventoryTag(
            new ItemStack(Items.MEMORY_LARGE.get()),
            new ItemStack(Items.MEMORY_LARGE.get()),
            new ItemStack(Items.MEMORY_LARGE.get()),
            new ItemStack(Items.MEMORY_LARGE.get())
        ));
        itemsTag.put(key(DeviceTypes.HARD_DRIVE), makeInventoryTag(
            new ItemStack(Items.HARD_DRIVE_CUSTOM.get())
        ));
        itemsTag.put(key(DeviceTypes.CARD), makeInventoryTag(
            new ItemStack(Items.NETWORK_INTERFACE_CARD.get())
        ));

        computer.setHoverName(text("block.{mod}.computer.preconfigured"));

        return computer;
    }
}
