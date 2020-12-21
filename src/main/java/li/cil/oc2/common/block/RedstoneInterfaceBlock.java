package li.cil.oc2.common.block;

import li.cil.oc2.common.block.entity.RedstoneInterfaceTileEntity;
import li.cil.oc2.common.init.TileEntities;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.StateManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import org.jetbrains.annotations.Nullable;

public final class RedstoneInterfaceBlock extends HorizontalFacingBlock implements BlockEntityProvider {
    public RedstoneInterfaceBlock() {
        super(Settings.of(Material.METAL).sounds(BlockSoundGroup.METAL));
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getPlacementState(final ItemPlacementContext context) {
        return super.getDefaultState().with(FACING, context.getPlayerLookDirection().getOpposite());
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(final BlockView world) {
        return TileEntities.REDSTONE_INTERFACE_TILE_ENTITY.instantiate();
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean emitsRedstonePower(final BlockState state) {
        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getWeakRedstonePower(final BlockState state, final BlockView world, final BlockPos pos, final Direction side) {
        if (side.getAxis().getType() == Direction.Type.HORIZONTAL) {
            final BlockEntity tileEntity = world.getBlockEntity(pos);
            if (tileEntity instanceof RedstoneInterfaceTileEntity) {
                final RedstoneInterfaceTileEntity redstoneInterface = (RedstoneInterfaceTileEntity) tileEntity;
                // Redstone requests info for faces with external perspective. We treat
                // the Direction from internal perspective, so flip it.
                return redstoneInterface.getOutputForDirection(side.getOpposite());
            }
        }
        return super.getWeakRedstonePower(state, world, pos, side);
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getStrongRedstonePower(final BlockState state, final BlockView world, final BlockPos pos, final Direction side) {
        if (side == Direction.NORTH || side == Direction.EAST || side == Direction.SOUTH || side == Direction.WEST) {
            return getWeakRedstonePower(state, world, pos, side);
        } else {
            return super.getStrongRedstonePower(state, world, pos, side);
        }
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void appendProperties(final StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(FACING);
    }
}
