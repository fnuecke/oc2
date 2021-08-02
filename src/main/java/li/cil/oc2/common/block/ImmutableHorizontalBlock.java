package li.cil.oc2.common.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;

public class ImmutableHorizontalBlock extends HorizontalBlock {
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
