package li.cil.oc2.common.block;

import li.cil.oc2.common.tags.ItemTags;
import li.cil.oc2.common.tileentity.DiskDriveTileEntity;
import li.cil.oc2.common.tileentity.TileEntities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

public final class DiskDriveBlock extends HorizontalBlock {
    public DiskDriveBlock() {
        super(Properties
                .create(Material.IRON)
                .sound(SoundType.METAL)
                .hardnessAndResistance(1.5f, 6.0f));
        setDefaultState(getStateContainer().getBaseState().with(HORIZONTAL_FACING, Direction.NORTH));
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public BlockState getStateForPlacement(final BlockItemUseContext context) {
        return super.getDefaultState().with(HORIZONTAL_FACING, context.getPlacementHorizontalFacing().getOpposite());
    }

    @Override
    public boolean hasTileEntity(final BlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(final BlockState state, final IBlockReader world) {
        return TileEntities.DISK_DRIVE_TILE_ENTITY.get().create();
    }

    @SuppressWarnings("deprecation")
    @Override
    public ActionResultType onBlockActivated(final BlockState state, final World world, final BlockPos pos, final PlayerEntity player, final Hand hand, final BlockRayTraceResult hit) {
        final TileEntity tileEntity = world.getTileEntity(pos);
        if (!(tileEntity instanceof DiskDriveTileEntity)) {
            return super.onBlockActivated(state, world, pos, player, hand, hit);
        }

        if (world.isRemote()) {
            return ActionResultType.SUCCESS;
        }

        final DiskDriveTileEntity diskDrive = (DiskDriveTileEntity) tileEntity;
        final ItemStack stack = player.getHeldItem(hand);

        if (ItemTags.WRENCHES.contains(stack.getItem())) {
            // TODO add container UI that opens when interacting with scrench?
        }

        if (player.isSneaking()) {
            diskDrive.eject();
        } else {
            player.setHeldItem(hand, diskDrive.insert(stack));
        }

        return ActionResultType.CONSUME;
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void fillStateContainer(final StateContainer.Builder<Block, BlockState> builder) {
        super.fillStateContainer(builder);
        builder.add(HORIZONTAL_FACING);
    }
}
