package li.cil.oc2.common.block;

import li.cil.oc2.common.tileentity.DiskDriveTileEntity;
import li.cil.oc2.common.tileentity.TileEntities;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

public final class DiskDriveBlock extends ImmutableHorizontalBlock implements EntityBlock {
    public DiskDriveBlock() {
        super(Properties
                .of(Material.METAL)
                .sound(SoundType.METAL)
                .strength(1.5f, 6.0f));
        registerDefaultState(getStateDefinition().any().setValue(FACING, Direction.NORTH));
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return super.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @SuppressWarnings("deprecation")
    @Override
    public InteractionResult use(final BlockState state, final Level world, final BlockPos pos, final Player player, final InteractionHand hand, final BlockHitResult hit) {
        final BlockEntity tileEntity = world.getBlockEntity(pos);
        if (!(tileEntity instanceof DiskDriveTileEntity)) {
            return super.use(state, world, pos, player, hand, hit);
        }

        final DiskDriveTileEntity diskDrive = (DiskDriveTileEntity) tileEntity;
        final ItemStack heldStack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (diskDrive.canEject()) {
                if (!world.isClientSide()) {
                    diskDrive.eject(player);
                }
                return InteractionResult.sidedSuccess(world.isClientSide());
            }
        } else {
            if (diskDrive.canInsert(heldStack)) {
                if (!world.isClientSide()) {
                    player.setItemInHand(hand, diskDrive.insert(heldStack, player));
                }
                return InteractionResult.sidedSuccess(world.isClientSide());
            }
        }

        return super.use(state, world, pos, player, hand, hit);
    }

    ///////////////////////////////////////////////////////////////////
    // EntityBlock

    @Nullable
    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return TileEntities.DISK_DRIVE_TILE_ENTITY.get().create(pos, state);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }
}
