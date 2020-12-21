package li.cil.oc2.common.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.Material;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.StateManager;
import net.minecraft.util.math.Direction;

public final class ScreenBlock extends HorizontalFacingBlock {
    public ScreenBlock() {
        super(Settings.of(Material.METAL).sounds(BlockSoundGroup.METAL));
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getPlacementState(final ItemPlacementContext context) {
        return super.getDefaultState().with(FACING, context.getPlayerLookDirection().getOpposite());
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void appendProperties(final StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(FACING);
    }
}
