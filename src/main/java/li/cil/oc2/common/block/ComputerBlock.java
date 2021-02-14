package li.cil.oc2.common.block;

import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.api.capabilities.RedstoneEmitter;
import li.cil.oc2.common.Config;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.bus.device.data.BlockDeviceDataRegistration;
import li.cil.oc2.common.bus.device.data.Firmwares;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.integration.Wrenches;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.tileentity.ComputerTileEntity;
import li.cil.oc2.common.tileentity.TileEntities;
import li.cil.oc2.common.util.NBTUtils;
import li.cil.oc2.common.util.TooltipUtils;
import li.cil.oc2.common.util.VoxelShapeUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

import static li.cil.oc2.common.Constants.BLOCK_ENTITY_TAG_NAME_IN_ITEM;
import static li.cil.oc2.common.Constants.ITEMS_TAG_NAME;
import static li.cil.oc2.common.util.NBTUtils.makeInventoryTag;

public final class ComputerBlock extends HorizontalBlock {
    // We bake the "screen" indent on the front into the collision shape to prevent stuff being
    // placeable on that side, such as network connectors, torches, etc.
    private static final VoxelShape NEG_Z_SHAPE = VoxelShapes.or(
            Block.makeCuboidShape(0, 0, 1, 16, 16, 16), // main body
            Block.makeCuboidShape(0, 15, 0, 16, 16, 1), // across top
            Block.makeCuboidShape(0, 0, 0, 16, 6, 1), // across bottom
            Block.makeCuboidShape(0, 0, 0, 1, 16, 1), // up left
            Block.makeCuboidShape(15, 0, 0, 16, 16, 1) // up right
    );
    private static final VoxelShape NEG_X_SHAPE = VoxelShapeUtils.rotateHorizontalClockwise(NEG_Z_SHAPE);
    private static final VoxelShape POS_Z_SHAPE = VoxelShapeUtils.rotateHorizontalClockwise(NEG_X_SHAPE);
    private static final VoxelShape POS_X_SHAPE = VoxelShapeUtils.rotateHorizontalClockwise(POS_Z_SHAPE);

    ///////////////////////////////////////////////////////////////////

    public ComputerBlock() {
        super(Properties
                .create(Material.IRON)
                .sound(SoundType.METAL)
                .hardnessAndResistance(1.5f, 6.0f));
        setDefaultState(getStateContainer().getBaseState().with(HORIZONTAL_FACING, Direction.NORTH));
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void fillItemGroup(final ItemGroup group, final NonNullList<ItemStack> items) {
        super.fillItemGroup(group, items);

        items.add(getPreconfiguredComputer());
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void addInformation(final ItemStack stack, @Nullable final IBlockReader world, final List<ITextComponent> tooltip, final ITooltipFlag advanced) {
        super.addInformation(stack, world, tooltip, advanced);
        TooltipUtils.addEnergyConsumption(Config.computerEnergyPerTick, tooltip);
        TooltipUtils.addTileEntityInventoryInformation(stack, tooltip);
    }

    @Override
    public boolean hasTileEntity(final BlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(final BlockState state, final IBlockReader world) {
        return TileEntities.COMPUTER_TILE_ENTITY.get().create();
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean canProvidePower(final BlockState state) {
        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getWeakPower(final BlockState state, final IBlockReader world, final BlockPos pos, final Direction side) {
        final TileEntity tileEntity = world.getTileEntity(pos);
        if (tileEntity != null) {
            // Redstone requests info for faces with external perspective. Capabilities treat
            // the Direction from internal perspective, so flip it.
            return tileEntity.getCapability(Capabilities.REDSTONE_EMITTER, side.getOpposite())
                    .map(RedstoneEmitter::getRedstoneOutput)
                    .orElse(0);
        }

        return super.getWeakPower(state, world, pos, side);
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getStrongPower(final BlockState state, final IBlockReader world, final BlockPos pos, final Direction side) {
        return getWeakPower(state, world, pos, side);
    }

    @Override
    public boolean shouldCheckWeakPower(final BlockState state, final IWorldReader world, final BlockPos pos, final Direction side) {
        return false;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void neighborChanged(final BlockState state, final World world, final BlockPos pos, final Block changedBlock, final BlockPos changedBlockPos, final boolean isMoving) {
        final TileEntity tileEntity = world.getTileEntity(pos);
        if (tileEntity instanceof ComputerTileEntity) {
            final ComputerTileEntity computer = (ComputerTileEntity) tileEntity;
            computer.handleNeighborChanged();
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public VoxelShape getShape(final BlockState state, final IBlockReader world, final BlockPos pos, final ISelectionContext context) {
        switch (state.get(HORIZONTAL_FACING)) {
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
    public ActionResultType onBlockActivated(final BlockState state, final World world, final BlockPos pos, final PlayerEntity player, final Hand hand, final BlockRayTraceResult hit) {
        final TileEntity tileEntity = world.getTileEntity(pos);
        if (!(tileEntity instanceof ComputerTileEntity)) {
            return super.onBlockActivated(state, world, pos, player, hand, hit);
        }

        final ComputerTileEntity computer = (ComputerTileEntity) tileEntity;
        final ItemStack heldItem = player.getHeldItem(hand);
        if (!world.isRemote()) {
            if (Wrenches.isWrench(heldItem)) {
                if (player instanceof ServerPlayerEntity) {
                    computer.openContainerScreen((ServerPlayerEntity) player);
                }
            } else {
                if (player.isSneaking()) {
                    computer.start();
                } else if (player instanceof ServerPlayerEntity) {
                    computer.openTerminalScreen((ServerPlayerEntity) player);
                }
            }
        }

        return world.isRemote() ? ActionResultType.SUCCESS : ActionResultType.CONSUME;
    }

    @Override
    public void onBlockHarvested(final World world, final BlockPos pos, final BlockState state, final PlayerEntity player) {
        final TileEntity tileEntity = world.getTileEntity(pos);
        if (!world.isRemote() && tileEntity instanceof ComputerTileEntity) {
            final ComputerTileEntity computer = (ComputerTileEntity) tileEntity;
            if (!computer.getItemStackHandlers().isEmpty()) {
                computer.getItemStackHandlers().exportDeviceDataToItemStacks();

                if (player.isCreative()) {
                    final ItemStack stack = new ItemStack(Items.COMPUTER.get());
                    computer.exportToItemStack(stack);
                    spawnAsEntity(world, pos, stack);
                }
            }
        }

        super.onBlockHarvested(world, pos, state, player);
    }

    @Override
    public BlockState getStateForPlacement(final BlockItemUseContext context) {
        return super.getDefaultState().with(HORIZONTAL_FACING, context.getPlacementHorizontalFacing().getOpposite());
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void fillStateContainer(final StateContainer.Builder<Block, BlockState> builder) {
        super.fillStateContainer(builder);
        builder.add(HORIZONTAL_FACING);
    }

    ///////////////////////////////////////////////////////////////////

    private ItemStack getPreconfiguredComputer() {
        final ItemStack computer = new ItemStack(Items.COMPUTER.get());

        final CompoundNBT itemsTag = NBTUtils.getOrCreateChildTag(computer.getOrCreateTag(), BLOCK_ENTITY_TAG_NAME_IN_ITEM, ITEMS_TAG_NAME);
        itemsTag.put(DeviceTypes.MEMORY.getRegistryName().toString(), makeInventoryTag(
                Items.MEMORY.get().withCapacity(8 * Constants.MEGABYTE),
                Items.MEMORY.get().withCapacity(8 * Constants.MEGABYTE),
                Items.MEMORY.get().withCapacity(8 * Constants.MEGABYTE)
        ));
        itemsTag.put(DeviceTypes.HARD_DRIVE.getRegistryName().toString(), makeInventoryTag(
                Items.HARD_DRIVE.get().withData(BlockDeviceDataRegistration.BUILDROOT.get())
        ));
        itemsTag.put(DeviceTypes.FLASH_MEMORY.getRegistryName().toString(), makeInventoryTag(
                Items.FLASH_MEMORY.get().withFirmware(Firmwares.BUILDROOT.get())
        ));
        itemsTag.put(DeviceTypes.CARD.getRegistryName().toString(), makeInventoryTag(
                new ItemStack(Items.NETWORK_INTERFACE_CARD.get())
        ));

        return computer;
    }
}
