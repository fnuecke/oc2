/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

public class ImmutableHorizontalBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<ImmutableHorizontalBlock> CODEC = simpleCodec(ImmutableHorizontalBlock::new);

    @Override
    protected MapCodec<? extends ImmutableHorizontalBlock> codec() {
        return CODEC;
    }

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
