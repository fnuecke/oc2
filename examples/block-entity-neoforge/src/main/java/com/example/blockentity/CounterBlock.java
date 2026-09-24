package com.example.blockentity;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class CounterBlock extends BaseEntityBlock {
    private static final MapCodec<CounterBlock> CODEC = simpleCodec(CounterBlock::new);

    public CounterBlock(final Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<CounterBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(final BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new CounterBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
                                               final Player player, final BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof final CounterBlockEntity counter) {
            counter.increment();
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
