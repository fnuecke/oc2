package li.cil.oc2.common.block;

import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

public class ImmutableHorizontalBlock extends HorizontalDirectionalBlock {
    public ImmutableHorizontalBlock(final Properties properties) {
        super(properties);
    }

    @Override
    public BlockState rotate(final BlockState state, final Rotation rotation) {
        return state;
    }

    @Override
    public BlockState mirror(final BlockState state, final Mirror mirror) {
        return state;
    }
}
