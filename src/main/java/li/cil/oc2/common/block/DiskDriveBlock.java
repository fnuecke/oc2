package li.cil.oc2.common.block;

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
                .of(Material.METAL)
                .sound(SoundType.METAL)
                .strength(1.5f, 6.0f));
        registerDefaultState(getStateDefinition().any().setValue(FACING, Direction.NORTH));
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public BlockState getStateForPlacement(final BlockItemUseContext context) {
        return super.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
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
    public ActionResultType use(final BlockState state, final World world, final BlockPos pos, final PlayerEntity player, final Hand hand, final BlockRayTraceResult hit) {
        final TileEntity tileEntity = world.getBlockEntity(pos);
        if (!(tileEntity instanceof DiskDriveTileEntity)) {
            return super.use(state, world, pos, player, hand, hit);
        }

        if (!world.isClientSide()) {
            final DiskDriveTileEntity diskDrive = (DiskDriveTileEntity) tileEntity;
            final ItemStack stack = player.getItemInHand(hand);

            if (player.isShiftKeyDown()) {
                diskDrive.eject();
            } else {
                player.setItemInHand(hand, diskDrive.insert(stack));
            }
        }

        return ActionResultType.sidedSuccess(world.isClientSide());
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void createBlockStateDefinition(final StateContainer.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }
}
