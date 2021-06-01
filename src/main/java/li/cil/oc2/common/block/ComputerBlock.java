package li.cil.oc2.common.block;

import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.api.capabilities.RedstoneEmitter;
import li.cil.oc2.common.Config;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.integration.Wrenches;
import li.cil.oc2.common.item.ItemGroup;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.tileentity.ComputerBlockEntity;
import li.cil.oc2.common.tileentity.TileEntities;
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
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jetbrains.annotations.Nullable;

import java.util.List;

import static li.cil.oc2.common.Constants.BLOCK_ENTITY_TAG_NAME_IN_ITEM;
import static li.cil.oc2.common.Constants.ITEMS_TAG_NAME;
import static li.cil.oc2.common.util.NBTUtils.makeInventoryTag;

public final class ComputerBlock extends HorizontalDirectionalBlock {
    // We bake the "screen" indent on the front into the collision shape to prevent stuff being
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
        super.fillItemCategory(group, items);

        items.add(getPreconfiguredComputer());
    }


    @Override
    public void appendHoverText(final ItemStack stack, @Nullable final BlockGetter world, final List<Component> tooltip, final TooltipFlag advanced) {
        super.appendHoverText(stack, world, tooltip, advanced);
        TooltipUtils.addEnergyConsumption(Config.computerEnergyPerTick, tooltip);
        TooltipUtils.addBlockEntityInventoryInformation(stack, tooltip);
    }

    @Override
    public boolean hasBlockEntity(final BlockState state) {
        return true;
    }

    @Override
    public BlockEntity createBlockEntity(final BlockState state, final BlockGetter world) {
        return TileEntities.COMPUTER_TILE_ENTITY.get().create();
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean isSignalSource(final BlockState state) {
        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getSignal(final BlockState state, final BlockGetter world, final BlockPos pos, final Direction side) {
        final BlockEntity tileEntity = world.getBlockEntity(pos);
        if (tileEntity != null) {
            // Redstone requests info for faces with external perspective. Capabilities treat
            // the Direction from internal perspective, so flip it.
            return tileEntity.getCapability(Capabilities.REDSTONE_EMITTER, side.getOpposite())
                    .map(RedstoneEmitter::getRedstoneOutput)
                    .orElse(0);
        }

        return super.getSignal(state, world, pos, side);
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getDirectSignal(final BlockState state, final BlockGetter world, final BlockPos pos, final Direction side) {
        return getSignal(state, world, pos, side);
    }

    @Override
    public boolean shouldCheckWeakPower(final BlockState state, final LevelReader world, final BlockPos pos, final Direction side) {
        return false;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void neighborChanged(final BlockState state, final Level world, final BlockPos pos, final Block changedBlock, final BlockPos changedBlockPos, final boolean isMoving) {
        final BlockEntity tileEntity = world.getBlockEntity(pos);
        if (tileEntity instanceof ComputerBlockEntity) {
            final ComputerBlockEntity computer = (ComputerBlockEntity) tileEntity;
            computer.handleNeighborChanged();
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public VoxelShape getShape(final BlockState state, final Level world, final BlockPos pos, final CollisionContext context) {
        switch (state.getValue(FACING)) {
            case NORTH:
                return NEG_Z_SHAPE;
            case SOUTH:
                return POS_Z_SHAPE;
            case WEST:
                return NEG_X_SHAPE;
            case EAST:
            default:
                return POS_X_SHAPE;
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public InteractionResult use(final BlockState state, final Level world, final BlockPos pos, final Player player, final InteractionHand hand, final BlockHitResult hit) {
        final BlockEntity tileEntity = world.getBlockEntity(pos);
        if (!(tileEntity instanceof ComputerBlockEntity)) {
            return super.use(state, world, pos, player, hand, hit);
        }

        final ComputerBlockEntity computer = (ComputerBlockEntity) tileEntity;
        final ItemStack heldItem = player.getItemInHand(hand);
        if (!world.isClientSide) {
            if (Wrenches.isWrench(heldItem)) {
                if (player instanceof ServerPlayer) {
                    computer.openContainerScreen((ServerPlayer) player);
                }
            } else {
                if (player.isShiftKeyDown()) {
                    computer.start();
                } else if (player instanceof ServerPlayer) {
                    computer.openTerminalScreen((ServerPlayer) player);
                }
            }
        }

        return world.isClientSide ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }

    @Override
    public void playerWillDestroy(final Level world, final BlockPos pos, final BlockState state, final Player player) {
        final BlockEntity tileEntity = world.getBlockEntity(pos);
        if (!world.isClientSide && tileEntity instanceof ComputerBlockEntity) {
            final ComputerBlockEntity computer = (ComputerBlockEntity) tileEntity;
            if (!computer.getContainerHelpers().isEmpty()) {
                computer.getContainerHelpers().exportDeviceDataToItemStacks();

                if (player.isCreative()) {
                    final ItemStack stack = new ItemStack(Items.COMPUTER);
                    computer.exportToItemStack(stack);
                    popResource(world, pos, stack);
                }
            }
        }

        super.playerWillDestroy(world, pos, state, player);
    }

    @Override
    public BlockState getStateForPlacement(final UseOnContext context) {
        return super.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    ///////////////////////////////////////////////////////////////////

    private ItemStack getPreconfiguredComputer() {
        final ItemStack computer = new ItemStack(Items.COMPUTER);

        final CompoundTag itemsTag = NBTUtils.getOrCreateChildTag(computer.getOrCreateTag(), BLOCK_ENTITY_TAG_NAME_IN_ITEM, ITEMS_TAG_NAME);
        itemsTag.put(DeviceTypes.MEMORY.getRegistryName().toString(), makeInventoryTag(
                new ItemStack(Items.MEMORY_LARGE),
                new ItemStack(Items.MEMORY_LARGE),
                new ItemStack(Items.MEMORY_LARGE)
        ));
        itemsTag.put(DeviceTypes.HARD_DRIVE.getRegistryName().toString(), makeInventoryTag(
                new ItemStack(Items.HARD_DRIVE_CUSTOM)
        ));
        itemsTag.put(DeviceTypes.FLASH_MEMORY.getRegistryName().toString(), makeInventoryTag(
                new ItemStack(Items.FLASH_MEMORY_CUSTOM)
        ));
        itemsTag.put(DeviceTypes.CARD.getRegistryName().toString(), makeInventoryTag(
                new ItemStack(Items.NETWORK_INTERFACE_CARD)
        ));

        return computer;
    }
}
